package com.tangem.features.tangempay.withdraw.bank.form.model

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.tangempay.details.impl.R

/** Every input the recipient and bank details steps can ask for; each step lists its own ordered subset. */
internal enum class BankWithdrawFormField(
    val label: TextReference,
    val placeholder: TextReference,
    val keyboardType: KeyboardType,
    val capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    AccountNumber(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_account_number),
        placeholder = stringReference(NUMBER_PLACEHOLDER),
        keyboardType = KeyboardType.Number,
    ),
    RoutingNumber(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_routing_number),
        placeholder = stringReference(NUMBER_PLACEHOLDER),
        keyboardType = KeyboardType.Number,
    ),
    FirstName(
        label = resourceReference(R.string.tangempay_order_data_first_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_first_name),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Words,
    ),
    LastName(
        label = resourceReference(R.string.tangempay_order_data_last_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_last_name),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Words,
    ),
    BusinessName(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_business_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_business_name),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Words,
    ),
    Country(
        label = resourceReference(R.string.tangempay_order_data_country),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_country),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Characters,
    ),
    Region(
        label = resourceReference(R.string.tangempay_order_data_region),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_region),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Characters,
    ),
    City(
        label = resourceReference(R.string.tangempay_order_data_city),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_city),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Words,
    ),
    AddressLine1(
        label = resourceReference(R.string.tangempay_order_data_address_line1),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_address_line1),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Sentences,
    ),
    PostalCode(
        label = resourceReference(R.string.tangempay_order_data_postal_code),
        placeholder = stringReference(POSTAL_CODE_PLACEHOLDER),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Characters,
    ),
    BankName(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_bank_name),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Words,
    ),
    BankCountry(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_country),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_country),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Characters,
    ),
    BankRegion(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_region),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_region),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Characters,
    ),
    BankCity(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_city),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_city),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Words,
    ),
    BankAddressLine1(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_address_line1),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_address_line1),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Sentences,
    ),
    BankPostalCode(
        label = resourceReference(R.string.tangempay_order_data_postal_code),
        placeholder = stringReference(POSTAL_CODE_PLACEHOLDER),
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Characters,
    ),
}

private const val NUMBER_PLACEHOLDER = "000000000000"
private const val POSTAL_CODE_PLACEHOLDER = "0000"