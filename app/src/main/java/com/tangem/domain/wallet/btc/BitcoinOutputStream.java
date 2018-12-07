package com.tangem.domain.wallet.btc;

/**
 * Created by Ilia on 29.09.2017.
 */

import com.tangem.domain.wallet.Transaction;

import java.io.ByteArrayOutputStream;

@SuppressWarnings("WeakerAccess")
public final class BitcoinOutputStream extends ByteArrayOutputStream {

    public void writeInt16(int value) {
        write(value & 0xff);
        write((value >> 8) & 0xff);
    }

    public void writeInt32(int value) {
        write(value & 0xff);
        write((value >> 8) & 0xff);
        write((value >> 16) & 0xff);
        write((value >>> 24) & 0xff);
    }

    public void writeInt64(long value) {
        writeInt32((int) (value & 0xFFFFFFFFL));
        writeInt32((int) ((value >>> 32) & 0xFFFFFFFFL));
    }

    public void writeVarInt(long value) {
        if (value < 0xfd) {
            write((int) (value & 0xff));
        } else if (value < 0xffff) {
            write(0xfd);
            writeInt16((int) value);
        } else if (value < 0xffffffffL) {
            write(0xfe);
            writeInt32((int) value);
        } else {
            write(0xff);
            writeInt64(value);
        }
    }

    public void write(Transaction.Script script) {
    }
}