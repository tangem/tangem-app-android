package com.tangem.common.apdu

enum class Instruction(var code: Int) {
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


    companion object {
        private val values = values()
        fun byCode(code: Int): Instruction = values.find { it.code == code } ?: Unknown
    }
}