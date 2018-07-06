package com.tangem.domain.wallet;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tangem.domain.cardReader.CardCrypto;
import com.tangem.util.Util;
import com.tangem.wallet.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dvol on 14.11.2017.
 */

//public enum Issuer {
//    Unknown("Unknown", "Unknown", null, null, null, null),
//    SMART_CASH_AG("SMART CASH AG", "SMART CASH AG",
//            IssuerKeyStorage.sdkPrivateDataKey, IssuerKeyStorage.GeneratePublicKey(IssuerKeyStorage.sdkPrivateDataKey),
//            IssuerKeyStorage.sdkPrivateTransactionKey, IssuerKeyStorage.GeneratePublicKey(IssuerKeyStorage.sdkPrivateTransactionKey)),
//    TANGEM_SDK("TANGEM SDK", "TANGEM SDK",
//            IssuerKeyStorage.sdkPrivateDataKey, IssuerKeyStorage.GeneratePublicKey(IssuerKeyStorage.sdkPrivateDataKey),
//            IssuerKeyStorage.sdkPrivateTransactionKey, IssuerKeyStorage.GeneratePublicKey(IssuerKeyStorage.sdkPrivateTransactionKey)),
//    TANGEM("TANGEM", "TANGEM", null, IssuerKeyStorage.tangemPublicDataKey, null, IssuerKeyStorage.tangemPublicTransactionKey)
//    ;
//
//
//    static class IssuerKeyStorage {
//        private static final byte[] sdkPrivateDataKey = new byte[]{
//                (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0x17, (byte) 0x18,
//                (byte) 0x47, (byte) 0x71, (byte) 0xED, (byte) 0x81, (byte) 0xF2, (byte) 0xBA, (byte) 0xCF, (byte) 0x57,
//                (byte) 0x47, (byte) 0x9E, (byte) 0x47, (byte) 0x35, (byte) 0xEB, (byte) 0x14, (byte) 0x05, (byte) 0x08,
//                (byte) 0x39, (byte) 0x27, (byte) 0x37, (byte) 0x2D, (byte) 0x40, (byte) 0xDA, (byte) 0x9E, (byte) 0x92};
//
//        private static final byte[] sdkPrivateTransactionKey = new byte[]{
//                (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0x17, (byte) 0x18,
//                (byte) 0x47, (byte) 0x71, (byte) 0xED, (byte) 0x81, (byte) 0xF2, (byte) 0xBA, (byte) 0xCF, (byte) 0x57,
//                (byte) 0x47, (byte) 0x9E, (byte) 0x47, (byte) 0x35, (byte) 0xEB, (byte) 0x14, (byte) 0x05, (byte) 0x08,
//                (byte) 0x19, (byte) 0x18, (byte) 0x17, (byte) 0x16, (byte) 0x15, (byte) 0x14, (byte) 0x13, (byte) 0x12};
//
//        private static byte[] tangemPublicDataKey =  {
//                (byte) 0x04 ,
//                (byte) 0x81 ,(byte) 0x96 ,(byte) 0xAA ,(byte) 0x4B ,(byte) 0x41 ,(byte) 0x0A ,(byte) 0xC4 ,(byte) 0x4A,
//                (byte) 0x3B ,(byte) 0x9C ,(byte) 0xCE ,(byte) 0x18 ,(byte) 0xE7 ,(byte) 0xBE ,(byte) 0x22 ,(byte) 0x6A,
//                (byte) 0xEA ,(byte) 0x07 ,(byte) 0x0A ,(byte) 0xCC ,(byte) 0x83 ,(byte) 0xA9 ,(byte) 0xCF ,(byte) 0x67,
//                (byte) 0x54 ,(byte) 0x0F ,(byte) 0xAC ,(byte) 0x49 ,(byte) 0xAF ,(byte) 0x25 ,(byte) 0x12 ,(byte) 0x9F,
//                (byte) 0x6A ,(byte) 0x53 ,(byte) 0x8A ,(byte) 0x28 ,(byte) 0xAD ,(byte) 0x63 ,(byte) 0x41 ,(byte) 0x35,
//                (byte) 0x8E ,(byte) 0x3C ,(byte) 0x4F ,(byte) 0x99 ,(byte) 0x63 ,(byte) 0x06 ,(byte) 0x4F ,(byte) 0x7E,
//                (byte) 0x36 ,(byte) 0x53 ,(byte) 0x72 ,(byte) 0xA6 ,(byte) 0x51 ,(byte) 0xD3 ,(byte) 0x74 ,(byte) 0xE5,
//                (byte) 0xC2 ,(byte) 0x3C ,(byte) 0xDD ,(byte) 0x37 ,(byte) 0xFD ,(byte) 0x09 ,(byte) 0x9B ,(byte) 0xF2};
//
//        private static byte[] tangemPublicTransactionKey =  {
//                (byte) 0x04 ,
//                (byte) 0x34 ,(byte) 0x3D ,(byte) 0x40 ,(byte) 0x49 ,(byte) 0x6C ,(byte) 0xBE ,(byte) 0x1F ,(byte) 0xE8,
//                (byte) 0xA8 ,(byte) 0xC0 ,(byte) 0x26 ,(byte) 0x57 ,(byte) 0x5C ,(byte) 0x43 ,(byte) 0x5A ,(byte) 0x29,
//                (byte) 0x14 ,(byte) 0x1E ,(byte) 0xA3 ,(byte) 0xBC ,(byte) 0x33 ,(byte) 0x5D ,(byte) 0xA5 ,(byte) 0x54,
//                (byte) 0x9A ,(byte) 0xB6 ,(byte) 0xC6 ,(byte) 0x46 ,(byte) 0x85 ,(byte) 0xA6 ,(byte) 0x46 ,(byte) 0x84,
//                (byte) 0x80 ,(byte) 0x36 ,(byte) 0xD4 ,(byte) 0x81 ,(byte) 0xCF ,(byte) 0x9A ,(byte) 0x98 ,(byte) 0x93,
//                (byte) 0x90 ,(byte) 0xA8 ,(byte) 0xB0 ,(byte) 0x34 ,(byte) 0xB2 ,(byte) 0x29 ,(byte) 0xD9 ,(byte) 0x9B,
//                (byte) 0xD4 ,(byte) 0x9E ,(byte) 0x6F ,(byte) 0x07 ,(byte) 0xD2 ,(byte) 0xFF ,(byte) 0x02 ,(byte) 0x74,
//                (byte) 0x6E ,(byte) 0xA2 ,(byte) 0x65 ,(byte) 0xEF ,(byte) 0x99 ,(byte) 0x38 ,(byte) 0x0A ,(byte) 0x80};
//
//        public static byte[] GeneratePublicKey(byte[] privateKey) {
//            try {
//                return CardCrypto.GeneratePublicKey(privateKey);
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//                return null;
//            }
//        }
//    }
//
//    private String ID;
//    private String officialName;
//    private byte[] privateDataKeyArray;
//    private byte[] publicDataKeyArray;
//    private byte[] privateTransactionKeyArray;
//    private byte[] publicTransactionKeyArray;
//
//    Issuer(String id, String officialName, byte[] privateDataKey, byte[] publicDataKey, byte[] privateTransactionKey, byte[] publicTransactionKey) {
//        this.ID = id;
//        this.officialName = officialName;
//        this.privateDataKeyArray = privateDataKey;
//        this.privateTransactionKeyArray = privateTransactionKey;
//        this.publicDataKeyArray = publicDataKey;
//        this.publicTransactionKeyArray = publicTransactionKey;
//    }
//
//    public byte[] getPublicDataKey() {
//        return publicDataKeyArray;
//    }
//
//    public byte[] getPublicTransactionKey() {
//        return publicTransactionKeyArray;
//    }
//
//    public byte[] getPrivateDataKey() {
//        return privateDataKeyArray;
//    }
//
//    public byte[] getPrivateTransactionKey() {
//        return privateTransactionKeyArray;
//    }
//
//    public byte[] getID() {
//        return ID.getBytes();
//    }
//
//    public String getOfficialName() {
//        return officialName;
//    }
//
//    public static Issuer FindIssuer(String ID, byte[] publicDataKey) {
//        Issuer[] issuers = Issuer.values();
//        for (int i = 1; i < issuers.length; i++) {
//            if (issuers[i].ID.equals(ID) && Arrays.equals(issuers[i].getPublicDataKey(), publicDataKey)) {
//                return issuers[i];
//            }
//        }
//        return Issuer.Unknown;
//    }
//
//}

public class Issuer {

    static class KeyPair
    {
        String privateKey;
        String publicKey;

        byte[] getPrivateKey() throws Exception {
            if (privateKey != null) {
                return CardCrypto.GeneratePublicKey(Util.hexToBytes(privateKey));
            } else {
                throw new Exception("No private key!");
            }
        }

        byte[] getPublicKey() throws Exception {
            if (publicKey == null) {
                if (privateKey != null) {
                    return CardCrypto.GeneratePublicKey(Util.hexToBytes(privateKey));
                } else {
                    throw new Exception("Invalid key format: no public and no private");
                }
            } else {
                return Util.hexToBytes(publicKey);
            }
        }

    }
    String id;
    String officialName;
    KeyPair dataKey;
    KeyPair transactionKey;

    public String getID() {
        return id;
    }

    private static List<Issuer> instances=new ArrayList<>();

    public static boolean needInit() {
        return instances.size()==0;
    }

    public static void Init(Context mContext){
        try {
            JsonArray jaIssuers;
            JsonParser jsonParser=new JsonParser();
            jaIssuers=jsonParser.parse(ReadJSONResource(mContext, R.raw.issuers)).getAsJsonArray();
            instances.clear();

            Issuer unknown=new Issuer();
            unknown.id="UNKNOWN";
            unknown.officialName="UNKNOWN";
            instances.add(unknown);

            for(JsonElement jeIssuer: jaIssuers)
            {
                Issuer instance=new Gson().fromJson(jeIssuer,Issuer.class);
                instances.add(instance);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String ReadJSONResource(Context mContext, int id) {
        Resources resources = mContext.getResources();
        InputStream resourceReader = resources.openRawResource(id);
        Writer writer = new StringWriter();
        try {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(resourceReader, "UTF-8"))) {
                String line = reader.readLine();
                while (line != null) {
                    writer.write(line);
                    line = reader.readLine();
                }
            }
        } catch (Exception e) {
            Log.e("ReadJSONResource", "Unhandled exception while using JSONResourceReader", e);
        } finally {
            try {
                resourceReader.close();
            } catch (Exception e) {
                Log.e("ReadJSONResource", "Unhandled exception while using JSONResourceReader", e);
            }
        }

        return writer.toString();
    }

    public byte[] getPublicDataKey() throws Exception {
        if( dataKey==null )
            throw new Exception("Data key not specified!");
        return dataKey.getPublicKey();
    }

    public byte[] getPublicTransactionKey() throws Exception {
        if( dataKey==null )
            throw new Exception("Transaction key not specified!");
        return transactionKey.getPublicKey();
    }

    public byte[] getPrivateDataKey() throws Exception {
        if( dataKey==null )
            throw new Exception("Data key not specified!");
        return dataKey.getPrivateKey();
    }

    public byte[] getPrivateTransactionKey() throws Exception {
        if( transactionKey==null )
            throw new Exception("Transaction key not specified!");
        return transactionKey.getPrivateKey();
    }

    public String getOfficialName() {
        return officialName!=null?officialName:id;
    }

    public static Issuer FindIssuer(String ID) {
        for (int i = 0; i < instances.size(); i++) {
            try {
                if (instances.get(i).id.equals(ID)) {
                    return instances.get(i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Unknown();
    }

    public static Issuer FindIssuer(String ID, byte[] publicDataKey) {
        for (int i = 0; i < instances.size(); i++) {
            try {
                if (instances.get(i).id.equals(ID) && Arrays.equals(instances.get(i).getPublicDataKey(), publicDataKey)) {
                    return instances.get(i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Unknown();
    }

    public static Issuer Unknown() {
        return instances.get(0);
    }
}
