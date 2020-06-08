package com.tangem.tangem_sdk_new.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcV
import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


data class NfcTag(val type: TagType, val isoDep: IsoDep?, val nfcV: NfcV? = null)

/**
 * Provides NFC communication between an Android application and Tangem card.
 */
class NfcReader : CardReader {
    override val tag = ConflatedBroadcastChannel<TagType?>()
    override var scope: CoroutineScope? = null

    var listener: ReadingActiveListener? = null

    private var nfcTag: NfcTag? = null
        set(value) {
            field = value
            scope?.launch { tag.send(value?.type) }
        }

    override fun startSession() {
        Log.i(this::class.simpleName!!, "NFC reader is starting NFC session")
        listener?.readingIsActive = true
    }

    fun onTagDiscovered(tag: Tag?) {
        NfcV.get(tag)?.let {
            nfcTag = NfcTag(TagType.Slix, null, NfcV.get(tag))
            return
        }
        IsoDep.get(tag)?.let { isoDep ->
            connect(isoDep)
            nfcTag = NfcTag(TagType.Nfc, isoDep)
        }
    }

    private fun connect(isoDep: IsoDep) {
        isoDep.connect()
        isoDep.close()
        isoDep.connect()
        isoDep.timeout = 240000
        Log.i(this::class.simpleName!!, "NFC tag is connected")
    }

    override fun stopSession(cancelled: Boolean) {
        nfcTag = null
        listener?.readingIsActive = false
        if (cancelled) scope?.cancel()
    }

    override suspend fun transceiveApdu(apdu: CommandApdu): CompletionResult<ResponseApdu> =
        suspendCancellableCoroutine { continuation ->
            transceiveApdu(apdu) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }

    override fun transceiveApdu(apdu: CommandApdu, callback: (response: CompletionResult<ResponseApdu>) -> Unit) {
        val rawResponse: ByteArray? = try {
            transcieveAndLog(apdu.apduData)
        } catch (exception: TagLostException) {
            callback.invoke(CompletionResult.Failure(TangemSdkError.TagLost()))
            nfcTag = null
            return
        } catch (exception: Exception) {
            tryHandleNfcError(exception, callback)
            nfcTag = null
            return
        }

        if (rawResponse != null) {
            Log.i(this::class.simpleName!!, "Data from the card was received")
        }
        rawResponse?.let { callback.invoke(CompletionResult.Success(ResponseApdu(it))) }
    }

    private fun transcieveAndLog(data: ByteArray): ByteArray? {
        Log.i(this::class.simpleName!!, "Sending data to the card, size is ${data.size}")
        Log.v(this::class.simpleName!!, "Raw data that is to be sent to the card: ${data.toHexString()}")
        val rawResponse = nfcTag?.isoDep?.transceive(data)
        Log.v(this::class.simpleName!!, "Raw data that was received from the card: ${rawResponse?.toHexString()}")
        return rawResponse
    }

    private fun tryHandleNfcError(exception: Exception, callback: (response: CompletionResult<ResponseApdu>) -> Unit) {
        Log.i(this::class.simpleName!!, exception.localizedMessage ?: "Error tranceiving data")
        // The messages of errors can vary on different Android devices,
        // but we try to identify it by parsing the message.
        if (exception.message?.contains("length") == true) {
            callback.invoke(CompletionResult.Failure(TangemSdkError.ExtendedLengthNotSupported()))
        }
    }

    override fun readSlixTag(callback: (result: CompletionResult<ResponseApdu>) -> Unit) {
        val nfcV = nfcTag?.nfcV
        if (nfcV == null) {
            callback.invoke(CompletionResult.Failure(TangemSdkError.ErrorProcessingCommand()))
            return
        }
        val response = SlixTagReader().transceive(nfcV)
        when (response) {
            is SlixReadResult.Failure -> {
                Log.e(this::class.simpleName!!, "${response.exception.message}")
                callback.invoke(CompletionResult.Failure(TangemSdkError.ErrorProcessingCommand()))
            }
            is SlixReadResult.Success -> {
                callback.invoke(CompletionResult.Success(ResponseApdu(response.data)))
            }
        }
    }
}