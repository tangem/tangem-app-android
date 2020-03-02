package com.tangem.wallet.bch;

import java.math.BigInteger;
import java.util.Arrays;


/**
 * Copyright (c) 2018 Tobias Brandt
 *
 * Distributed under the MIT software license, see the accompanying file LICENSE
 * or http://www.opensource.org/licenses/mit-license.php.
 */


public class CashAddr {

    public static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    private static final char[] CHARS = CHARSET.toCharArray();


    public static final String SEPARATOR = ":";

    public static final String MAIN_NET_PREFIX = "bitcoincash";

    public static final String TEST_NET_PREFIX = "bchtest";

    public static final String ASSUMED_DEFAULT_PREFIX = MAIN_NET_PREFIX;

    private static final BigInteger[] POLYMOD_GENERATORS = new BigInteger[] { new BigInteger("98f2bc8e61", 16),
            new BigInteger("79b76d99e2", 16), new BigInteger("f33e5fb3c4", 16), new BigInteger("ae2eabe2a8", 16),
            new BigInteger("1e4f43e470", 16) };

    private static final BigInteger POLYMOD_AND_CONSTANT = new BigInteger("07ffffffff", 16);

    public static String toCashAddress(BitcoinCashAddressType addressType, byte[] hash) {
        String prefixString =  MAIN_NET_PREFIX;
        byte[] prefixBytes = getPrefixBytes(prefixString);
        byte[] payloadBytes = concatenateByteArrays(new byte[] { addressType.getVersionByte() }, hash);
        payloadBytes = convertBits(payloadBytes, 8, 5, false);
        byte[] allChecksumInput = concatenateByteArrays(
                concatenateByteArrays(concatenateByteArrays(prefixBytes, new byte[] { 0 }), payloadBytes),
                new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 });
        byte[] checksumBytes = calculateChecksumBytesPolymod(allChecksumInput);
        checksumBytes = convertBits(checksumBytes, 8, 5, true);
        String cashAddress = BitcoinCashBase32.encode(concatenateByteArrays(payloadBytes, checksumBytes));
        return prefixString + SEPARATOR + cashAddress;
    }

    public static BitcoinCashAddressDecodedParts decodeCashAddress(String bitcoinCashAddress) {
        if (!isValidCashAddress(bitcoinCashAddress)) {
            throw new RuntimeException("Address wasn't valid: " + bitcoinCashAddress);
        }

        BitcoinCashAddressDecodedParts decoded = new BitcoinCashAddressDecodedParts();
        String[] addressParts = bitcoinCashAddress.split(SEPARATOR);
        if (addressParts.length == 2) {
            decoded.setPrefix(addressParts[0]);
        } else {
            decoded.setPrefix(MAIN_NET_PREFIX);
        }

        byte[] addressData = BitcoinCashBase32.decode(addressParts[addressParts.length - 1]);
        addressData = Arrays.copyOfRange(addressData, 0, addressData.length - 8);
        addressData = BitcoinCashBitArrayConverter.convertBits(addressData, 5, 8, true);
        byte versionByte = addressData[0];
        byte[] hash = Arrays.copyOfRange(addressData, 1, addressData.length);

        decoded.setAddressType(getAddressTypeFromVersionByte(versionByte));
        decoded.setHash(hash);

        return decoded;
    }

    private static BitcoinCashAddressType getAddressTypeFromVersionByte(byte versionByte) {
        for (BitcoinCashAddressType addressType : BitcoinCashAddressType.values()) {
            if (addressType.getVersionByte() == versionByte) {
                return addressType;
            }
        }

        throw new RuntimeException("Unknown version byte: " + versionByte);
    }



    public static boolean isValidCashAddress(String bitcoinCashAddress ) {
        try {
            if (!isSingleCase(bitcoinCashAddress))
                return false;

            bitcoinCashAddress = bitcoinCashAddress.toLowerCase();
            String prefix;

            if (bitcoinCashAddress.contains(SEPARATOR)) {
                String[] split = bitcoinCashAddress.split(SEPARATOR);
                prefix = split[0];
                if (!prefix.equals(MAIN_NET_PREFIX)) {return false;} //for now we use main net only
                bitcoinCashAddress = split[1];
            } else {
                prefix = MAIN_NET_PREFIX;
            }
            if (!bitcoinCashAddress.startsWith("q")) {return false;} //for now we use P2PKH addresses only

            byte[] checksumData =  concatenateByteArrays(
                    concatenateByteArrays(getPrefixBytes(prefix ), new byte[] { 0x00 }),
                    BitcoinCashBase32.decode(bitcoinCashAddress));

            byte[] calculateChecksumBytesPolymod = calculateChecksumBytesPolymod(checksumData);
            return new BigInteger(calculateChecksumBytesPolymod).compareTo(BigInteger.ZERO) == 0;
        } catch (RuntimeException re) {
            return false;
        }
    }



    private static boolean isSingleCase(String bitcoinCashAddress) {
        if (bitcoinCashAddress.equals(bitcoinCashAddress.toLowerCase())) {
            return true;
        }
        if (bitcoinCashAddress.equals(bitcoinCashAddress.toUpperCase())) {
            return true;
        }

        return false;
    }

    /**
     * @param checksumInput
     * @return Returns a 40 bits checksum in form of 5 8-bit arrays. This still has
     *         to me mapped to 5-bit array representation
     */
    private static byte[] calculateChecksumBytesPolymod(byte[] checksumInput) {
        BigInteger c = BigInteger.ONE;

        for (int i = 0; i < checksumInput.length; i++) {
            byte c0 = c.shiftRight(35).byteValue();
            c = c.and(POLYMOD_AND_CONSTANT).shiftLeft(5)
                    .xor(new BigInteger(String.format("%02x", checksumInput[i]), 16));

            if ((c0 & 0x01) != 0)
                c = c.xor(POLYMOD_GENERATORS[0]);
            if ((c0 & 0x02) != 0)
                c = c.xor(POLYMOD_GENERATORS[1]);
            if ((c0 & 0x04) != 0)
                c = c.xor(POLYMOD_GENERATORS[2]);
            if ((c0 & 0x08) != 0)
                c = c.xor(POLYMOD_GENERATORS[3]);
            if ((c0 & 0x10) != 0)
                c = c.xor(POLYMOD_GENERATORS[4]);
        }

        byte[] checksum = c.xor(BigInteger.ONE).toByteArray();
        if (checksum.length == 5) {
            return checksum;
        } else {
            byte[] newChecksumArray = new byte[5];

            System.arraycopy(checksum, Math.max(0, checksum.length - 5), newChecksumArray,
                    Math.max(0, 5 - checksum.length), Math.min(5, checksum.length));

            return newChecksumArray;
        }

    }

    private static byte[] getPrefixBytes(String prefixString ) {
        byte[] prefixBytes = new byte[prefixString.length()];

        char[] charArray = prefixString.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            prefixBytes[i] = (byte) (charArray[i] & 0x1f);
        }

        return prefixBytes;
    }

    private static byte[] concatenateByteArrays(byte[] first, byte[] second) {
        byte[] concatenatedBytes = new byte[first.length + second.length];

        System.arraycopy(first, 0, concatenatedBytes, 0, first.length);
        System.arraycopy(second, 0, concatenatedBytes, first.length, second.length);

        return concatenatedBytes;
    }

    private static byte[] convertBits(byte[] bytes8Bits, int from, int to, boolean strictMode) {
        //Copyright (c) 2017 Pieter Wuille

        int length = (int) (strictMode ? Math.floor((double) bytes8Bits.length * from / to)
                : Math.ceil((double) bytes8Bits.length * from / to));
        int mask = ((1 << to) - 1) & 0xff;
        byte[] result = new byte[length];
        int index = 0;
        int accumulator = 0;
        int bits = 0;
        for (int i = 0; i < bytes8Bits.length; i++) {
            byte value = bytes8Bits[i];
            accumulator = (((accumulator & 0xff) << from) | (value & 0xff));
            bits += from;
            while (bits >= to) {
                bits -= to;
                result[index] = (byte) ((accumulator >> bits) & mask);
                ++index;
            }
        }
        if (!strictMode) {
            if (bits > 0) {
                result[index] = (byte) ((accumulator << (to - bits)) & mask);
                ++index;
            }
        } else {
            if (!(bits < from && ((accumulator << (to - bits)) & mask) == 0)) {
                throw new RuntimeException("Strict mode was used but input couldn't be converted without padding");
            }
        }

        return result;
    }




}
