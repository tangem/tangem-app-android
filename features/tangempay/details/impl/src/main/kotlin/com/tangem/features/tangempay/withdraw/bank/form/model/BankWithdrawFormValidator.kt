package com.tangem.features.tangempay.withdraw.bank.form.model

import com.tangem.features.tangempay.common.TangemPayFormCharset

/**
 * Format rules from the analyst spec, applied to trimmed values; US addresses get the stricter state and ZIP checks.
 */
internal object BankWithdrawFormValidator {

    private const val ROUTING_NUMBER_LENGTH = 9
    private const val NAME_MAX_LENGTH = 255
    private const val ADDRESS_MAX_LENGTH = 100
    private const val POSTAL_CODE_MAX_LENGTH = 20
    private val ACCOUNT_NUMBER_LENGTH = 4..17
    private val TWO_LETTER_CODE = Regex("^[A-Za-z]{2}$")
    private val US_ZIP = Regex("^\\d{5}(-?\\d{4})?$")

    /** Free-text fields Rain accepts in Latin script only; the rest are digits and codes the format rules pin down. */
    private val LATIN_ONLY_FIELDS = setOf(
        BankWithdrawFormField.FirstName,
        BankWithdrawFormField.LastName,
        BankWithdrawFormField.BusinessName,
        BankWithdrawFormField.BankName,
        BankWithdrawFormField.Region,
        BankWithdrawFormField.BankRegion,
        BankWithdrawFormField.City,
        BankWithdrawFormField.AddressLine1,
        BankWithdrawFormField.BankCity,
        BankWithdrawFormField.BankAddressLine1,
    )

    fun validate(field: BankWithdrawFormField, values: Map<BankWithdrawFormField, String>): BankWithdrawFieldError? {
        val value = values[field].orEmpty().trim()
        if (value.isEmpty()) return BankWithdrawFieldError.Required
        val isUsAddress = countryFieldOf(field)
            ?.let { countryField -> values[countryField] }
            ?.trim()
            ?.equals(BankWithdrawFormFields.US_COUNTRY_CODE, ignoreCase = true) == true
        return if (field == BankWithdrawFormField.RoutingNumber) {
            if (isRoutingNumber(value)) null else BankWithdrawFieldError.InvalidRoutingNumber
        } else {
            formatError(field = field, value = value, isUsAddress = isUsAddress)
        }
    }

    fun validateAll(
        fields: List<BankWithdrawFormField>,
        values: Map<BankWithdrawFormField, String>,
    ): Map<BankWithdrawFormField, BankWithdrawFieldError> = fields
        .mapNotNull { field -> validate(field = field, values = values)?.let { error -> field to error } }
        .toMap()

    /** Address fields whose rules change with [field]; editing a country makes their earlier verdicts stale. */
    fun dependentFieldsOf(field: BankWithdrawFormField): List<BankWithdrawFormField> = when (field) {
        BankWithdrawFormField.Country -> listOf(BankWithdrawFormField.Region, BankWithdrawFormField.PostalCode)
        else -> emptyList()
    }

    /** Shape and length are judged first, so an over-long non-Latin value reads as a length problem, as on iOS. */
    private fun formatError(
        field: BankWithdrawFormField,
        value: String,
        isUsAddress: Boolean,
    ): BankWithdrawFieldError? = when {
        !hasValidFormat(field = field, value = value, isUsAddress = isUsAddress) ->
            BankWithdrawFieldError.InvalidFormat
        field in LATIN_ONLY_FIELDS && !TangemPayFormCharset.isAddressCharsetValid(value) ->
            BankWithdrawFieldError.NonLatin
        else -> null
    }

    private fun hasValidFormat(field: BankWithdrawFormField, value: String, isUsAddress: Boolean): Boolean {
        return when (field) {
            BankWithdrawFormField.AccountNumber -> value.length in ACCOUNT_NUMBER_LENGTH && value.all(Char::isDigit)
            BankWithdrawFormField.RoutingNumber -> isRoutingNumber(value)
            BankWithdrawFormField.FirstName,
            BankWithdrawFormField.LastName,
            BankWithdrawFormField.BusinessName,
            BankWithdrawFormField.BankName,
            -> value.length <= NAME_MAX_LENGTH
            BankWithdrawFormField.Country -> TWO_LETTER_CODE.matches(value)
            BankWithdrawFormField.Region ->
                if (isUsAddress) TWO_LETTER_CODE.matches(value) else value.length <= ADDRESS_MAX_LENGTH
            BankWithdrawFormField.City,
            BankWithdrawFormField.AddressLine1,
            BankWithdrawFormField.BankCity,
            BankWithdrawFormField.BankAddressLine1,
            -> value.length <= ADDRESS_MAX_LENGTH
            BankWithdrawFormField.PostalCode ->
                if (isUsAddress) US_ZIP.matches(value) else value.length <= POSTAL_CODE_MAX_LENGTH
            // Bank rails are US-only: the form has no bank country and the order always sends US.
            BankWithdrawFormField.BankRegion -> TWO_LETTER_CODE.matches(value)
            BankWithdrawFormField.BankPostalCode -> US_ZIP.matches(value)
        }
    }

    private fun isRoutingNumber(value: String): Boolean =
        value.length == ROUTING_NUMBER_LENGTH && value.all(Char::isDigit)

    /** Address fields whose rules depend on the country entered in the same section of the form. */
    private fun countryFieldOf(field: BankWithdrawFormField): BankWithdrawFormField? = when (field) {
        BankWithdrawFormField.Region,
        BankWithdrawFormField.PostalCode,
        -> BankWithdrawFormField.Country
        else -> null
    }
}