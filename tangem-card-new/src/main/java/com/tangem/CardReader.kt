package com.tangem

import com.tangem.common.apdu.ResponseApdu

interface CardReader {
    fun sendApdu(apduSerialized: ByteArray, callback: (responseApdu: ResponseApdu) -> Unit)
}