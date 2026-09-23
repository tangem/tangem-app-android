package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawHeaderFactoryTest {

    private val onCloseClick: () -> Unit = {}

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN draft and step WHEN header created THEN title and counter shown`(model: HeaderModel) {
        // Act
        val actual = BankWithdrawHeaderFactory.create(
            draft = model.draft,
            step = model.step,
            onCloseClick = onCloseClick,
        )

        // Assert
        assertThat(actual).isEqualTo(
            BankWithdrawHeaderUM(title = model.title, subtitle = model.subtitle, onCloseClick = onCloseClick),
        )
    }

    @ParameterizedTest
    @MethodSource("provideSteps")
    fun `GIVEN any wizard step WHEN header created THEN close action is the one passed`(
        step: TangemPayBankWithdrawInnerRoute,
    ) {
        // Arrange
        val draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal)

        // Act
        val actual = BankWithdrawHeaderFactory.create(draft = draft, step = step, onCloseClick = onCloseClick)

        // Assert
        assertThat(actual.onCloseClick).isSameInstanceAs(onCloseClick)
    }

    data class HeaderModel(
        val draft: BankWithdrawDraft,
        val step: TangemPayBankWithdrawInnerRoute,
        val title: TextReference,
        val subtitle: TextReference?,
    )

    private fun provideSteps() = listOf(
        TangemPayBankWithdrawInnerRoute.Amount,
        TangemPayBankWithdrawInnerRoute.TransferType,
        TangemPayBankWithdrawInnerRoute.AccountType,
        TangemPayBankWithdrawInnerRoute.RecipientDetails,
        TangemPayBankWithdrawInnerRoute.BankDetails,
        TangemPayBankWithdrawInnerRoute.Preview,
        TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount),
    )

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
            step = TangemPayBankWithdrawInnerRoute.Amount,
            title = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
            subtitle = counter(current = 1, total = 5),
        ),
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewBusiness),
            step = TangemPayBankWithdrawInnerRoute.BankDetails,
            title = resourceReference(R.string.tangempay_withdraw_hub_to_business_account),
            subtitle = counter(current = 4, total = 4),
        ),
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
            step = TangemPayBankWithdrawInnerRoute.AccountType,
            title = resourceReference(R.string.tangempay_withdraw_bank_account_type_screen_title),
            subtitle = counter(current = 3, total = 5),
        ),
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewBusiness),
            step = TangemPayBankWithdrawInnerRoute.AccountType,
            title = resourceReference(R.string.tangempay_withdraw_bank_account_type_screen_title),
            subtitle = counter(current = 2, total = 4),
        ),
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
            step = TangemPayBankWithdrawInnerRoute.Preview,
            title = resourceReference(R.string.tangempay_withdraw_bank_confirm_title),
            subtitle = null,
        ),
        // GIVEN saved individual recipient WHEN header created THEN title is the full display name
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-1"), recipient = melnikov),
            step = TangemPayBankWithdrawInnerRoute.Amount,
            title = stringReference("Andrei Melnikov"),
            subtitle = null,
        ),
        // GIVEN saved business recipient WHEN header created THEN title is the business name
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-2"), recipient = arasaka),
            step = TangemPayBankWithdrawInnerRoute.Amount,
            title = stringReference("Arasaka"),
            subtitle = null,
        ),
        // GIVEN saved entry without a loaded recipient WHEN header created THEN title is bank transfer
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-1")),
            step = TangemPayBankWithdrawInnerRoute.Amount,
            title = resourceReference(R.string.tangempay_withdraw_hub_bank_section_title),
            subtitle = null,
        ),
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.SeeAll),
            step = TangemPayBankWithdrawInnerRoute.Recipients,
            title = resourceReference(R.string.tangempay_withdraw_hub_bank_section_title),
            subtitle = null,
        ),
        // GIVEN new personal entry with a picked recipient WHEN header created THEN title is the display name
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal, recipient = melnikov),
            step = TangemPayBankWithdrawInnerRoute.Amount,
            title = stringReference("Andrei Melnikov"),
            subtitle = null,
        ),
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewBusiness, recipient = arasaka),
            step = TangemPayBankWithdrawInnerRoute.Amount,
            title = stringReference("Arasaka"),
            subtitle = null,
        ),
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
            step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount),
            title = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
            subtitle = counter(current = 1, total = 5),
        ),
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
            step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.BankDetails),
            title = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
            subtitle = counter(current = 5, total = 5),
        ),
        HeaderModel(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-1"), recipient = primaryChecking),
            step = TangemPayBankWithdrawInnerRoute.Amount,
            title = stringReference("Primary checking"),
            subtitle = null,
        ),
    )

    private companion object {

        val melnikov = bankAccount(
            id = "acc-1",
            beneficiary = Beneficiary.Individual(firstName = "Andrei", lastName = "Melnikov"),
        )

        val arasaka = bankAccount(id = "acc-2", beneficiary = Beneficiary.Business(name = "Arasaka"))

        /** The backend's own display name wins over the name assembled from the beneficiary. */
        val primaryChecking = melnikov.copy(displayName = "Primary checking")

        fun counter(current: Int, total: Int): TextReference =
            resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(current, total))
    }
}