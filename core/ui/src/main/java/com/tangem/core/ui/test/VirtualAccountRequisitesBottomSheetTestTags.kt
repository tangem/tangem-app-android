package com.tangem.core.ui.test

object VirtualAccountRequisitesBottomSheetTestTags {

    private val NON_TAG_CHARS = Regex("[^A-Z0-9]+")

    /** [field] is the row's non-localized `titleForShare`, e.g. `Account number`. */
    fun value(field: String): String = "VIRTUAL_ACCOUNT_REQUISITES_VALUE_${field.normalize()}"

    private fun String.normalize(): String = uppercase().replace(NON_TAG_CHARS, "_")
}