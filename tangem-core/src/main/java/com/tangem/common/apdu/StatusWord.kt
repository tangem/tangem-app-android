package com.tangem.common.apdu

import com.tangem.SessionError

/**
 * Part of a response from the card, shows the status of the operation
 */
enum class StatusWord(val code: Int, val description: String) {

    ProcessCompleted(0x9000, "SW_PROCESS_COMPLETED"),
    InvalidParams(0x6A86, "SW_INVALID_PARAMS"),
    ErrorProcessingCommand(0x6286, "SW_ERROR_PROCESSING_COMMAND"),
    InvalidState(0x6985, "SW_INVALID_STATE"),
    Pin1Changed(ProcessCompleted.code + 0x0001, "SW_PIN1_CHANGED"),
    Pin2Changed(ProcessCompleted.code + 0x0002, "SW_PIN2_CHANGED"),
    PinsChanged(ProcessCompleted.code + 0x0003, "SW_PINS_CHANGED"),
    InsNotSupported(0x6D00, "SW_INS_NOT_SUPPORTED"),
    NeedEncryption(0x6982, "SW_NEED_ENCRYPTION"),
    NeedPause(0x9789, "SW_NEED_PAUSE"),
    Unknown(0x0000, "SW_UNKNOWN");

    companion object {
        private val values = values()
        fun byCode(code: Int): StatusWord = values.find { it.code == code } ?: Unknown
    }
}

fun StatusWord.toSessionError(): SessionError? {
    return when (this) {
        StatusWord.ProcessCompleted, StatusWord.Pin1Changed,
        StatusWord.Pin2Changed, StatusWord.PinsChanged -> null
        StatusWord.NeedPause -> null
        StatusWord.InvalidParams -> SessionError.InvalidParams()
        StatusWord.ErrorProcessingCommand -> SessionError.ErrorProcessingCommand()
        StatusWord.InvalidState -> SessionError.InvalidState()
        StatusWord.InsNotSupported -> SessionError.InsNotSupported()
        StatusWord.NeedEncryption -> SessionError.NeedEncryption()
        StatusWord.Unknown -> SessionError.UnknownStatus()
    }
}
