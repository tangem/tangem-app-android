package com.tangem.tangem_sdk_new

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.tangem.CardReader
import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.ResponseApdu
import com.tangem.tasks.TaskError

class NfcReader : CardReader {

    var isoDep: IsoDep? = null
        set(value) {
            if (field == null) {
                field = value
                connect()
            }
        }
    override var readingActive = false
    var startSession = false
    var readingCancelled = false
    set(value) {
        if (value) readingActive = false
        field = value
    }

    var data: ByteArray? = null
    var callback: ((response: CompletionResult<ResponseApdu>) -> Unit)? = null

    override fun setStartSession() {
        readingActive = true
        readingCancelled = false
    }

    override fun sendApdu(apduSerialized: ByteArray, callback: (response: CompletionResult<ResponseApdu>) -> Unit) {
        data = apduSerialized
//        readingActive = true
        this.callback = callback
        if (isoDep != null) {
            try {
                transceiveData()
            } catch (exception: Exception) {
                isoDep = null
            }
        }
    }

    private fun transceiveData() {
        if (readingCancelled) {
            callback?.invoke(CompletionResult.Failure(TaskError.UserCancelledError()))
            return
        }
        val rawResponse = isoDep?.transceive(data)
        if (rawResponse != null) {
            Log.i(this::class.simpleName!!, "Nfc response is received")
            data = null
            readingActive = false
        }
        rawResponse?.let { callback?.invoke(CompletionResult.Success(ResponseApdu(it))) }
    }

    fun onTagDiscovered(tag: Tag?) {
//        if (readingActive) {
//            if (startSession) {
//                connect()
//                startSession = false
//            }
        isoDep = IsoDep.get(tag)
        transceiveData()
    }


    private fun connect() {
//        val timeout = isoDep?.timeout
        isoDep?.connect()
        isoDep?.close()
        isoDep?.connect()
        isoDep?.timeout = 120000
        Log.i(this::class.simpleName!!, "Nfc session is started")

    }

}