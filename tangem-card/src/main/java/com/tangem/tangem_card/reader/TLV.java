package com.tangem.tangem_card.reader;

import com.tangem.tangem_card.util.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by dvol on 23.06.2017.
 */

public class TLV {
    public enum Tag {
        TAG_Unknown(0x00),
        TAG_CardID(0x01),
        TAG_Status(0x02),
        TAG_CardPublicKey(0x03),
        TAG_CardSignature(0x04),
        TAG_CurveID(0x05),
        TAG_HashAlgID(0x06),
        TAG_SigningMethod(0x07),
        TAG_MaxSignatures(0x08),
        TAG_PauseBeforePIN2(0x09),
        TAG_SettingsMask(0x0A),
        TAG_CardData(0x0C),
        TAG_NDEFData(0x0D),
        TAG_Health(0x0F),

        TAG_PIN(0x10),
        TAG_PIN2(0x11),
        TAG_NewPIN(0x12),
        TAG_NewPIN2(0x13),
        TAG_NewPIN_Hash(0x14),
        TAG_NewPIN2_Hash(0x15),
        TAG_Challenge(0x16),
        TAG_Salt(0x17),
        TAG_ValidationCounter(0x18),
        TAG_CVC(0x19),

        TAG_Session_Key_A(0x1A),
        TAG_Session_Key_B(0x1B),
        TAG_Pause(0x1C),

        TAG_Manufacture_ID(0x20),
        TAG_Manufacturer_Signature(0x21),

        TAG_Issuer_Data_PublicKey(0x30),
        TAG_Issuer_Transaction_PublicKey(0x31),
        TAG_Issuer_Data(0x32),
        TAG_Issuer_Data_Signature(0x33),
        TAG_Issuer_Transaction_Signature(0x34),
        TAG_Issuer_Data_Counter(0x35),

        TAG_IsActivated(0x3A),
        TAG_ActivationSeed(0x3B),
        TAG_ResetPIN(0x36),

        TAG_CodePageAddress(0x40),
        TAG_CodePageCount(0x41),
        TAG_CodeHash(0x42),

        TAG_TrOut_Hash(0x50),
        TAG_TrOut_HashSize(0x51),
        TAG_TrOut_Raw(0x52),

        TAG_Wallet_PublicKey(0x60),
        TAG_Signature(0x61),
        TAG_RemainingSignatures(0x62),
        TAG_SignedHashes(0x63),

        TAG_Firmware(0x80),
        TAG_Batch(0x81),
        TAG_ManufactureDateTime(0x82),
        TAG_Issuer_ID(0x83),
        TAG_Blockchain_ID(0x84),
        TAG_Manufacturer_PublicKey(0x85),
        TAG_CardID_Manufacturer_Signature(0x86),

        TAG_Token_Symbol(0xA0),
        TAG_Token_Contract_Address(0xA1),
        TAG_Token_Decimal(0xA2),
        TAG_Denomination(0xC0),
        TAG_ValidatedBalance(0xC1),
        TAG_LastSign_Date(0xC2),
        TAG_DenominationText(0xC3);


        Tag(int Code) {
            this.Code = Code;
        }

        public int getCode() {
            return Code;
        }

        private int Code;

        public static Tag ByCode(int Code) {
            Tag[] allTags = Tag.values();
            for (Tag t : allTags) if (t.getCode() == Code) return t;
            return TAG_Unknown;
        }
    }

    private Tag tag;

    public Tag getTag() {
        return tag;
    }

    public byte[] Value;

    public TLV(Tag tag, byte[] value) {
        this.tag = tag;
        this.Value = value;
    }

    public void WriteToStream(ByteArrayOutputStream stream) throws IOException {
        stream.write(tag.getCode());
        if (Value != null) {
            if (Value.length > 0xFE) {
                stream.write(0xFF);
                stream.write((Value.length >> 8) & 0xFF);
                stream.write(Value.length & 0xFF);
            } else {
                stream.write(Value.length & 0xFF);
            }
            stream.write(Value);
        } else {
            stream.write(0x00);
        }
    }

    public static TLV ReadFromStream(ByteArrayInputStream stream) throws IOException {
        int code = stream.read();
        if (code == -1) return null;
        int len = stream.read();
        if (len == -1)
            throw new IOException("Can't read TLV");
        if (len == 0xFF) {
            int lenH = stream.read();
            if (lenH == -1)
                throw new IOException("Can't read TLV");
            len = stream.read();
            if (len == -1)
                throw new IOException("Can't read TLV");
            len |= (lenH << 8);
        }
        byte[] value = new byte[len];
        if (len > 0) {
            if (len != stream.read(value)) {
                throw new IOException("Can't read TLV");
            }
        }
        Tag tag = Tag.ByCode(code);
        TLV result = new TLV(tag, value);
        return result;
    }

    public int getAsInt() {
        return Util.byteArrayToInt(Value);
    }

    public String getAsHexString() {
        return Util.bytesToHex(Value);
    }

    public String getAsString() {
        if( Value.length==0 ) return "";

        if (Value[Value.length - 1] == 0) {
            String s1 = new String(Arrays.copyOfRange(Value, 0, Value.length - 1), Charset.forName("utf-8"));
            return s1.trim();
        } else {
            String s1 = new String(Value, Charset.forName("utf-8"));
            return s1.trim();

        }
    }

    @Override
    public String toString() {
        switch (tag) {
            case TAG_CardData:
            case TAG_Issuer_Data: {
                try {
                    TLVList tlvSub = TLVList.fromBytes(Value);
                    return String.format("%s[%d]: %s (%s)", tag.name(), Value.length, Util.bytesToHex(Value), tlvSub.toString());
                } catch (TLVException e) {
                    e.printStackTrace();
                }
                if (Value != null) {
                    return String.format("%s[%d]: %s (non TLV)", tag.name(), Value.length, Util.bytesToHex(Value));
                } else {
                    return String.format("%s[]: [[NULL]]", tag.name());
                }
            }
            case TAG_CurveID:
            case TAG_HashAlgID:
            case TAG_Blockchain_ID:
            case TAG_Manufacture_ID:
            case TAG_Firmware:
            case TAG_Issuer_ID:
            case TAG_Token_Symbol:
                if (Value != null) {
                    return String.format("%s[%d]: %s(%s)", tag.name(), Value.length, Util.bytesToHex(Value), getAsString());
                } else {
                    return String.format("%s[]: [[NULL]]", tag.name());
                }
            case TAG_SettingsMask: {
                StringBuilder sb=new StringBuilder();
                if( Value!=null ) {
                    try {
                        int iValue = Util.byteArrayToInt(Value);
                        return String.format("%s[%d]: %s (%s)", tag.name(), Value.length, Util.bytesToHex(Value), SettingsMask.getDescription(iValue));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        return String.format("%s[%d]: %s", tag.name(), Value.length, Util.bytesToHex(Value));
                    }
                }else{
                    return String.format("%s[]: [[NULL]]", tag.name());
                }
            }
            default:
                if (Value != null) {
                    return String.format("%s[%d]: %s", tag.name(), Value.length, Util.bytesToHex(Value));
                } else {
                    return String.format("%s[]: [[NULL]]", tag.name());
                }
        }
    }
}
