package com.tangem.features.tangempay.withdraw.bank.form.model

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.tangempay.details.impl.R

internal sealed interface BankWithdrawFieldKind {

    data class TextField(
        val keyboardType: KeyboardType,
        val capitalization: KeyboardCapitalization,
    ) : BankWithdrawFieldKind

    data object SelectorField : BankWithdrawFieldKind
}

internal enum class BankWithdrawFormField(
    val label: TextReference,
    val placeholder: TextReference,
    val kind: BankWithdrawFieldKind,
) {
    AccountNumber(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_account_number),
        placeholder = stringReference(NUMBER_PLACEHOLDER),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Number, KeyboardCapitalization.None),
    ),
    RoutingNumber(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_routing_number),
        placeholder = stringReference(NUMBER_PLACEHOLDER),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Number, KeyboardCapitalization.None),
    ),
    FirstName(
        label = resourceReference(R.string.tangempay_order_data_first_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_first_name),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
    ),
    LastName(
        label = resourceReference(R.string.tangempay_order_data_last_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_last_name),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
    ),
    BusinessName(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_business_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_business_name),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
    ),
    Country(
        label = resourceReference(R.string.tangempay_order_data_country),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_country),
        kind = BankWithdrawFieldKind.SelectorField,
    ),
    Region(
        label = resourceReference(R.string.tangempay_order_data_region),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_region),
        kind = BankWithdrawFieldKind.SelectorField,
    ),
    City(
        label = resourceReference(R.string.tangempay_order_data_city),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_city),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
    ),
    AddressLine1(
        label = resourceReference(R.string.tangempay_order_data_address_line1),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_address_line1),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Sentences),
    ),
    PostalCode(
        label = resourceReference(R.string.tangempay_order_data_postal_code),
        placeholder = stringReference(POSTAL_CODE_PLACEHOLDER),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Characters),
    ),
    BankName(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_bank_name),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
    ),
    BankCountry(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_country),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_country),
        kind = BankWithdrawFieldKind.SelectorField,
    ),
    BankRegion(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_region),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_region),
        kind = BankWithdrawFieldKind.SelectorField,
    ),
    BankCity(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_city),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_city),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
    ),
    BankAddressLine1(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_address_line1),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_address_line1),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Sentences),
    ),
    BankPostalCode(
        label = resourceReference(R.string.tangempay_order_data_postal_code),
        placeholder = stringReference(POSTAL_CODE_PLACEHOLDER),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Characters),
    ),
}

private const val NUMBER_PLACEHOLDER = "000000000000"
private const val POSTAL_CODE_PLACEHOLDER = "0000"