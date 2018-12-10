package com.tangem.tangemcard.android.reader

import android.nfc.tech.IsoDep

data class NfcReader(
        val nfcManager: NfcManager,
        val isoDep: IsoDep
) : com.tangem.tangemcard.reader.NfcReader {
    override fun getId(): ByteArray {
        return isoDep.tag.id
    }

    override fun setTimeout(timeout: Int) {
        isoDep.timeout = timeout
    }

    override fun getTimeout(): Int {
        return isoDep.timeout
    }

    override fun transceive(data: ByteArray?): ByteArray {
        return isoDep.transceive(data)
    }

    override fun ignoreTag() {
        nfcManager.ignoreTag(isoDep.tag)
    }

    override fun notifyReadResult(success: Boolean) {
        nfcManager.notifyReadResult(success)
    }

    override fun connect() {
        val timeout = isoDep.timeout
        isoDep.connect()
        isoDep.close()
        isoDep.connect()
        isoDep.timeout = timeout
    }

}