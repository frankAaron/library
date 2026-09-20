package com.library.util;

import java.security.MessageDigest;

/**
 * MD5 加密工具类
 * 说明：课程设计场景采用 MD5 摘要存储密码，生产环境建议升级 BCrypt 等加盐算法
 */
public final class MD5Util {

    private MD5Util() {
    }

    /** 十六进制字符表 */
    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 对明文进行 MD5 加密，返回 32 位小写十六进制字符串
     *
     * @param text 明文
     * @return 密文，异常时返回 null
     */
    public static String encrypt(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : bytes) {
                sb.append(HEX_CHARS[(b >> 4) & 0x0F]).append(HEX_CHARS[b & 0x0F]);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }

    /**
     * 校验明文与密文是否匹配
     */
    public static boolean matches(String rawText, String encrypted) {
        return encrypt(rawText) != null && encrypt(rawText).equals(encrypted);
    }
}