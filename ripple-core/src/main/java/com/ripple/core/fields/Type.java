/* DO NOT EDIT, AUTO GENERATED */
package com.ripple.core.fields;

public enum Type {
    Unknown(-2),
    Done(-1),
    NotPresent(0),
    UInt16(1),
    UInt32(2),
    UInt64(3),
    Hash128(4),
    Hash256(5),
    Amount(6),
    Blob(7),
    AccountID(8),
    STObject(14),
    STArray(15),
    UInt8(16),
    Hash160(17),
    PathSet(18),
    Vector256(19),
    Transaction(10001),
    LedgerEntry(10002),
    Validation(10003);

    final int id;

    Type(int type) {
        this.id = type;
    }

    public int getId() {
        return id;
    }
}
