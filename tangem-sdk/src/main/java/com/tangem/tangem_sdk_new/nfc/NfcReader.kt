package com.tangem.tangem_sdk_new.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.tangem.CardReader
import com.tangem.Log
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.tasks.TaskError

class NfcReader : CardReader {

    override var readingActive = false
    var nfcEnabled = false
    var isoDep: IsoDep? = null
        set(value) {
            // if tag is received, call connect first before transceiving data
            if (field == null) {
                field = value
                connect()
            }
            // don't reassign when there's an active tag already
            if (value == null) field = value
        }

    var readingCancelled = false
        set(value) {
            field = value
            if (value) {
                // Stops reading and sends failure callback to a task
                // if reading is cancelled (when user closes nfc bottom sheet dialog).
                readingActive = false
                callback?.invoke(CompletionResult.Failure(TaskError.UserCancelledError()))
            }
        }

    var data: ByteArray? = null
    var callback: ((response: CompletionResult<ResponseApdu>) -> Unit)? = null

    override fun setStartSession() {
        readingActive = true
        readingCancelled = false
    }

    override fun closeSession() {
        isoDep = null
        readingActive = false
    }

    override fun transceiveApdu(apdu: CommandApdu, callback: (response: CompletionResult<ResponseApdu>) -> Unit) {
        data = apdu.apduData
        this.callback = callback
        if (isoDep != null) {
            transceiveData()
        }
    }

    private fun transceiveData() {
        if (readingCancelled) {
            callback?.invoke(CompletionResult.Failure(TaskError.UserCancelledError()))
            return
        }
        if (data == null) return

        val rawResponse: ByteArray?
        try {
            rawResponse = isoDep?.transceive(data)
        } catch (exception: Exception) {
            isoDep = null
            return
        }
        if (rawResponse != null) {
            Log.i(this::class.simpleName!!, "Nfc response is received")
            data = null
        }
        rawResponse?.let { callback?.invoke(CompletionResult.Success(ResponseApdu(it))) }
    }

    fun onTagDiscovered(tag: Tag?) {
        isoDep = IsoDep.get(tag)
        transceiveData()
    }


    private fun connect() {
        isoDep?.connect()
        isoDep?.close()
        isoDep?.connect()
        isoDep?.timeout = 240000
        Log.i(this::class.simpleName!!, "Nfc session is started")

    }

}