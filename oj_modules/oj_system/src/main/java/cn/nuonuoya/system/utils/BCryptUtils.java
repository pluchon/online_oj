package cn.nuonuoya.system.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

//加密密文
public class BCryptUtils {

    public static String encryptPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    // 会根据数据库的密文提取出对应的盐值，对输入的密码进行加密，然后再进行比较
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}