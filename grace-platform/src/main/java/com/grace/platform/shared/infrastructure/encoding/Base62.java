package com.grace.platform.shared.infrastructure.encoding;

/**
 * Base62 编码工具类
 * <p>
 * 使用 0-9, a-z, A-Z 共 62 个字符进行编码。
 * </p>
 */
public final class Base62 {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    private Base62() {
        // 工具类，禁止实例化
    }

    /**
     * 将字节数组编码为 Base62 字符串
     *
     * @param bytes 字节数组
     * @return Base62 编码字符串
     */
    public static String encode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        // 将字节数组转换为正整数（大端序）
        java.math.BigInteger value = new java.math.BigInteger(1, bytes);

        // 特殊情况：全零字节数组
        if (value.equals(java.math.BigInteger.ZERO)) {
            return "0";
        }

        StringBuilder sb = new StringBuilder();
        while (value.compareTo(java.math.BigInteger.ZERO) > 0) {
            java.math.BigInteger[] divRem = value.divideAndRemainder(java.math.BigInteger.valueOf(BASE));
            sb.append(ALPHABET.charAt(divRem[1].intValue()));
            value = divRem[0];
        }

        return sb.reverse().toString();
    }

    /**
     * 将 Base62 字符串解码为字节数组
     *
     * @param str Base62 编码字符串
     * @return 字节数组
     */
    public static byte[] decode(String str) {
        if (str == null || str.isEmpty()) {
            return new byte[0];
        }

        java.math.BigInteger value = java.math.BigInteger.ZERO;
        for (char c : str.toCharArray()) {
            int digit = ALPHABET.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            value = value.multiply(java.math.BigInteger.valueOf(BASE))
                        .add(java.math.BigInteger.valueOf(digit));
        }

        // 转换为字节数组
        byte[] bytes = value.toByteArray();

        // 如果结果以 0x00 开头（符号位），需要去除
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] result = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, result, 0, result.length);
            return result;
        }

        return bytes;
    }
}
