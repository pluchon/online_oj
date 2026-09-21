package cn.nuonuoya.system.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// 密码 BCrypt 加密与校验工具类
public class BCryptUtils {

    // BCrypt 编码器（线程安全，全局复用）
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    // 加密明文密码（每次生成随机盐）
    public static String encryptPassword(String password) {
        return PASSWORD_ENCODER.encode(password);
    }

    // 校验明文密码与密文是否匹配（从密文中提取盐值）
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }
}
