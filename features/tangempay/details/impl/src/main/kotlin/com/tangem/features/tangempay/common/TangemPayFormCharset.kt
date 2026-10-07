package com.tangem.features.tangempay.common

import com.tangem.utils.StringsSigns
import java.text.Normalizer

/** Charset rules shared by the card order form and the bank off-ramp form. */
internal object TangemPayFormCharset {

    private const val RIGHT_SINGLE_QUOTE = '’'
    private const val LEFT_SINGLE_QUOTE = '‘'

    private val EMBOSS_ALLOWED = Regex("^[A-Za-z0-9 .\\-]*$")
    private val RECIPIENT_NAME_ALLOWED = Regex("^[A-Za-z \\-]*$")
    private val SHIPPING_ALLOWED = Regex("^[A-Za-z0-9 .,#'/&()°\\-]*$")
    private val POSTAL_CODE_ALLOWED = Regex("^[A-Za-z0-9 \\-]*$")
    private val ADDRESS_ALLOWED = Regex("^[\\p{IsLatin}\\p{M}0-9 .,/#'()&\\-]*$")
    private val NON_LATIN_LETTER = Regex("[\\p{L}\\p{M}&&[^A-Za-z]]")

    fun isEmbossCharsetValid(value: String): Boolean = EMBOSS_ALLOWED.matches(normalize(value))

    fun isRecipientNameCharsetValid(value: String): Boolean = RECIPIENT_NAME_ALLOWED.matches(normalize(value))

    fun isShippingCharsetValid(value: String): Boolean = SHIPPING_ALLOWED.matches(normalize(value))

    fun isPostalCodeCharsetValid(value: String): Boolean = POSTAL_CODE_ALLOWED.matches(normalize(value))

    fun isAddressCharsetValid(value: String): Boolean = ADDRESS_ALLOWED.matches(normalize(value))

    fun containsNonLatinLetter(value: String): Boolean = NON_LATIN_LETTER.containsMatchIn(normalize(value))

    fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFC)
        .replace(RIGHT_SINGLE_QUOTE, '\'')
        .replace(LEFT_SINGLE_QUOTE, '\'')
        .replace(StringsSigns.NON_BREAKING_SPACE, ' ')
}