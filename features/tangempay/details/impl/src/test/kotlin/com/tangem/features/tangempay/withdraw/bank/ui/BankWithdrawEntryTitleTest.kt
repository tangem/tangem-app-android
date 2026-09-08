package com.tangem.features.tangempay.withdraw.bank.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawEntryTitleTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN entry WHEN toScreenTitle THEN expected title`(model: TitleModel) {
        // Act
        val actual = model.entry.toScreenTitle()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class TitleModel(val entry: BankWithdrawEntry, val expected: TextReference)

    private fun provideTestModels() = listOf(
        TitleModel(
            entry = BankWithdrawEntry.NewPersonal,
            expected = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
        ),
        TitleModel(
            entry = BankWithdrawEntry.NewBusiness,
            expected = resourceReference(R.string.tangempay_withdraw_hub_to_business_account),
        ),
        TitleModel(
            entry = BankWithdrawEntry.Saved(bankAccountId = "acc-1"),
            expected = resourceReference(R.string.tangempay_card_details_withdraw),
        ),
        TitleModel(
            entry = BankWithdrawEntry.SeeAll,
            expected = resourceReference(R.string.tangempay_card_details_withdraw),
        ),
    )
}