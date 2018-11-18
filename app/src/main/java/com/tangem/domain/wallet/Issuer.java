package com.tangem.domain.wallet;

import android.content.Context;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tangem.domain.cardReader.CardCrypto;
import com.tangem.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dvol on 14.11.2017.
 */

public class Issuer {

    static class KeyPair {
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

    private String id;
    private String officialName;
    private KeyPair dataKey;
    private KeyPair transactionKey;

    public String getID() {
        return id;
    }

    private static List<Issuer> instances = new ArrayList<>();

    public static boolean needInit() {
        return instances.size() == 0;
    }

    public static void init(Context context) {
        try {
            Issuer unknown = new Issuer();
            unknown.id = "UNKNOWN";
            unknown.officialName = "UNKNOWN";

            try (InputStream is = context.getAssets().open("issuers.json")) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<List<Issuer>>() {
                    }.getType();
                    instances = new Gson().fromJson(reader, listType);
                }
            }
            instances.add(0, unknown);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getPublicDataKey() throws Exception {
        if (dataKey == null)
            throw new Exception("Data key not specified!");
        return dataKey.getPublicKey();
    }

    public byte[] getPublicTransactionKey() throws Exception {
        if (dataKey == null)
            throw new Exception("Transaction key not specified!");
        return transactionKey.getPublicKey();
    }

    public byte[] getPrivateDataKey() throws Exception {
        if (dataKey == null)
            throw new Exception("Data key not specified!");
        return dataKey.getPrivateKey();
    }

    public byte[] getPrivateTransactionKey() throws Exception {
        if (transactionKey == null)
            throw new Exception("Transaction key not specified!");
        return transactionKey.getPrivateKey();
    }

    public String getOfficialName() {
        return officialName != null ? officialName : id;
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
        for (int i = 1; i < instances.size(); i++) {
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
