package com.tangem.tangem_sdk_new

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.tangem.CardReader
import com.tangem.data.ResponseApdu

class NfcReader() : CardReader {

    var isoDep: IsoDep? = null
    var readingActive = false

    var data: ByteArray? = null
    var callback: ((responseApdu: ResponseApdu) -> Unit)? = null

    override fun sendApdu(apduSerialized: ByteArray, callback: (responseApdu: ResponseApdu) -> Unit) {
        data = apduSerialized
        this.callback = callback
    }

    fun onTagDiscovered(tag: Tag?) {
        if (data != null) {
            readingActive = true
            connect()
            val rawResponse = isoDep?.transceive(data)
            if (rawResponse != null) {
                readingActive = false
                data = null
            }
            rawResponse?.let { callback?.invoke(ResponseApdu(it)) }
        }

    }

    private fun connect() {
        val timeout = isoDep?.timeout
        isoDep?.connect()
        isoDep?.close()
        isoDep?.connect()
        isoDep?.timeout = timeout ?: 0
    }

}