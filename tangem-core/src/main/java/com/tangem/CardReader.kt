package com.tangem

import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu

interface CardReader {
    var readingActive: Boolean
    fun transceiveApdu(apdu: CommandApdu, callback: (response: CompletionResult<ResponseApdu>) -> Unit)
    fun startNfcSession()
    fun closeSession()
}