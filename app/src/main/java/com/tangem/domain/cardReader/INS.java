package com.tangem.domain.cardReader;

/**
 * Created by dvol on 07.03.2018.
 */
public enum INS {
    Unknown(0x00),
    BootROM_SOS(0x40),
    BootROM_Tangem(0xF0),
    Personalize(0xF1),
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
    OpenSession(0xFF),
    ReadBlockedData(0xE4),
    CreateTestWallet(0xE0),
    ExtractWalletKey(0xE1),
    Test(0xE2),
    Depersonalize(0xE3);

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
