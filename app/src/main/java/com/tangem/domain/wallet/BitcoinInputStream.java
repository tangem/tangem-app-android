package com.tangem.domain.wallet;

/**
 * Created by Ilia on 29.09.2017.
 */

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

@SuppressWarnings("WeakerAccess")
public class BitcoinInputStream extends ByteArrayInputStream {
    public BitcoinInputStream(byte[] buf) {
        super(buf);
    }

    @SuppressWarnings("unused")
    public BitcoinInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
    }

    public int readInt16() throws EOFException {
        return (readByte() & 0xff) | ((readByte() & 0xff) << 8);
    }

    public int readInt32() throws EOFException {
        return (readByte() & 0xff) | ((readByte() & 0xff) << 8) | ((readByte() & 0xff) << 16) | ((readByte() & 0xff) << 24);
    }

    public long readInt64() throws EOFException {
        return (readInt32() & 0xFFFFFFFFL) | ((readInt32() & 0xFFFFFFFFL) << 32);
    }

    public int readByte() throws EOFException {
        int readedByte = super.read();
        if (readedByte == -1) {
            throw new EOFException();
        }
        return readedByte;
    }

    public long readVarInt() throws EOFException {
        int readedByte = readByte();
        if (readedByte < 0xfd) {
            return readedByte;
        } else if (readedByte == 0xfd) {
            return readInt16();
        } else if (readedByte == 0xfe) {
            return readInt32();
        } else {
            return readInt64();
        }
    }

    public byte[] readChars(final int count) throws IOException {
        byte[] buf = new byte[count];
        int off = 0;
        while (off != count) {
            int bytesReadCurr = read(buf, off, count - off);
            if (bytesReadCurr == -1) {
                throw new EOFException();
            } else {
                off += bytesReadCurr;
            }
        }
        return buf;
    }

}
