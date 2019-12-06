package com.tangem.tangem_card.data;

import com.tangem.tangem_card.reader.CardCrypto;
import com.tangem.tangem_card.util.PBKDF2;
import com.tangem.tangem_card.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dvol on 14.11.2017.
 */

public class Issuer {

    static class KeyPair {
        String privateKey;
        String privateKeyEncrypted;
        String publicKey;


        byte[] getPrivateKey() throws Exception {
            if (privateKey != null) {
                return Util.hexToBytes(privateKey);
            } else if (privateKeyEncrypted != null) {
                byte[] A = PBKDF2.deriveKey(Util.calculateSHA256(Tangem().getPrivateTransactionKey()),Util.calculateSHA256(Tangem().getPrivateDataKey()), 100);
                //String B = Util.bytesToHex(CardCrypto.Encrypt(A, Util.hexToBytes(""), true));
                try {
                    return CardCrypto.Decrypt(A, Util.hexToBytes(privateKeyEncrypted), true);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    throw new Exception("No private key!");
                }

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

    public String id;
    public String officialName;
    private KeyPair dataKey;
    private KeyPair transactionKey;

    public String getID() {
        return id;
    }

    private static List<Issuer> instances = new ArrayList<>();

    static {
        Issuer unknown = new Issuer();
        unknown.id = "UNKNOWN";
        unknown.officialName = "UNKNOWN";
        instances.add(unknown);
    }

    public static void fillIssuers(List<Issuer> issuers) {
        instances.addAll(issuers);
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

    public static Issuer Tangem() {
        return instances.get(1);
    }

}
