package cn.nuonuoya.gateway.filter;

import cn.hutool.core.util.StrUtil;
import cn.nuonuoya.common.constants.CacheConstants;
import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.domain.LoginUser;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.enums.ResultCode;
import cn.nuonuoya.common.enums.UserIdentity;
import cn.nuonuoya.common.utils.JwtUtils;
import cn.nuonuoya.gateway.properties.IgnoreWhiteProperties;
import cn.nuonuoya.redis.service.RedisService;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

// 网关统一鉴权：校验令牌与会话，按路径前缀校验身份，并向下游透传用户身份
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    // 服务间内部接口路径（/{domain}/internal/**），网关不对外暴露
    private static final String INTERNAL_PATH_PATTERN = "/**/internal/**";

    // 管理端路径前缀
    private static final String SYSTEM_PATH_PREFIX = "/" + HttpConstants.SYSTEM_URL_PREFIX + "/";

    // 用户端路径前缀
    private static final String FRIEND_PATH_PREFIX = "/" + HttpConstants.FRIEND_URL_PREFIX + "/";

    // 兼容旧客户端的令牌请求头
    private static final String LEGACY_TOKEN_HEADER = "token";

    // 路径匹配器（线程安全，全局复用）
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // 免登录白名单，配置于 Nacos
    @Autowired
    private IgnoreWhiteProperties ignoreWhite;

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private RedisService redisService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String url = exchange.getRequest().getURI().getPath();

        // 内部接口仅供服务间调用，一律禁止经网关访问
        if (PATH_MATCHER.match(INTERNAL_PATH_PATTERN, url)) {
            return unauthorizedResponse(exchange, "禁止访问内部接口");
        }

        // 丢弃客户端自带的身份请求头，身份只能由网关校验后写入
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HttpConstants.USER_ID);
                    headers.remove(HttpConstants.USER_KEY);
                })
                .build();
        ServerWebExchange cleanExchange = exchange.mutate().request(request).build();
        boolean isWhite = matches(url, ignoreWhite.getWhites());

        String token = JwtUtils.cleanToken(getToken(request));
        if (token == null) {
            return isWhite ? chain.filter(cleanExchange) : unauthorizedResponse(exchange, "令牌不能为空");
        }

        Claims claims;
        try {
            claims = JwtUtils.parseToken(token, secret);
        } catch (Exception e) {
            log.warn("[鉴权] 令牌解析失败: {}, 请求路径: {}", e.getMessage(), url);
            claims = null;
        }
        if (claims == null) {
            return isWhite ? chain.filter(cleanExchange) : unauthorizedResponse(exchange, "令牌已过期或验证不正确");
        }

        String userKey = JwtUtils.getUserKey(claims);
        String userId = JwtUtils.getUserId(claims);
        if (StrUtil.isEmpty(userKey) || StrUtil.isEmpty(userId)) {
            return isWhite ? chain.filter(cleanExchange) : unauthorizedResponse(exchange, "令牌验证失败");
        }

        LoginUser user = redisService.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + userKey, LoginUser.class);
        if (user == null) {
            return isWhite ? chain.filter(cleanExchange) : unauthorizedResponse(exchange, "登录状态已过期");
        }

        // 管理端接口仅限管理员，用户端接口仅限普通用户
        if (url.startsWith(SYSTEM_PATH_PREFIX) && !Objects.equals(user.getIdentity(), UserIdentity.ADMIN.getValue())) {
            return unauthorizedResponse(exchange, "令牌验证失败");
        }
        if (url.startsWith(FRIEND_PATH_PREFIX) && !Objects.equals(user.getIdentity(), UserIdentity.ORDINARY.getValue())) {
            return unauthorizedResponse(exchange, "令牌验证失败");
        }

        // 向下游微服务透传已校验的用户身份
        ServerHttpRequest authedRequest = request.mutate()
                .header(HttpConstants.USER_ID, userId)
                .header(HttpConstants.USER_KEY, userKey)
                .build();
        return chain.filter(exchange.mutate().request(authedRequest).build());
    }

    // 判断路径是否命中任一白名单规则（? 单字符，* 单层路径，** 任意层路径）
    private boolean matches(String url, List<String> patternList) {
        if (StrUtil.isEmpty(url) || CollectionUtils.isEmpty(patternList)) {
            return false;
        }
        for (String pattern : patternList) {
            if (PATH_MATCHER.match(pattern, url)) {
                return true;
            }
        }
        return false;
    }

    // 从请求头获取原始令牌
    private String getToken(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst(HttpConstants.AUTHENTICATION);
        return StrUtil.isEmpty(token) ? request.getHeaders().getFirst(LEGACY_TOKEN_HEADER) : token;
    }

    // 输出未授权响应（网关基于 WebFlux，无法使用 MVC 全局异常处理）
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg) {
        log.warn("[鉴权] 请求被拒绝, 路径: {}, 原因: {}", exchange.getRequest().getPath(), msg);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        OJResult<?> result = OJResult.fail(ResultCode.FAILED_UNAUTHORIZED.getCode(), msg);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(dataBuffer));
    }

    // 鉴权过滤器优先执行（值越小越先执行）
    @Override
    public int getOrder() {
        return -200;
    }
}
