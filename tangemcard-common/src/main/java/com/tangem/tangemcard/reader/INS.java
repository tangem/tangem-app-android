package com.tangem.tangemcard.reader;

/**
 * Created by dvol on 07.03.2018.
 */
public enum INS {
    Unknown(0x00),
    Read(0xF2),
    VerifyCard(0xF3),
    ValidateCard(0xF4),
    VerifyCode(0xF5),
    WriteIssuerData(0xF6),
    GetIssuerData(0xF7),
    CreateWallet(0xF8),
    CheckWallet(0xF9),
    SwapPIN(0xFA),
    Sign(0xFB),
    PurgeWallet(0xFC),
    Activate(0xFE),
    OpenSession(0xFF);

    INS(int Code) {
        this.Code = Code;
    }

    public int Code;

    public static INS ByCode(int Code) {
        INS[] allINS = INS.values();
        for (INS i : allINS) {
            if (i.Code == Code) return i;
        }
        return Unknown;
    }
}
