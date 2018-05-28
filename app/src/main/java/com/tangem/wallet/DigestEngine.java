package com.tangem.wallet;


import java.security.MessageDigest;

/**
 * Created by Ilia on 18.12.2017.
 */

public abstract class DigestEngine extends MessageDigest implements Digest {

    /**
     * Reset the hash algorithm state.
     */
    protected abstract void engineReset();

    /**
     * Process one block of data.
     *
     * @param data   the data block
     */
    protected abstract void processBlock(byte[] data);

    /**
     * Perform the final padding and store the result in the
     * provided buffer. This method shall call {@link #flush}
     * and then {@link #update} with the appropriate padding
     * data in order to get the full input data.
     *
     * @param buf   the output buffer
     * @param off   the output offset
     */
    protected abstract void doPadding(byte[] buf, int off);

    /**
     * This function is called at object creation time; the
     * implementation should use it to perform initialization tasks.
     * After this method is called, the implementation should be ready
     * to process data or meaningfully honour calls such as
     * {@link #engineGetDigestLength}
     */
    protected abstract void doInit();

    private int digestLen, blockLen, inputLen;
    private byte[] inputBuf, outputBuf;
    private long blockCount;

    /**
     * Instantiate the engine.
     */
    public DigestEngine(String alg)
    {
        super(alg);
        doInit();
        digestLen = engineGetDigestLength();
        blockLen = getInternalBlockLength();
        inputBuf = new byte[blockLen];
        outputBuf = new byte[digestLen];
        inputLen = 0;
        blockCount = 0;
    }

    private void adjustDigestLen()
    {
        if (digestLen == 0) {
            digestLen = engineGetDigestLength();
            outputBuf = new byte[digestLen];
        }
    }

    public byte[] digest()
    {
        adjustDigestLen();
        byte[] result = new byte[digestLen];
        digest(result, 0, digestLen);
        return result;
    }

    public byte[] digest(byte[] input)
    {
        update(input, 0, input.length);
        return digest();
    }

    public int digest(byte[] buf, int offset, int len)
    {
        adjustDigestLen();
        if (len >= digestLen) {
            doPadding(buf, offset);
            reset();
            return digestLen;
        } else {
            doPadding(outputBuf, 0);
            System.arraycopy(outputBuf, 0, buf, offset, len);
            reset();
            return len;
        }
    }

    public void reset()
    {
        engineReset();
        inputLen = 0;
        blockCount = 0;
    }

    public void update(byte input)
    {
        inputBuf[inputLen ++] = (byte)input;
        if (inputLen == blockLen) {
            processBlock(inputBuf);
            blockCount ++;
            inputLen = 0;
        }
    }

    public void update(byte[] input)
    {
        update(input, 0, input.length);
    }

    public void update(byte[] input, int offset, int len)
    {
        while (len > 0) {
            int copyLen = blockLen - inputLen;
            if (copyLen > len)
                copyLen = len;
            System.arraycopy(input, offset, inputBuf, inputLen,
                    copyLen);
            offset += copyLen;
            inputLen += copyLen;
            len -= copyLen;
            if (inputLen == blockLen) {
                processBlock(inputBuf);
                blockCount ++;
                inputLen = 0;
            }
        }
    }

    /**
     * Get the internal block length. This is the length (in
     * bytes) of the array which will be passed as parameter to
     * {@link #processBlock}. The default implementation of this
     * method calls {@link #getBlockLength} and returns the same
     * value. Overriding this method is useful when the advertised
     * block length (which is used, for instance, by HMAC) is
     * suboptimal with regards to internal buffering needs.
     *
     * @return  the internal block length (in bytes)
     */
    protected int getInternalBlockLength()
    {
        return getBlockLength();
    }

    /**
     * Flush internal buffers, so that less than a block of data
     * may at most be upheld.
     *
     * @return  the number of bytes still unprocessed after the flush
     */
    protected final int flush()
    {
        return inputLen;
    }

    /**
     * Get a reference to an internal buffer with the same size
     * than a block. The contents of that buffer are defined only
     * immediately after a call to {@link #flush()}: if
     * {@link #flush()} return the value {@code n}, then the
     * first {@code n} bytes of the array returned by this method
     * are the {@code n} bytes of input data which are still
     * unprocessed. The values of the remaining bytes are
     * undefined and may be altered at will.
     *
     * @return  a block-sized internal buffer
     */
    protected final byte[] getBlockBuffer()
    {
        return inputBuf;
    }

    /**
     * Get the "block count": this is the number of times the
     * {@link #processBlock} method has been invoked for the
     * current hash operation. That counter is incremented
     * <em>after</em> the call to {@link #processBlock}.
     *
     * @return  the block count
     */
    protected long getBlockCount()
    {
        return blockCount;
    }

    /**
     * This function copies the internal buffering state to some
     * other instance of a class extending {@code DigestEngine}.
     * It returns a reference to the copy. This method is intended
     * to be called by the implementation of the {@link #copy}
     * method.
     *
     * @param dest   the copy
     * @return  the value {@code dest}
     */
    protected Digest copyState(DigestEngine dest)
    {
        dest.inputLen = inputLen;
        dest.blockCount = blockCount;
        System.arraycopy(inputBuf, 0, dest.inputBuf, 0,
                inputBuf.length);
        adjustDigestLen();
        dest.adjustDigestLen();
        System.arraycopy(outputBuf, 0, dest.outputBuf, 0,
                outputBuf.length);
        return dest;
    }
}
