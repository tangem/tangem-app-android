package com.tangem.card_common.reader;

/**
 * Created by dvol on 23.06.2017.
 */

import com.tangem.card_common.util.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class TLVList extends ArrayList<TLV> {
    public String getParsedTLVs(String Prefix) {
        String parsed = "";
        for (int i = 0; i < size(); i++) {
            parsed += Prefix + this.get(i).toString() + (i < size() - 1 ? "\n" : "");
        }
        return parsed;//.substring(0,parsed.length()-2);
    }

    public TLVList() {
        super();
    }

    public TLVList(Collection<? extends TLV> c) {
        super(c);
    }

    public TLV getTLV(TLV.Tag tag) {
        for (TLV tlv : this) {
            if (tlv.getTag() == tag) return tlv;
        }
        return null;
    }

    public int getTagAsInt(TLV.Tag tag) {
        TLV tlv = getTLV(tag);
        return Util.byteArrayToInt(tlv.Value);
    }

    public byte[] toBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (TLV tlv : this) {
            try {
                tlv.WriteToStream(stream);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        return stream.toByteArray();
    }

    public static TLVList fromBytes(byte[] mData) throws TLVException {
        TLVList tlvList = new TLVList();
        ByteArrayInputStream stream = new ByteArrayInputStream(mData);
        TLV tlv = null;
        do {
            try {
                tlv = TLV.ReadFromStream(stream);
                if (tlv != null) tlvList.add(tlv);
            } catch (IOException e) {
                throw new TLVException("TLVError: " + e.getMessage());
            }
        }
        while (tlv != null);
        return tlvList;
    }
}
