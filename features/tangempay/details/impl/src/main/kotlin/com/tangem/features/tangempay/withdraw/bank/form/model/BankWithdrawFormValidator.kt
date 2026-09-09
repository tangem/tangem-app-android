package com.tangem.features.tangempay.withdraw.bank.form.model

/**
 * Format rules from the analyst spec, applied to trimmed values; US addresses get the stricter state and ZIP checks.
 */
internal object BankWithdrawFormValidator {

    private const val US_COUNTRY_CODE = "US"
    private const val ROUTING_NUMBER_LENGTH = 9
    private const val NAME_MAX_LENGTH = 255
    private const val ADDRESS_MAX_LENGTH = 100
    private const val POSTAL_CODE_MAX_LENGTH = 20
    private val ACCOUNT_NUMBER_LENGTH = 4..17
    private val TWO_LETTER_CODE = Regex("^[A-Za-z]{2}$")
    private val US_ZIP = Regex("^\\d{5}(-?\\d{4})?$")

    fun validate(field: BankWithdrawFormField, values: Map<BankWithdrawFormField, String>): BankWithdrawFieldError? {
        val value = values[field].orEmpty().trim()
        if (value.isEmpty()) return BankWithdrawFieldError.Required
        val isUsAddress = countryFieldOf(field)
            ?.let { countryField -> values[countryField] }
            ?.trim()
            ?.equals(US_COUNTRY_CODE, ignoreCase = true) == true
        return when {
            field == BankWithdrawFormField.RoutingNumber -> {
                if (isRoutingNumber(value)) null else BankWithdrawFieldError.InvalidRoutingNumber
            }
            hasValidFormat(field = field, value = value, isUsAddress = isUsAddress) -> null
            else -> BankWithdrawFieldError.InvalidFormat
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
        BankWithdrawFormField.BankCountry ->
            listOf(BankWithdrawFormField.BankRegion, BankWithdrawFormField.BankPostalCode)
        else -> emptyList()
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
            BankWithdrawFormField.Country,
            BankWithdrawFormField.BankCountry,
            -> TWO_LETTER_CODE.matches(value)
            BankWithdrawFormField.Region,
            BankWithdrawFormField.BankRegion,
            -> if (isUsAddress) TWO_LETTER_CODE.matches(value) else value.length <= ADDRESS_MAX_LENGTH
            BankWithdrawFormField.City,
            BankWithdrawFormField.AddressLine1,
            BankWithdrawFormField.BankCity,
            BankWithdrawFormField.BankAddressLine1,
            -> value.length <= ADDRESS_MAX_LENGTH
            BankWithdrawFormField.PostalCode,
            BankWithdrawFormField.BankPostalCode,
            -> if (isUsAddress) US_ZIP.matches(value) else value.length <= POSTAL_CODE_MAX_LENGTH
        }
    }

    private fun isRoutingNumber(value: String): Boolean =
        value.length == ROUTING_NUMBER_LENGTH && value.all(Char::isDigit)

    /** Address fields whose rules depend on the country entered in the same section of the form. */
    private fun countryFieldOf(field: BankWithdrawFormField): BankWithdrawFormField? = when (field) {
        BankWithdrawFormField.Region,
        BankWithdrawFormField.PostalCode,
        -> BankWithdrawFormField.Country
        BankWithdrawFormField.BankRegion,
        BankWithdrawFormField.BankPostalCode,
        -> BankWithdrawFormField.BankCountry
        else -> null
    }
}