package com.tangem.commands.common

import com.tangem.commands.WriteIssuerExtraDataCommand

/**
 * This enum specifies modes for [WriteIssuerExtraDataCommand].
 */
enum class IssuerDataMode(val code: Byte) {


    /**
     * This mode is required to read issuer data from the card.
     */
    ReadData(0),

    /**
     * This mode is required to write issuer data to the card.
     */
    WriteData(0),
    /**
     * This mode is required to read issuer extra data from the card.
     */
    ReadExtraData(1),
    /**
     * This mode is required to initiate writing issuer extra data to the card.
     */
    InitializeWritingExtraData(1),
    /**
     * With this mode, the command writes part of issuer extra data
     * (block of a size [WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE]) to the card.
     */
    WriteExtraData(2),
    /**
     * This mode is used after the issuer extra data was fully written to the card.
     * Under this mode the command provides the issuer signature
     * to confirm the validity of data that was written to card.
     */
    FinalizeExtraData(3);

    companion object {
        private val values = values()
        fun byCode(code: Byte): IssuerDataMode? = values.find { it.code == code }
    }
}