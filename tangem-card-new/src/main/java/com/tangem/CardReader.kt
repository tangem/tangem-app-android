package com.tangem

import com.tangem.common.CompletionResult
import com.tangem.common.apdu.ResponseApdu

interface CardReader {
    var readingActive: Boolean
    fun sendApdu(apduSerialized: ByteArray, callback: (response: CompletionResult<ResponseApdu>) -> Unit)
    fun setStartSession()
}