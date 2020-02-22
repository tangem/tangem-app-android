package com.tangem.common.apdu

/**
 * Instruction code that determines the type of the command that is sent to the Tangem card.
 * It is used in the construction of [com.tangem.common.apdu.CommandApdu].
 */
enum class Instruction(var code: Int) {
    Unknown(0x00),
    Read(0xF2),
    VerifyCard(0xF3),
    ValidateCard(0xF4),
    VerifyCode(0xF5),
    WriteIssuerData(0xF6),
    ReadIssuerData(0xF7),
    CreateWallet(0xF8),
    CheckWallet(0xF9),
    SwapPIN(0xFA),
    Sign(0xFB),
    PurgeWallet(0xFC),
    Activate(0xFE),
    OpenSession(0xFF),
		WriteUserData(0xE0),
		ReadUserData(0xE1);


    companion object {
        private val values = values()
        fun byCode(code: Int): Instruction = values.find { it.code == code } ?: Unknown
    }
}