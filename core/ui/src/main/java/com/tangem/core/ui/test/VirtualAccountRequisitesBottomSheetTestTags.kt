package com.tangem.core.ui.test

object VirtualAccountRequisitesBottomSheetTestTags {

    const val SHARE_BUTTON = "VIRTUAL_ACCOUNT_REQUISITES_SHARE_BUTTON"
    const val DEPOSIT_LIMIT_TITLE = "VIRTUAL_ACCOUNT_REQUISITES_DEPOSIT_LIMIT_TITLE"
    const val DEPOSIT_LIMIT_SUBTITLE = "VIRTUAL_ACCOUNT_REQUISITES_DEPOSIT_LIMIT_SUBTITLE"

    private val NON_TAG_CHARS = Regex("[^A-Z0-9]+")

    /** [field] is the row's non-localized `titleForShare`, e.g. `Account number`. */
    fun value(field: String): String = "VIRTUAL_ACCOUNT_REQUISITES_VALUE_${field.normalize()}"

    /** [field] is the row's non-localized `titleForShare`, e.g. `Account number`. */
    fun copyButton(field: String): String = "VIRTUAL_ACCOUNT_REQUISITES_COPY_${field.normalize()}"

    private fun String.normalize(): String = uppercase().replace(NON_TAG_CHARS, "_")
}