package com.tangem.features.tangempay.withdraw.bank.choice.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.features.tangempay.details.impl.R
import org.junit.jupiter.api.Test

internal class BankWithdrawChoicesTest {

    @Test
    fun `GIVEN own account WHEN transfer type config THEN own option first and selected`() {
        // Act
        val config = BankWithdrawChoices.transferType(isThirdParty = false)

        // Assert
        assertThat(config.sectionTitle).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_transfer_type_title))
        assertThat(config.options.map { it.title }).containsExactly(
            resourceReference(R.string.tangempay_withdraw_bank_transfer_type_own),
            resourceReference(R.string.tangempay_withdraw_bank_transfer_type_third_party),
        ).inOrder()
        assertThat(config.selectedId).isEqualTo(config.options[0].id)
        assertThat(BankWithdrawChoices.isThirdParty(requireNotNull(config.selectedId))).isFalse()
    }

    @Test
    fun `GIVEN third party WHEN transfer type config THEN third party option selected`() {
        // Act
        val config = BankWithdrawChoices.transferType(isThirdParty = true)

        // Assert
        assertThat(config.selectedId).isEqualTo(config.options[1].id)
        assertThat(BankWithdrawChoices.isThirdParty(requireNotNull(config.selectedId))).isTrue()
    }

    @Test
    fun `GIVEN nothing chosen WHEN transfer type config THEN no option selected`() {
        // Act
        val config = BankWithdrawChoices.transferType(isThirdParty = null)

        // Assert
        assertThat(config.selectedId).isNull()
        assertThat(config.options).hasSize(2)
    }

    @Test
    fun `GIVEN savings WHEN account type config THEN checking first and savings selected`() {
        // Act
        val config = BankWithdrawChoices.accountType(selected = BankAccountType.SAVINGS)

        // Assert
        assertThat(config.sectionTitle).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_account_type_title))
        assertThat(config.options.map { it.title }).containsExactly(
            resourceReference(R.string.tangempay_withdraw_bank_account_type_checking),
            resourceReference(R.string.tangempay_withdraw_bank_account_type_saving),
        ).inOrder()
        assertThat(BankWithdrawChoices.accountTypeOf(requireNotNull(config.selectedId)))
            .isEqualTo(BankAccountType.SAVINGS)
        assertThat(BankWithdrawChoices.accountTypeOf(config.options[0].id)).isEqualTo(BankAccountType.CHECKING)
    }

    @Test
    fun `GIVEN nothing chosen WHEN account type config THEN no option selected`() {
        // Act
        val config = BankWithdrawChoices.accountType(selected = null)

        // Assert
        assertThat(config.selectedId).isNull()
        assertThat(config.options.map { it.id })
            .containsExactlyElementsIn(BankAccountType.entries.map { it.name })
            .inOrder()
    }
}