package com.tangem

import com.tangem.data.ResponseApdu

interface CardReader {
    fun sendApdu(apduSerialized: ByteArray, callback: (responseApdu: ResponseApdu) -> Unit)
}