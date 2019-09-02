package com.tangem.tangem_card.reader;

import com.tangem.tangem_card.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class CommandApdu {

    public static final byte ISO_CLA = (byte) 0x00;

    protected String mCmdName;
    protected int mCla = 0x00;
    protected int mIns = 0x00;
    protected int mP1 = 0x00;
    protected int mP2 = 0x00;
    protected int mLc = 0x00;

    protected byte[] mData = new byte[0];

    protected int mLe = 0x00;
    protected boolean mLeUsed = false;
    protected TLVList tlvList = new TLVList();

    public CommandApdu() {
    }

    public CommandApdu(int cla, int ins, int p1, int p2) {
        setCommandName(ins);
        mCla = cla;
        mIns = ins;
        mP1 = p1;
        mP2 = p2;
    }

    public CommandApdu(int cla, int ins, int p1, int p2, byte[] data) {
        setCommandName(ins);
        mCla = cla;
        mIns = ins;
        mLc = data.length;
        mP1 = p1;
        mP2 = p2;
        mData = data;
    }

    public CommandApdu(INS ins) {
        setCommandName(ins.name());
        mCla = ISO_CLA;
        mIns = ins.Code;
        mP1 = 0;
        mP2 = 0;
    }

    public CommandApdu(int cla, int ins, int p1, int p2, byte[] data, int le) {
        setCommandName(ins);
        mCla = cla;
        mIns = ins;
        mLc = data.length;
        mP1 = p1;
        mP2 = p2;
        mData = data;
        mLe = le;
        mLeUsed = true;
    }

    public CommandApdu(int cla, int ins, int p1, int p2, int le) {
        setCommandName(ins);
        mCla = cla;
        mIns = ins;
        mP1 = p1;
        mP2 = p2;
        mLe = le;
        mLeUsed = true;
    }

    public void setCommandName(String cmdName) {
        mCmdName = cmdName;
    }

    private void setCommandName(int ins) {
        INS ins1 = INS.ByCode(ins);
        if (ins1 != null) {
            mCmdName = ins1.toString();
        } else {
            mCmdName = String.format("INS[%2X]", ins);
        }
    }

    public String getCommandName() {
        return mCmdName;
    }

    public void setP1(int p1) {
        mP1 = p1;
    }

    public void setP2(int p2) {
        mP2 = p2;
    }

    public void setData(byte[] data) {
        mLc = data.length;
        mData = data;
    }

    public void addTLV(TLV.Tag tag, byte[] value) {
        tlvList.add(new TLV(tag, value));
    }

    public void addTLV_U8(TLV.Tag tag, int U8) {
        addTLV(tag, new byte[]{(byte) U8});
    }

    public void addTLV_U16(TLV.Tag tag, int U16) {
        addTLV(tag, Util.intToByteArray2(U16));
    }

    public void addTLV_U32(TLV.Tag tag, int U32) {
        addTLV(tag, Util.intToByteArray4(U32));
    }

    public void setLe(int le) {
        mLe = le;
        mLeUsed = true;
    }

    public int getP1() {
        return mP1;
    }

    public int getP2() {
        return mP2;
    }

    public int getLc() {
        return mLc;
    }

    public byte[] getData() {
        return mData;
    }

    public TLVList getTLVs() {
        return tlvList;
    }

    public int getLe() {
        return mLe;
    }

    public static String toString(byte[] cmdApdu, int Lc) {
        String cmd = Util.bytesToHex(cmdApdu);
        if (Lc == 0) return cmd;
        return cmd.substring(0, 8) + " " + cmd.substring(8, 10) + " " +
                cmd.substring(10, 10 + Lc * 2) + " " + cmd.substring(10 + Lc * 2, cmd.length());
    }


    public void Crypt(byte[] key) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidAlgorithmParameterException, NoSuchProviderException {
        if (tlvList.size() != 0) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            for (TLV tlv : tlvList) {
                try {
                    tlv.WriteToStream(stream);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }

            mData = stream.toByteArray();
            byte[] crc = Util.calculateCRC16(mData);
            stream = new ByteArrayOutputStream();
            stream.write(Util.intToByteArray2(mData.length));
            stream.write(crc);
            stream.write(mData);
            mData = stream.toByteArray();

            byte[] mEncryptedData = CardCrypto.Encrypt(key, mData);

            mData = mEncryptedData;
            mLc = mData.length;

            tlvList.clear();

        }

    }

    public byte[] toBytes() {
        int length = 4; // CLA, INS, P1, P2

        if (tlvList.size() != 0) {
            mData = tlvList.toBytes();
            mLc = mData.length;
        }

        if (mData.length != 0) {
            length += 1; // LC
            if (mLc >= 256)
            length += 2;
            length += mData.length; // DATA
        }
        if (mLeUsed) {
            length += 1; // LE
            if (mLc >= 256)
            length += 2;
        }

        byte[] apdu = new byte[length];

        int index = 0;
        apdu[index] = (byte) mCla;
        index++;
        apdu[index] = (byte) mIns;
        index++;
        apdu[index] = (byte) mP1;
        index++;
        apdu[index] = (byte) mP2;
        index++;
        if (mLc != 0) {
            if (mLc < 256) {
                apdu[index] = (byte) mLc;
                index++;
            } else {
                apdu[index] = 0;
                index++;
                apdu[index] = (byte) (mLc >> 8);
                index++;
                apdu[index] = (byte) (mLc & 0xFF);
                index++;
            }

            System.arraycopy(mData, 0, apdu, index, mData.length);
            index += mData.length;
        }
        if (mLeUsed) {
            if (mLc < 256) {
                apdu[index] += (byte) mLe; // LE
            } else {
                apdu[index] = 0;
                index++;
                apdu[index] = (byte) (mLe >> 8);
                index++;
                apdu[index] = (byte) (mLe & 0xFF);
                index++;
            }
        }

        return apdu;
    }

    public CommandApdu clone() {
        CommandApdu apdu = new CommandApdu();
        apdu.setCommandName(mCmdName);
        apdu.mCla = mCla;
        apdu.mIns = mIns;
        apdu.mP1 = mP1;
        apdu.mP2 = mP2;
        apdu.mLc = mLc;
        apdu.mData = new byte[mData.length];
        System.arraycopy(mData, 0, apdu.mData, 0, mData.length);
        apdu.mLe = mLe;
        apdu.mLeUsed = mLeUsed;
        apdu.tlvList = new TLVList(tlvList);
        return apdu;
    }
}
