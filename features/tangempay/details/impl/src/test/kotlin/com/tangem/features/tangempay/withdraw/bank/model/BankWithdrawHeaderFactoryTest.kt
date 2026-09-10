package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import org.junit.jupiter.api.Test

internal class BankWithdrawHeaderFactoryTest {

    @Test
    fun `GIVEN new personal entry WHEN create for amount THEN title and counter`() {
        // Arrange
        val draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal)

        // Act
        val actual = BankWithdrawHeaderFactory.create(draft, TangemPayBankWithdrawInnerRoute.Amount)

        // Assert
        assertThat(actual).isEqualTo(
            BankWithdrawHeaderUM(
                title = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
                subtitle = resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(1, 5)),
            ),
        )
    }

    @Test
    fun `GIVEN new business entry WHEN create for bank details THEN counter 4 of 4`() {
        // Arrange
        val draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewBusiness)

        // Act
        val actual = BankWithdrawHeaderFactory.create(draft, TangemPayBankWithdrawInnerRoute.BankDetails)

        // Assert
        assertThat(actual).isEqualTo(
            BankWithdrawHeaderUM(
                title = resourceReference(R.string.tangempay_withdraw_hub_to_business_account),
                subtitle = resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(4, 4)),
            ),
        )
    }

    @Test
    fun `GIVEN saved entry with recipient WHEN create THEN title to short name without counter`() {
        // Arrange
        val recipient = bankAccount(
            id = "acc-1",
            beneficiary = Beneficiary.Individual(firstName = "Andrei", lastName = "Melnikov"),
        )
        val draft = BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-1"), recipient = recipient)

        // Act
        val actual = BankWithdrawHeaderFactory.create(draft, TangemPayBankWithdrawInnerRoute.Amount)

        // Assert
        assertThat(actual).isEqualTo(
            BankWithdrawHeaderUM(
                title = resourceReference(R.string.tangempay_withdraw_bank_title_to_recipient, wrappedList("Andrei M.")),
                subtitle = null,
            ),
        )
    }

    @Test
    fun `GIVEN see all entry without recipient WHEN create THEN generic withdraw title`() {
        // Arrange
        val draft = BankWithdrawDraft(entry = BankWithdrawEntry.SeeAll)

        // Act
        val actual = BankWithdrawHeaderFactory.create(draft, TangemPayBankWithdrawInnerRoute.Recipients)

        // Assert
        assertThat(actual).isEqualTo(
            BankWithdrawHeaderUM(
                title = resourceReference(R.string.tangempay_card_details_withdraw),
                subtitle = null,
            ),
        )
    }

    @Test
    fun `GIVEN new personal entry WHEN create for account type THEN select account type title and counter 3 of 5`() {
        // Arrange
        val draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal)

        // Act
        val actual = BankWithdrawHeaderFactory.create(draft, TangemPayBankWithdrawInnerRoute.AccountType)

        // Assert
        assertThat(actual).isEqualTo(
            BankWithdrawHeaderUM(
                title = resourceReference(R.string.tangempay_withdraw_bank_account_type_screen_title),
                subtitle = resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(3, 5)),
            ),
        )
    }

    @Test
    fun `GIVEN new business entry WHEN create for account type THEN select account type title and counter 2 of 4`() {
        // Arrange
        val draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewBusiness)

        // Act
        val actual = BankWithdrawHeaderFactory.create(draft, TangemPayBankWithdrawInnerRoute.AccountType)

        // Assert
        assertThat(actual).isEqualTo(
            BankWithdrawHeaderUM(
                title = resourceReference(R.string.tangempay_withdraw_bank_account_type_screen_title),
                subtitle = resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(2, 4)),
            ),
        )
    }
}