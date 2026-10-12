package com.tangem.features.tangempay.withdraw.bank.form.model

import androidx.compose.ui.autofill.ContentType
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
    // A null hint is deliberate: Compose has no organisation-name type, and neither the account and routing
    // numbers nor the fields that only ever open a picker are data a password manager fills.
    val contentType: ContentType? = null,
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
        contentType = ContentType.PersonFirstName,
    ),
    LastName(
        label = resourceReference(R.string.tangempay_order_data_last_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_last_name),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
        contentType = ContentType.PersonLastName,
    ),
    BusinessName(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_business_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_business_name),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
    ),
    Country(
        label = resourceReference(R.string.tangempay_order_data_country),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_not_selected),
        kind = BankWithdrawFieldKind.SelectorField,
    ),
    Region(
        label = resourceReference(R.string.tangempay_order_data_region),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_not_selected),
        kind = BankWithdrawFieldKind.SelectorField,
        contentType = ContentType.AddressRegion,
    ),
    City(
        label = resourceReference(R.string.tangempay_order_data_city),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_city),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
        contentType = ContentType.AddressLocality,
    ),
    AddressLine1(
        label = resourceReference(R.string.tangempay_order_data_address_line1),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_address_line1),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Sentences),
        contentType = ContentType.AddressStreet,
    ),
    PostalCode(
        label = resourceReference(R.string.tangempay_order_data_postal_code),
        placeholder = stringReference(POSTAL_CODE_PLACEHOLDER),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Characters),
        contentType = ContentType.PostalCode,
    ),
    BankName(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_name),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_bank_name),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
    ),
    BankRegion(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_region),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_not_selected),
        kind = BankWithdrawFieldKind.SelectorField,
    ),
    BankCity(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_city),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_city),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words),
        contentType = ContentType.AddressLocality,
    ),
    BankAddressLine1(
        label = resourceReference(R.string.tangempay_withdraw_bank_field_bank_address_line1),
        placeholder = resourceReference(R.string.tangempay_withdraw_bank_placeholder_address_line1),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Sentences),
        contentType = ContentType.AddressStreet,
    ),
    BankPostalCode(
        label = resourceReference(R.string.tangempay_order_data_postal_code),
        placeholder = stringReference(POSTAL_CODE_PLACEHOLDER),
        kind = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Characters),
        contentType = ContentType.PostalCode,
    ),
}

/** A US address picks its state from the official list; anywhere else the region is typed in by hand. */
internal fun BankWithdrawFormField.kindIn(values: Map<BankWithdrawFormField, String>): BankWithdrawFieldKind =
    if (isFreeTextRegion(values)) FREE_TEXT_REGION_KIND else kind

internal fun BankWithdrawFormField.placeholderIn(values: Map<BankWithdrawFormField, String>): TextReference =
    if (isFreeTextRegion(values)) {
        resourceReference(R.string.tangempay_withdraw_bank_placeholder_region_name)
    } else {
        placeholder
    }

private fun BankWithdrawFormField.isFreeTextRegion(values: Map<BankWithdrawFormField, String>): Boolean =
    this == BankWithdrawFormField.Region && !BankWithdrawFormFields.isUsRecipientCountry(values)

private val FREE_TEXT_REGION_KIND = BankWithdrawFieldKind.TextField(KeyboardType.Text, KeyboardCapitalization.Words)

private const val NUMBER_PLACEHOLDER = "000000000000"
private const val POSTAL_CODE_PLACEHOLDER = "0000"