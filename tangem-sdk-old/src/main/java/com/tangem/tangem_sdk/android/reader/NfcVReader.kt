package com.tangem.tangem_sdk.android.reader

import android.nfc.tech.NfcV

data class NfcVReader(
        var nfcV: NfcV?
) : com.tangem.tangem_card.reader.NfcReader {

    override fun getId(): ByteArray? {
        return nfcV?.tag?.id
    }

    override fun setTimeout(timeout: Int) {
    }

    override fun getTimeout(): Int {
        return 0
    }

    override fun transceive(data: ByteArray?): ByteArray? {
        return nfcV?.transceive(data)
    }

    override fun ignoreTag() {
        nfcV?.close()
        nfcV = null
    }

    override fun notifyReadResult(success: Boolean) {
    }

    override fun connect() {
        nfcV?.connect()
    }

    override fun isConnected(): Boolean {
        return nfcV?.isConnected == true
    }
}