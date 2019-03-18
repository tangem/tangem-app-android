package com.ripple.core.coretypes;

import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.BinaryParser;

import java.text.SimpleDateFormat;
import java.util.*;

//public class RippleDate extends Date implements SerializedType {
public class RippleDate extends Date {
    private static final SimpleDateFormat sdf = new SimpleDateFormat();

    public static long RIPPLE_EPOCH_SECONDS_OFFSET = 0x386D4380;
    static {
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        sdf.applyPattern("dd MMM yyyy HH:mm:ss z");

        /**
         * Magic constant tested and documented.
         *
         * Seconds since the unix epoch from unix time (accounting leap years etc)
         * at 1/January/2000 GMT
         */
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        long computed = cal.getTimeInMillis() / 1000;
        assertEquals("01 Jan 2000 00:00:00 GMT", sdf.format(cal.getTime())); // TODO
        assertEquals(RippleDate.RIPPLE_EPOCH_SECONDS_OFFSET, computed);
    }

    static public String gmtString(Date date) {
        return sdf.format(date);
    }

    private static void assertEquals(String s, String s1) {
        if (!s.equals(s1)) throw new AssertionError(String.format("%s != %s", s, s1));
    }
    private static void assertEquals(long a, long b) {
        if (a != b) throw new AssertionError(String.format("%s != %s", a, b));
    }

    private RippleDate() {
        super();
    }
    private RippleDate(long milliseconds) {
        super(milliseconds);
    }

    public long secondsSinceRippleEpoch() {
        return ((this.getTime() / 1000) - RIPPLE_EPOCH_SECONDS_OFFSET);
    }
    public static RippleDate fromSecondsSinceRippleEpoch(Number seconds) {
        return new RippleDate((seconds.longValue() + RIPPLE_EPOCH_SECONDS_OFFSET) * 1000);
    }
    public static RippleDate fromParser(BinaryParser parser) {
        UInt32 uInt32 = UInt32.fromParser(parser);
        return fromSecondsSinceRippleEpoch(uInt32);
    }
    public static RippleDate now() {
        return new RippleDate();
    }

/*    @Override
    public Object toJSON() {
        return secondsSinceRippleEpoch();
    }

    @Override
    public byte[] toBytes() {
        return new UInt32(secondsSinceRippleEpoch()).toBytes();
    }

    @Override
    public String toHex() {
        return new UInt32(secondsSinceRippleEpoch()).toHex();
    }

    @Override
    public void toBytesSink(BytesSink to) {
        new UInt32(secondsSinceRippleEpoch()).toBytesSink(to);
    }*/
}
