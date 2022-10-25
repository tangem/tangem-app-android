package com.tangem.tap.domain.moduleMessage

/**
[REDACTED_AUTHOR]
 * Base interface for conversion outputs from ModuleMessageConverter
 */
interface ConvertedMessage {
    val message: String
}

/**
 * Dialog message used to construct a android.Dialog
 */
interface DialogMessage : ConvertedMessage {
    val title: String
    override val message: String
    val onPositive: String?
    val onNegative: String?
    val onNeutral: String?
}

data class ConvertedStringMessage(
    override val message: String,
) : ConvertedMessage

data class ConvertedDialogMessage(
    override val title: String,
    override val message: String,
    override val onPositive: String? = null,
    override val onNegative: String? = null,
    override val onNeutral: String? = null,
) : DialogMessage