package com.ers.common.util;

import com.ers.common.exception.BusinessException;

import java.util.Currency;

public final class CurrencyUtils {

    private CurrencyUtils() {
    }

    public static boolean isValidIsoCode(String code) {
        if (code == null || code.length() != 3) {
            return false;
        }
        try {
            Currency.getInstance(code.toUpperCase());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static String requireValidIsoCode(String code) {
        if (!isValidIsoCode(code)) {
            throw new BusinessException("INVALID_CURRENCY", "Not a valid ISO 4217 currency code: " + code);
        }
        return code.toUpperCase();
    }
}
