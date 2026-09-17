package cn.nuonuoya.mybatis;

import cn.nuonuoya.common.constants.HttpConstants;
import cn.nuonuoya.common.utils.ThreadLocalUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// MyBatis-Plus 自动填充组件
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    // 插入数据时自动填充创建时间与创建人
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        Long userId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        if (userId != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, userId);
        }
    }

    // 更新数据时自动填充更新时间与更新人
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        Long userId = ThreadLocalUtil.get(HttpConstants.USER_ID, Long.class);
        if (userId != null) {
            this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
    }
}
