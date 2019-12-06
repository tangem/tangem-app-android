package com.tangem.tangem_card.util;

public class Log {
    public static void i(String logTag, String message) {
        if( loggerInstance!=null ) loggerInstance.i(logTag,message);
    }
    public static void e(String logTag, String message) {
        if( loggerInstance!=null ) loggerInstance.e(logTag,message);
    }
    public static void v(String logTag, String message) {
        if( loggerInstance!=null ) loggerInstance.v(logTag,message);
    }

    private static LoggerInterface loggerInstance=null;
    public static void setLogger(LoggerInterface logger)
    {
        loggerInstance=logger;
    }
}
