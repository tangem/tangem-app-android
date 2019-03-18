package com.ripple.core.coretypes.hash.prefixes;

import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.encodings.common.B16;

/**
 * The prefix codes are part of the Ripple protocol
 * and existing codes cannot be arbitrarily changed.
 */
public enum HashPrefix implements Prefix {
    transactionID               ('T', 'X', 'N'),
    txNode                      ('S', 'N', 'D'),
    leafNode                    ('M', 'L', 'N'),
    innerNode                   ('M', 'I', 'N'),
    innerNodeV2                 ('I', 'N', 'R'),
    ledgerMaster                ('L', 'W', 'R'),
    txSign                      ('S', 'T', 'X'),
    txMultiSign                 ('S', 'M', 'T'),
    validation                  ('V', 'A', 'L'),
    proposal                    ('P', 'R', 'P'),
    manifest                    ('M', 'A', 'N'),
    paymentChannelClaim         ('C', 'L', 'M');

    private UInt32 uInt32;
    private byte[] bytes;
    private String chars;

    @Override
    public byte[] bytes() {
        return bytes;
    }

    public String toHex() {
        return B16.encode(bytes);
    }

    HashPrefix(char... chars) {
        this.chars = new String(chars);
        byte[] bytes = {
                (byte) chars[0],
                (byte) chars[1],
                (byte) chars[2],
                (byte) 0,
        };
        uInt32 = UInt32.fromBytes(bytes);
        this.bytes = bytes;
    }

    public UInt32 uInt32() {
        return uInt32;
    }

    public String chars() {
        return chars;
    }
}
