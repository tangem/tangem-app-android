package com.tangem.features.tangempay.withdraw.bank.form.ui

import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFieldError
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormFields
import com.tangem.features.tangempay.withdraw.bank.form.model.FormState
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM.SuggestionUM
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUMFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal object TangemPayBankWithdrawFormPreviewData {

    private val personalDraft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal)
    private val businessDraft = BankWithdrawDraft(entry = BankWithdrawEntry.NewBusiness)

    private val callbacks = TangemPayBankWithdrawFormUMFactory.Callbacks(
        onBackClick = {},
        onCloseClick = {},
        onValueChange = { _, _ -> },
        onFocusChange = { _, _ -> },
        onSelectorClick = {},
        onSuggestionClick = {},
        onContinueClick = {},
    )

    val personalEmpty: TangemPayBankWithdrawFormUM = create(
        draft = personalDraft,
        step = TangemPayBankWithdrawInnerRoute.RecipientDetails,
        fields = BankWithdrawFormFields.recipientDetails(isBusiness = false),
        state = FormState(edits = emptyMap()),
    )

    val personalWithSuggestions: TangemPayBankWithdrawFormUM = create(
        draft = personalDraft,
        step = TangemPayBankWithdrawInnerRoute.RecipientDetails,
        fields = BankWithdrawFormFields.recipientDetails(isBusiness = false),
        state = FormState(edits = mapOf(BankWithdrawFormField.AccountNumber to "48")),
    ).copy(
        suggestions = persistentListOf(
            SuggestionUM(
                id = "acc-1",
                matchedPrefix = "48",
                accountNumberSuffix = "30019271",
                name = "Basit A.",
                onClick = {},
            ),
            SuggestionUM(
                id = "acc-2",
                matchedPrefix = "48",
                accountNumberSuffix = "11002233",
                name = "Arasaka",
                onClick = {},
            ),
        ),
    )

    val personalWithError: TangemPayBankWithdrawFormUM = create(
        draft = personalDraft,
        step = TangemPayBankWithdrawInnerRoute.RecipientDetails,
        fields = BankWithdrawFormFields.recipientDetails(isBusiness = false),
        state = FormState(
            edits = mapOf(
                BankWithdrawFormField.AccountNumber to "000000000000",
                BankWithdrawFormField.RoutingNumber to "0210",
                BankWithdrawFormField.FirstName to "John",
                BankWithdrawFormField.LastName to "Doe",
                BankWithdrawFormField.Country to "US",
                BankWithdrawFormField.Region to "CA",
                BankWithdrawFormField.City to "Night City",
                BankWithdrawFormField.AddressLine1 to "Ebunike st., Apt. 32",
                BankWithdrawFormField.PostalCode to "90001",
            ),
            errors = mapOf(BankWithdrawFormField.RoutingNumber to BankWithdrawFieldError.InvalidRoutingNumber),
        ),
    )

    val personalFreeTextRegion: TangemPayBankWithdrawFormUM = create(
        draft = personalDraft,
        step = TangemPayBankWithdrawInnerRoute.RecipientDetails,
        fields = BankWithdrawFormFields.recipientDetails(isBusiness = false),
        state = FormState(
            edits = mapOf(
                BankWithdrawFormField.Country to "MC",
                BankWithdrawFormField.Region to "Bavaria",
            ),
        ),
    )

    val personalUsRegion: TangemPayBankWithdrawFormUM = create(
        draft = personalDraft,
        step = TangemPayBankWithdrawInnerRoute.RecipientDetails,
        fields = BankWithdrawFormFields.recipientDetails(isBusiness = false),
        state = FormState(
            edits = mapOf(
                BankWithdrawFormField.Country to "US",
                BankWithdrawFormField.Region to "CA",
            ),
            displayNameEdits = mapOf(
                BankWithdrawFormField.Country to "United States",
                BankWithdrawFormField.Region to "California",
            ),
        ),
    )

    val businessBank: TangemPayBankWithdrawFormUM = create(
        draft = businessDraft,
        step = TangemPayBankWithdrawInnerRoute.BankDetails,
        fields = BankWithdrawFormFields.bankDetails,
        state = FormState(edits = mapOf(BankWithdrawFormField.BankName to "Arasaka Bank")),
    )

    private fun create(
        draft: BankWithdrawDraft,
        step: TangemPayBankWithdrawInnerRoute,
        fields: ImmutableList<BankWithdrawFormField>,
        state: FormState,
    ): TangemPayBankWithdrawFormUM = TangemPayBankWithdrawFormUMFactory(
        fields = fields,
        step = step,
        callbacks = callbacks,
    ).create(draft = draft, state = state, accounts = emptyList())
}