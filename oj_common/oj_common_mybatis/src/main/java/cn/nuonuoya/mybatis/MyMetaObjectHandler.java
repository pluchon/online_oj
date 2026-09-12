package cn.nuonuoya.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// mabtis-plus工具类
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject,"createTime", LocalDateTime.class,LocalDateTime.now());
        // TODO 拿取当前操作的用户ID
        this.strictInsertFill(metaObject,"createBy", Long.class,100L);
    }

    @Override
    public void updateFill(MetaObject metaObject) {

    }
}
