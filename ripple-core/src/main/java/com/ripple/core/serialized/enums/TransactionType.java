package com.ripple.core.serialized.enums;

import com.ripple.core.fields.Type;
import com.ripple.core.serialized.BinaryParser;
import com.ripple.core.serialized.BytesSink;
import com.ripple.core.serialized.SerializedType;
import com.ripple.core.serialized.TypeTranslator;
import com.ripple.encodings.common.B16;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public enum TransactionType implements SerializedType {
    // Invalid (-1),
    Payment (0),
    EscrowCreate (1), // open
    EscrowFinish (2),
    AccountSet (3), // open,
    EscrowCancel (4),
    SetRegularKey(5),
    // unused1 (6), // open
    OfferCreate (7),
    OfferCancel (8),
    // unused2(9),
    TicketCreate(10),
    TicketCancel(11),
    SignerListSet(12),
    PaymentChannelCreate(13),
    PaymentChannelFund(14),
    PaymentChannelClaim(15),
    CheckCreate(16),
    CheckCash(17),
    CheckCancel(18),
    DepositPreauth(19),
    TrustSet (20),
    EnableAmendment(100),
    SetFee(101);

    public int asInteger() {
        return ord;
    }

    final int ord;
    TransactionType(int i) {
       ord = i;
    }

    @Override
    public Type type() {
        return Type.UInt16;
    }

    static private Map<Integer, TransactionType> byCode = new TreeMap<>();
    static {
        for (Object a : TransactionType.values()) {
            TransactionType f = (TransactionType) a;
            byCode.put(f.ord, f);
        }
    }

    static public TransactionType fromNumber(Number i) {
        return byCode.get(i.intValue());
    }

    // SeralizedType interface
    @Override
    public byte[] toBytes() {
        // TODO: bytes
        return new byte[]{(byte) (ord >> 8), (byte) (ord & 0xFF)};
    }
    @Override
    public Object toJSON() {
        return toString();
    }
    @Override
    public String toHex() {
        return B16.encode(toBytes());
    }
    @Override
    public void toBytesSink(BytesSink to) {
        to.add(toBytes());
    }
    public static class Translator extends TypeTranslator<TransactionType> {
        @Override
        public TransactionType fromParser(BinaryParser parser, Integer hint) {
            return fromNumber(parser.readOneInt() << 8 | parser.readOneInt());
        }

        @Override
        public TransactionType fromInteger(int integer) {
            return fromNumber(integer);
        }

        @Override
        public TransactionType fromString(String value) {
            return TransactionType.valueOf(value);
        }
    }

    @SuppressWarnings("unused")
    public static Translator translate = new Translator();
}
