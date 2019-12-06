package com.tangem

import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu

/**
 * Allows interaction between the phone or any other terminal and Tangem card.
 *
 * Its default implementation, NfcCardReader, is in our tangem-sdk module.
 */
interface CardReader {

    /**
     * Sends data to the card and receives the reply.
     *
     * @param apdu Data to be sent. [CommandApdu] serializes it to a [ByteArray]
     * @param callback Returns response from the card,
     * [ResponseApdu] Allows to convert raw data to [Tlv]
     */
    fun transceiveApdu(apdu: CommandApdu, callback: (response: CompletionResult<ResponseApdu>) -> Unit)

    /**
     * Signals to [CardReader] to become ready to transceive data.
     */
    fun openSession()

    /**
     * Signals to [CardReader] that no further NFC transition is expected.
     */
    fun closeSession()
}