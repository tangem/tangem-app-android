package com.tangem.util;

import com.tangem.wallet.btc.BitcoinOutputStream;

import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DERSequenceGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Created by Ilia on 15.02.2018.
 */

public class DerEncodingUtil {

    public static byte[] PackInteger(byte[] s)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte)0x02);

        byte length = (byte)s.length;
        if (s[0] > 0x7f) {
            baos.write((byte)(length+1));
            baos.write((byte) 0x00);
        }
        else {
            baos.write((byte)length);
        }

        for(int i = 0; i < length; ++i)
            baos.write(s[i]);

        return baos.toByteArray();
    }

    public static byte[] packSignDer(BigInteger r, BigInteger s, byte[] pubKey) throws IOException
    {
        byte[] signDer = DerEncoding(r, s);
        BitcoinOutputStream packKey = new BitcoinOutputStream();

        packKey.write((byte)0x41);
        packKey.write(pubKey);

        byte[] keyArray = packKey.toByteArray();

        BitcoinOutputStream result = new BitcoinOutputStream();
        result.write((byte)(signDer.length+1));
        result.write(signDer);
        result.write((byte)0x1);

        result.write(keyArray);

        return result.toByteArray();

    }

    public static byte[] packSignDerBitcoinCash(BigInteger r, BigInteger s, byte[] pubKey) throws IOException
    {
        byte[] signDer = DerEncoding(r, s);
        BitcoinOutputStream packKey = new BitcoinOutputStream();

        packKey.write((byte)0x21); //compress key
        packKey.write(pubKey);

        byte[] keyArray = packKey.toByteArray();

        BitcoinOutputStream result = new BitcoinOutputStream();
        result.write((byte)(signDer.length+1));
        result.write(signDer);
        result.write((byte)0x41);

        result.write(keyArray);

        return result.toByteArray();

    }


    public static byte[] DerEncoding(BigInteger r, BigInteger s) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(72);
        DERSequenceGenerator seq = new DERSequenceGenerator(bos);
        seq.addObject(new ASN1Integer(r));
        seq.addObject(new ASN1Integer(s));
        seq.close();
        return bos.toByteArray();
    }

//    public static byte[] DerEncoding(byte[] sign)
//    {
//        byte[] r = sign;
//        byte[] s = new byte[32];
//        for(int i =0; i < 32; ++i)
//        {
//            s[i] = sign[i+32];
//        }
//
//        byte[] newR = PackInteger(r);
//        byte[] newS = PackInteger(s);
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        baos.write((byte)(newR.length+newS.length+2));
//        baos.write((byte)newR.length);
//        baos.write(newR, 0, newR.length);
//        baos.write((byte)newS.length);
//        baos.write(newS, 0, newS.length);
//
//        return baos.toByteArray();
//    }
//
//    public static byte[] DerEncodingBI(BigInteger[] sign)
//    {
//        byte[] r = sign[0].toByteArray();
//        byte[] s = sign[1].toByteArray();
//
//        byte[] newR = PackInteger(r);
//        byte[] newS = PackInteger(s);
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//        baos.write((byte)(newR.length+newS.length+2));
//
//        baos.write((byte)newR.length);
//        baos.write(newR, 0, newR.length);
//
//        baos.write((byte)newS.length);
//        baos.write(newS, 0, newS.length);
//
//        return baos.toByteArray();
//    }
}
