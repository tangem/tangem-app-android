package com.tangem.features.tangempay.common

import com.tangem.utils.StringsSigns
import java.text.Normalizer

/** Charset rules shared by the card order form and the bank off-ramp form. */
internal object TangemPayFormCharset {

    private const val RIGHT_SINGLE_QUOTE = '’'
    private const val LEFT_SINGLE_QUOTE = '‘'

    private val EMBOSS_ALLOWED = Regex("^[A-Za-z0-9 .,/#'()&\\-]*$")
    private val ADDRESS_ALLOWED = Regex("^[\\p{IsLatin}\\p{M}0-9 .,/#'()&\\-]*$")

    fun isEmbossCharsetValid(value: String): Boolean = EMBOSS_ALLOWED.matches(value.normalizeForValidation())

    fun isAddressCharsetValid(value: String): Boolean = ADDRESS_ALLOWED.matches(value.normalizeForValidation())

    private fun String.normalizeForValidation(): String = Normalizer
        .normalize(this, Normalizer.Form.NFC)
        .replace(RIGHT_SINGLE_QUOTE, '\'')
        .replace(LEFT_SINGLE_QUOTE, '\'')
        .replace(StringsSigns.NON_BREAKING_SPACE, ' ')
}