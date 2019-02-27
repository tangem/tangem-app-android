package com.tangem.tangemcommon.reader;

import com.tangem.tangemcommon.util.Util;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class ResponseApdu {

    private int mSw1 = 0x00;
    private int mSw2 = 0x00;

    private byte[] mData = new byte[0];
    private byte[] mBytes = new byte[0];

    private TLVList tlvList = new TLVList();

    private String parseError = null;

    private ResponseApdu() {
    }

    ResponseApdu(byte[] respApdu) {
        if (respApdu.length < 2) {
            return;
        }
        if (respApdu.length > 2) {
            mData = new byte[respApdu.length - 2];
            System.arraycopy(respApdu, 0, mData, 0, respApdu.length - 2);

            try {
                tlvList = TLVList.fromBytes(mData);
            } catch (TLVException e) {
                parseError = e.getMessage();
            }

        }else{
            tlvList=new TLVList();
            parseError=null;
        }
        mSw1 = 0x00FF & respApdu[respApdu.length - 2];
        mSw2 = 0x00FF & respApdu[respApdu.length - 1];
        mBytes = respApdu;
    }

    public static boolean isStatusWord(byte[] respApdu, int SW)
    {
        int mSw1 = 0x00FF & respApdu[respApdu.length - 2];
        int mSw2 = 0x00FF & respApdu[respApdu.length - 1];
        return ((mSw1 << 8) | mSw2)==SW;
    }

    public static ResponseApdu Decrypt(byte[] data, byte[] key) throws Exception{

        if( data.length==2 )
        {
            ResponseApdu responseApdu = new ResponseApdu();
            responseApdu.mSw1 = ((int) data[0] & 0xFF);
            responseApdu.mSw2 = ((int) data[1] & 0xFF);
            return responseApdu;
        }else if( data.length>=18 ){
            byte[] decryptedData = CardCrypto.Decrypt(key, Arrays.copyOfRange(data, 0, data.length - 2),true);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(decryptedData);
            byte[] baLength = new byte[2];
            inputStream.read(baLength);
            int length = ((int) baLength[0] & 0xFF) * 256 + ((int) baLength[1] & 0xFF);
            if (length > decryptedData.length - 4)
                throw new Exception("Can't decrypt - data size invalid");
            byte[] baCRC = new byte[2];
            inputStream.read(baCRC);
            byte[] answerData = new byte[length];
            inputStream.read(answerData);
            byte[] crc = Util.calculateCRC16(answerData);
            if (!Arrays.equals(baCRC, crc)) throw new Exception("Can't decrypt - crc invalid");

            ResponseApdu responseApdu = new ResponseApdu();
            responseApdu.mSw1 = ((int) data[data.length - 2] & 0xFF);
            responseApdu.mSw2 = ((int) data[data.length - 1] & 0xFF);
            responseApdu.mBytes = data;
            responseApdu.mData = answerData;

            try {
                responseApdu.tlvList = TLVList.fromBytes(answerData);
            } catch (TLVException e) {
                responseApdu.parseError = e.getMessage();
            }

            return responseApdu;
        }else{
            throw new Exception("Can't decrypt - data size to small");
        }
    }


    public int getSW1() {
        return mSw1;
    }

    public int getSW2() {
        return mSw2;
    }

    public int getSW1SW2() {
        return (mSw1 << 8) | mSw2;
    }

    public byte[] getData() {
        return mData;
    }

    public TLVList getTLVs() {
        return tlvList;
    }

    public boolean isParsedWithError() {
        return parseError != null;
    }
    public String getParseErroMessage() {
        return parseError;
    }
    public byte[] toBytes() {
        return mBytes;
    }

    public boolean isStatus(int sw1sw2) {
        if (getSW1SW2() == sw1sw2) {
            return true;
        } else {
            return false;
        }
    }

    public String getSW1SW2Description() {
        return SW.getDescription(getSW1SW2());
    }
}
