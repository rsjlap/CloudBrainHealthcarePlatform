package com.neusoft.cloudbrain.common.util;

public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.replaceAll("(\\d{4})\\d+(\\d{4})", "$1**********$2");
    }
}
