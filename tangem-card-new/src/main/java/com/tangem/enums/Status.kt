package com.tangem.enums

enum class Status (val code: Int, val description: String){

    ProcessCompleted(0x9000, "SW_PROCESS_COMPLETED"),
    InvalidParams(0x6A86, "SW_INVALID_PARAMS"),
    ErrorProcessingCommand(0x6286, "SW_ERROR_PROCESSING_COMMAND"),
    InvalidState(0x6985, "SW_INVALID_STATE"),
    //    PinsNotChanged(ProcessCompleted.code, ProcessCompleted.description),
    Pin1Changed(ProcessCompleted.code + 0x0001, "SW_PIN1_CHANGED"),
    Pin2Changed(ProcessCompleted.code + 0x0002, "SW_PIN2_CHANGED"),
    PinsChanged(ProcessCompleted.code + 0x0003, "SW_PINS_CHANGED"),
    InsNotSupported(0x6D00, "SW_INS_NOT_SUPPORTED"),
    NeedEncryption(0x6982, "SW_NEED_ENCRYPTION"),
    NeedPause(0x9789, "SW_NEED_PAUSE");

    companion object {
        fun byCode(code: Int): Status = values().find { it.code == code } ?: InvalidParams
    }

}
