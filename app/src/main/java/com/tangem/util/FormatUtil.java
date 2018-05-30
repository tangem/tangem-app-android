package com.tangem.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Created by Ilia on 15.02.2018.
 */

public class FormatUtil {
    public static long parseValue(String valueStr) throws NumberFormatException {
        return new BigDecimal(valueStr).multiply(BigDecimal.valueOf(1_0000_0000)).setScale(0, BigDecimal.ROUND_HALF_DOWN).longValueExact();
    }

    public static String DoubleToString(double amount) {
        DecimalFormat myFormatter = GetDecimalFormat();
        String output = myFormatter.format(amount);
        return output;
    }

    public static DecimalFormat GetDecimalFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');

        String pattern = "#0.######";
        DecimalFormat myFormatter = new DecimalFormat(pattern, symbols);

        myFormatter.setParseBigDecimal(true);

        return myFormatter;
    }

    public static long ConvertStringToLong(String caption) throws Exception {
        BigDecimal d = new BigDecimal(caption);
        d = d.multiply(new BigDecimal(100000));
        d = d.setScale(5);
        BigInteger b = d.toBigInteger();
        long l = b.longValue();
        return l;
    }

}