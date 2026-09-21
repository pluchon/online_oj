-- ========================================================
-- 个人中心字段扩充：tb_user 表新增 qq 字段
-- ========================================================
ALTER TABLE `tb_user` ADD COLUMN `qq` varchar(20) DEFAULT NULL COMMENT 'QQ号' AFTER `wechat`;
