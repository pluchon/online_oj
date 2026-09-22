-- 把 Nacos 2.x 配置库 bitoj_nacos_local 的命名空间与配置迁到 3.x 配置库 bitoj_nacos_v3
-- 前提：已执行 nacos_v3_init.sql；可重复执行，已存在的配置不覆盖
-- 旧库保持不动，作为回退点；登录账号不迁移，首次打开 3.x 控制台时重新设置管理员密码
SET NAMES utf8mb4;

INSERT IGNORE INTO bitoj_nacos_v3.tenant_info
    (kp, tenant_id, tenant_name, tenant_desc, create_source, gmt_create, gmt_modified)
SELECT kp, tenant_id, tenant_name, tenant_desc, create_source, gmt_create, gmt_modified
FROM bitoj_nacos_local.tenant_info;

INSERT IGNORE INTO bitoj_nacos_v3.config_info
    (data_id, group_id, content, md5, gmt_create, gmt_modified, src_user, src_ip,
     app_name, tenant_id, c_desc, c_use, effect, type, c_schema, encrypted_data_key)
SELECT data_id, group_id, content, md5, gmt_create, gmt_modified, src_user, src_ip,
       app_name, tenant_id, c_desc, c_use, effect, type, c_schema, IFNULL(encrypted_data_key, '')
FROM bitoj_nacos_local.config_info;

-- 迁移后需手动调整网关配置 oj-gateway-{profile}.yaml：Spring Cloud 2025.0 起
-- spring.cloud.gateway.routes 改为 spring.cloud.gateway.server.webflux.routes（整段路由缩进随之下移两级）
