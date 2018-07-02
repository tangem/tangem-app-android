package com.tangem.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

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
//        DecimalFormat myFormatter = new DecimalFormat(pattern, symbols);
        DecimalFormat myFormatter = new DecimalFormat(pattern);

        myFormatter.setParseBigDecimal(true);

        return myFormatter;
    }

    public static BigDecimal stringToBigDecimal(final String formattedString,
                                                 final Locale locale)
    {
        final DecimalFormatSymbols symbols;
        final char                 groupSeparatorChar;
        final String               groupSeparator;
        final char                 decimalSeparatorChar;
        final String               decimalSeparator;
        String                     fixedString;
        final BigDecimal           number;

        symbols              = new DecimalFormatSymbols(locale);
        groupSeparatorChar   = symbols.getGroupingSeparator();
        decimalSeparatorChar = symbols.getDecimalSeparator();

        if(groupSeparatorChar == '.')
        {
            groupSeparator = "\\" + groupSeparatorChar;
        }
        else
        {
            groupSeparator = Character.toString(groupSeparatorChar);
        }

        if(decimalSeparatorChar == '.')
        {
            decimalSeparator = "\\" + decimalSeparatorChar;
        }
        else
        {
            decimalSeparator = Character.toString(decimalSeparatorChar);
        }

        fixedString = formattedString.replaceAll(groupSeparator , "");
        fixedString = fixedString.replaceAll(decimalSeparator , ".");
        number      = new BigDecimal(fixedString);

        return (number);
    }

    public static long ConvertStringToLong(String caption) throws Exception {
//        BigDecimal d = new BigDecimal(caption);
        BigDecimal d = stringToBigDecimal(caption,Locale.US);
        d = d.multiply(new BigDecimal(100000));
        d = d.setScale(5);
        BigInteger b = d.toBigInteger();
        long l = b.longValue();
        return l;
    }

}