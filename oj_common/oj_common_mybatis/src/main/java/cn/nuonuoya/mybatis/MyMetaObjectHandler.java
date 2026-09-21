package cn.nuonuoya.mybatis;

import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// MyBatis-Plus 审计字段自动填充（字段为空时才填充）
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    // 插入时填充创建时间与创建人
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        Long userId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        if (userId != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, userId);
        }
    }

    // 更新时填充更新时间与更新人
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        Long userId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        if (userId != null) {
            this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
    }
}
