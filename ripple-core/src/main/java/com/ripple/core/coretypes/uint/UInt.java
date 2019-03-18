package com.ripple.core.coretypes.uint;

import com.ripple.core.serialized.BinaryParser;
import com.ripple.core.serialized.BytesSink;
import com.ripple.core.serialized.SerializedType;
import com.ripple.core.serialized.TypeTranslator;
import com.ripple.encodings.common.B16;
import com.ripple.utils.Utils;

import java.math.BigInteger;

abstract public class UInt<Subclass extends UInt> extends Number implements SerializedType, Comparable<UInt> {
    private final BigInteger value;

    private static BigInteger[] upperBounds = new BigInteger[8];
    private static BigInteger maxUIntVal(int bits) {
        return new BigInteger("2").pow(bits).subtract(BigInteger.ONE);
    }

    static {
        for (int i = 0; i < 8; i++) {
            upperBounds[i] = maxUIntVal((i + 1) * 8 );
        }
    }

    UInt(byte[] bytes) {
        value = new BigInteger(1, bytes);
        checkBounds();
    }

    private void checkBounds() {
        BigInteger upper = upperBounds[getByteWidth() - 1];
        if (value.compareTo(upper) > 0 || value.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("value `" + value +
                    "` is illegal for " + getClass().getSimpleName());
        }
    }

    UInt(BigInteger bi) {
        value = (bi);
        checkBounds();
    }
    UInt(Number s) {
        value = (BigInteger.valueOf(s.longValue()));
        checkBounds();
    }
    UInt(String s) {
        value = (new BigInteger(s));
        checkBounds();
    }
    UInt(String s, int radix) {
        value = (new BigInteger(s, radix));
        checkBounds();
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public abstract int getByteWidth();
    protected abstract Subclass instanceFrom(BigInteger n);

    public Subclass add(UInt val) {
        return instanceFrom(value.add(val.value));
    }

    public Subclass subtract(UInt val) {
        return instanceFrom(value.subtract(val.value));
    }

    public Subclass multiply(UInt val) {
        return instanceFrom(value.multiply(val.value));
    }

    public Subclass divide(UInt val) {
        return instanceFrom(value.divide(val.value));
    }

    public Subclass or(UInt val) {
        return instanceFrom(value.or(val.value));
    }

    public Subclass shiftLeft(int n) {
        return instanceFrom(value.shiftLeft(n));
    }

    public Subclass shiftRight(int n) {
        return instanceFrom(value.shiftRight(n));
    }

    public int compareTo(UInt val) {
        return value.compareTo(val.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UInt) {
            return equals((UInt) obj);
        }
        else return super.equals(obj);
    }

    public boolean equals(UInt x) {
        return value.equals(x.value);
    }

    public String toString(int radix) {
        return value.toString(radix);
    }
    public byte[] toByteArray() {
        int length = getByteWidth();
        return Utils.leadingZeroesTrimmedOrPaddedTo(length, value.toByteArray());
    }

    abstract public Object value();

    public BigInteger bigInteger(){
        return value;
    }

    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public long longValue() {
        return value.longValue();
    }

    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    @Override
    public float floatValue() {
        return value.floatValue();
    }

    @Override
    public byte byteValue() {
        return value.byteValue();
    }

    @Override
    public short shortValue() {
        return value.shortValue();
    }

    public <T extends UInt> boolean  lte(T sequence) {
        return compareTo(sequence) < 1;
    }

    public boolean testBit(int f) {
        // TODO, optimized ;) // move to Uint32
        return value.testBit(f);
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    static public abstract class UINTTranslator<T extends UInt> extends TypeTranslator<T> {
        public abstract T newInstance(BigInteger i);
        public abstract int byteWidth();

        @Override
        public T fromParser(BinaryParser parser, Integer hint) {
            return newInstance(new BigInteger(1, parser.read(byteWidth())));
        }

        @Override
        public Object toJSON(T obj) {
            if (obj.getByteWidth() <= 4) {
                return obj.longValue();
            } else {
                return toString(obj);
            }
        }

        @Override
        public T fromLong(long aLong) {
            return newInstance(BigInteger.valueOf(aLong));
        }

        @Override
        public T fromString(String value) {
            int radix = byteWidth() <= 4 ? 10 : 16;
            return newInstance(new BigInteger(value, radix));
        }

        @Override
        public T fromInteger(int integer) {
            return fromLong(integer);
        }

        @Override
        public String toString(T obj) {
            return B16.encode(obj.toByteArray());
        }

        @Override
        public void toBytesSink(T obj, BytesSink to) {
            to.add(obj.toByteArray());
        }
    }
}
