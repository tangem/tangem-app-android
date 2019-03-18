package com.ripple.core.binary;

import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.serialized.BinarySerializer;
import com.ripple.core.serialized.BytesSink;
import com.ripple.core.serialized.SerializedType;
import com.ripple.core.serialized.StreamSink;
import com.ripple.core.types.known.sle.LedgerEntry;
import com.ripple.core.types.known.tx.result.TransactionResult;

import java.io.*;

public class STWriter implements BytesSink, Closeable {
    BytesSink sink;
    BinarySerializer serializer;
    public STWriter(BytesSink bytesSink) {
        serializer = new BinarySerializer(bytesSink);
        sink = bytesSink;
    }

    private OutputStream stream;
    private STWriter(BytesSink sink, OutputStream stream) {
        this(sink);
        this.stream = stream;
    }

    public static STWriter toFile(String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            return new STWriter(new StreamSink(bos), bos);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(SerializedType obj) {
        obj.toBytesSink(sink);
    }
    public void writeVl(SerializedType obj) {
        serializer.addLengthEncoded(obj);
    }

    @Override
    public void add(byte aByte) {
        sink.add(aByte);
    }

    @Override
    public void add(byte[] bytes) {
        sink.add(bytes);
    }

    public void write(TransactionResult result) {
        write(result.hash);
        writeVl(result.txn);
        writeVl(result.meta);
    }

    public void write(Hash256 hash256, LedgerEntry le) {
        write(hash256);
        writeVl(le);
    }

    @Override
    public void close() throws IOException {
        if (this.stream != null) {
            this.stream.close();
        }
    }
}
