package cn.nuonuoya.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// mabtis-plus工具类
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    // 插入的时候
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject,"createTime", LocalDateTime.class,LocalDateTime.now());
        // TODO 拿取当前操作的用户ID
        this.strictInsertFill(metaObject,"createBy", Long.class,100L);
    }

    // 更新的时候
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject,"updateTime", LocalDateTime.class,LocalDateTime.now());
        // TODO 拿取当前操作的用户ID
        this.strictInsertFill(metaObject,"updateBy", Long.class,100L);
    }
}
