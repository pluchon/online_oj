package cn.nuonuoya.gateway.filter;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.CacheConstants;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.common.enums.UserIdentity;
import cn.nuonuoya.gateway.properties.IgnoreWhiteProperties;
import cn.nuonuoya.redis.service.RedisService;
import cn.nuonuoya.common.domain.LoginUser;
import cn.nuonuoya.common.utils.JwtUtils;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

// 网关鉴权
// ordered指的是鉴权代码的优先级，因为后续可能会有多级鉴权
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    // 服务间内部接口路径（/{domain}/internal/**），网关不对外暴露
    private static final String INTERNAL_PATH_PATTERN = "/**/internal/**";

    // 排除过滤的 uri ⽩名单地址，在nacos⾃⾏添加
    @Autowired
    private IgnoreWhiteProperties ignoreWhite;

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private RedisService redisService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String url = request.getURI().getPath();

        // 内部接口仅供服务间调用，一律禁止经网关访问
        if (isMatch(INTERNAL_PATH_PATTERN, url)) {
            return unauthorizedResponse(exchange, "禁止访问内部接口");
        }
        boolean isWhite = matches(url, ignoreWhite.getWhites());

        // 从http请求头中获取token
        String token = getToken(request);
        if (StrUtil.isEmpty(token)) {
            if (isWhite) {
                return chain.filter(exchange);
            }
            return unauthorizedResponse(exchange, "令牌不能为空");
        }
        Claims claims;
        try {
            claims = JwtUtils.parseToken(token, secret);
            // 获取令牌中信息 解析payload中信息
            if (claims == null) {
                if (isWhite) {
                    return chain.filter(exchange);
                }
                return unauthorizedResponse(exchange, "令牌已过期或验证不正确！");
            }
        } catch (Exception e) {
            log.error("[鉴权异常处理] 解析令牌异常: {}, 请求路径: {}", e.getMessage(), exchange.getRequest().getPath());
            if (isWhite) {
                return chain.filter(exchange);
            }
            return unauthorizedResponse(exchange, "令牌已过期或验证不正确！");
        }
        String userKey = JwtUtils.getUserKey(claims);
        // 获取jwt中的key，判断是否过期
        boolean isLogin = redisService.hasKey(getTokenKey(userKey));
        if (!isLogin) {
            if (isWhite) {
                return chain.filter(exchange);
            }
            return unauthorizedResponse(exchange, "登录状态已过期");
        }
        String userid = JwtUtils.getUserId(claims);
        // 判断jwt中的信息是否完整
        if (StrUtil.isEmpty(userid)) {
            if (isWhite) {
                return chain.filter(exchange);
            }
            return unauthorizedResponse(exchange, "令牌验证失败");
        }
        LoginUser user = redisService.getCacheObject(getTokenKey(userKey), LoginUser.class);
        if (user == null) {
            if (isWhite) {
                return chain.filter(exchange);
            }
            return unauthorizedResponse(exchange, "登录状态已过期");
        }
        if (url.contains(HttpConstants.SYSTEM_URL_PREFIX) && !Objects.equals(user.getIdentity(), UserIdentity.ADMIN.getValue())) {
            return unauthorizedResponse(exchange, "令牌验证失败");
        }
        if (url.contains(HttpConstants.FRIEND_URL_PREFIX) && !Objects.equals(user.getIdentity(), UserIdentity.ORDINARY.getValue())) {
            return unauthorizedResponse(exchange, "令牌验证失败");
        }
        // 绑定到网关当前线程上下文
        ThreadLocalUtil.set(HttpConstants.USER_ID, userid);
        ThreadLocalUtil.set(HttpConstants.USER_KEY, userKey);
        // 向下游微服务透传用户身份请求头
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(HttpConstants.USER_ID, userid)
                .header(HttpConstants.USER_KEY, userKey)
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 查找指定url是否匹配指定匹配规则链表中的任意⼀个字符串
     *
     * @param url         指定url
     * @param patternList 需要检查的匹配规则链表
     * @return 是否匹配
     */
    private boolean matches(String url, List<String> patternList) {
        if (StrUtil.isEmpty(url) || CollectionUtils.isEmpty(patternList)) {
            return false;
        }
        for (String pattern : patternList) {
            if (isMatch(pattern, url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断url是否与规则匹配
     * 匹配规则中：
     * ? 表⽰单个字符;
     * * 表⽰⼀层路径内的任意字符串，不可跨层级;
     * ** 表⽰任意层路径;
     *
     * @param pattern 匹配规则
     * @param url     需要匹配的url
     * @return 是否匹配
     */
    private boolean isMatch(String pattern, String url) {
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, url);
    }

    /**
     * 获取缓存key
     */
    private String getTokenKey(String token) {
        return CacheConstants.LOGIN_TOKEN_KEY + token;
    }

    /**
     * 从请求头中获取请求token，容错处理引号、多余Bearer前缀及空白字符
     */
    private String getToken(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst(HttpConstants.AUTHENTICATION);
        if (StrUtil.isEmpty(token)) {
            token = request.getHeaders().getFirst("token");
        }
        if (StrUtil.isEmpty(token)) {
            return null;
        }
        token = token.trim();
        // 去除可能的双引号包裹（从JSON复制时易带入引号）
        if (token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
            token = token.substring(1, token.length() - 1).trim();
        }
        // 循环去除可能重复的 Bearer 前缀
        while (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        return token;
    }

    // 为什么不用全局异常处理？因为我们gateway是用于webflux实现的，全局异常做不到
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg) {
        log.error("[鉴权异常处理]请求路径:{}, 原因:{}", exchange.getRequest().getPath(), msg);
        return webFluxResponseWriter(exchange.getResponse(), msg, ResultCode.FAILED_UNAUTHORIZED.getCode());
    }

    //拼装webflux模型响应
    private Mono<Void> webFluxResponseWriter(ServerHttpResponse response, String msg, int code) {
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        OJResult<?> result = OJResult.fail(code, msg);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JSON.toJSONString(result).getBytes());
        return response.writeWith(Mono.just(dataBuffer));
    }

    // 值越小越先执行
    @Override
    public int getOrder() {
        return -200;
    }
}
