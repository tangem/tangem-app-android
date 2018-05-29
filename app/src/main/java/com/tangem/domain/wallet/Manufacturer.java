package com.tangem.domain.wallet;

/**
 * Created by dvol on 09.08.2017.
 */

public enum Manufacturer {
//    Unknown("", "Unknown", new byte[]{}),
//    SMARTCASH_AG("SMART CASH AG","SMART CASH AG",
//            new byte[]{0x04,
//                    (byte) 0x4F, (byte) 0x53, (byte) 0x90, (byte) 0x2D, (byte) 0x50, (byte) 0xE2, (byte) 0xBB, (byte) 0x16,
//                    (byte) 0xD3, (byte) 0xDD, (byte) 0xC7, (byte) 0xA2, (byte) 0x03, (byte) 0x97, (byte) 0x28, (byte) 0x5E,
//                    (byte) 0x94, (byte) 0x21, (byte) 0x53, (byte) 0x69, (byte) 0x59, (byte) 0x8C, (byte) 0xE4, (byte) 0xDD,
//                    (byte) 0x62, (byte) 0x42, (byte) 0xDD, (byte) 0xB4, (byte) 0x5B, (byte) 0x96, (byte) 0xA1, (byte) 0x03,
//                    (byte) 0x1A, (byte) 0xF5, (byte) 0xC9, (byte) 0x73, (byte) 0x94, (byte) 0xC6, (byte) 0xF9, (byte) 0xC8,
//                    (byte) 0xD7, (byte) 0x6F, (byte) 0x38, (byte) 0xF9, (byte) 0x65, (byte) 0xCB, (byte) 0xA8, (byte) 0xAE,
//                    (byte) 0x85, (byte) 0xAF, (byte) 0xF7, (byte) 0x68, (byte) 0x55, (byte) 0xDC, (byte) 0xAA, (byte) 0x08,
//                    (byte) 0xF3, (byte) 0xCD, (byte) 0x15, (byte) 0x43, (byte) 0x04, (byte) 0x19, (byte) 0xF4, (byte) 0x49}),
//    DEVELOPERS_SMARTCASH_AG("DEVELOP CASH AG","SMART CASH AG (DEVELOPERS)",
//            new byte[]{0x04,
//                    (byte) 0xBA, (byte) 0xB8, (byte) 0x6D, (byte) 0x56, (byte) 0x29, (byte) 0x8C, (byte) 0x99, (byte) 0x6F,
//                    (byte) 0x56, (byte) 0x4A, (byte) 0x84, (byte) 0xFC, (byte) 0x88, (byte) 0xE2, (byte) 0x8A, (byte) 0xED,
//                    (byte) 0x38, (byte) 0x18, (byte) 0x4B, (byte) 0x12, (byte) 0xF0, (byte) 0x7E, (byte) 0x51, (byte) 0x91,
//                    (byte) 0x13, (byte) 0xBE, (byte) 0xF4, (byte) 0x8C, (byte) 0x76, (byte) 0xF3, (byte) 0xDF, (byte) 0x3A,
//
//                    (byte) 0xDC, (byte) 0x30, (byte) 0x35, (byte) 0x99, (byte) 0xB0, (byte) 0x8A, (byte) 0xC0, (byte) 0x5B,
//                    (byte) 0x55, (byte) 0xEC, (byte) 0x3D, (byte) 0xF9, (byte) 0x8D, (byte) 0x93, (byte) 0x38, (byte) 0x57,
//                    (byte) 0x3A, (byte) 0x62, (byte) 0x42, (byte) 0xF7, (byte) 0x6F, (byte) 0x5D, (byte) 0x28, (byte) 0xF4,
//                    (byte) 0xF0, (byte) 0xF3, (byte) 0x64, (byte) 0xE8, (byte) 0x7E, (byte) 0x8F, (byte) 0xCA, (byte) 0x2F});


    Unknown("", "Unknown"),
    SMARTCASH_AG("SMART CASH AG", "SMART CASH AG"),
    DEVELOPERS_SMARTCASH_AG("DEVELOP CASH AG", "SMART CASH AG (DEVELOPERS)"),
    SMARTCASH("SMART CASH", "SMART CASH");

    private String ID;
    private String officialName;
//    private byte[] publicKey;

    //    Manufacturer(String id, String officialName, byte[] publicKey) {
    Manufacturer(String id, String officialName) {
        this.ID = id;
        this.officialName = officialName;
//        this.publicKey = publicKey;
    }

    public String getOfficialName() {
        return officialName;
    }

//    public boolean VerifySignature(byte[] Challenge, byte[] Salt, byte[] Signature) {
//
//        if (publicKey == null || Challenge == null || Salt == null || Signature == null) {
//            Log.e(getOfficialName(), "Not all data read, can't check signature!");
//            return false;
//        }
//        try {
//            java.security.Signature signature = java.security.Signature.getInstance("SHA256withECDSA");
//            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
//            KeyFactory factory = KeyFactory.getInstance("EC", "SC");
//
//            ECPoint p1 = spec.getCurve().decodePoint(publicKey);
//
//            ECPublicKeySpec keySpec = new ECPublicKeySpec(p1, spec);
//
//            PublicKey publicKey = factory.generatePublic(keySpec);
//            signature.initVerify(publicKey);
//            signature.update(Challenge);
//            signature.update(Salt);
//
//            ASN1EncodableVector v = new ASN1EncodableVector();
//            int size = Signature.length / 2;
//            v.add(/*r*/new ASN1Integer(new BigInteger(1, Arrays.copyOfRange(Signature, 0, size))));
//            v.add(/*s*/new ASN1Integer(new BigInteger(1, Arrays.copyOfRange(Signature, size, size * 2))));
//            byte[] sigDer = new DERSequence(v).getEncoded();
//
//            if (signature.verify(sigDer)) {
//                Log.i(getOfficialName(), "Signature verification OK");
//                return true;
//            } else {
//                Log.e(getOfficialName(), "Signature verification failed");
//            }
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//    public static Manufacturer FindManufacturer(String ID, byte[] Challenge, byte[] Salt, byte[] Signature) {
//        Manufacturer[] manufacturers = Manufacturer.values();
//        for (int i = 1; i < manufacturers.length; i++) {
//            if ( manufacturers[i].ID.equals(ID) && manufacturers[i].VerifySignature(Challenge, Salt, Signature)) {
//                return manufacturers[i];
//            }
//        }
//        return Manufacturer.Unknown;
//    }

    public static Manufacturer FindManufacturer(String ID) {
        Manufacturer[] manufacturers = Manufacturer.values();
        for (int i = 1; i < manufacturers.length; i++) {
            if (manufacturers[i].ID.equals(ID)) {
                return manufacturers[i];
            }
        }
        return Manufacturer.Unknown;
    }
}
