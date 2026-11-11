package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawSpeiAvailabilityTest {

    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk()
    private val availability = BankWithdrawSpeiAvailability(moneyAccountFeatureToggles)

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountFeatureToggles)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN source account and MXN toggle WHEN isOffered THEN only the Money account with MXN on offers SPEI`(
        model: AvailabilityModel,
    ) {
        // Arrange
        every { moneyAccountFeatureToggles.isMxnEnabled } returns model.isMxnEnabled

        // Act
        val actual = availability.isOffered(model.account)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        AvailabilityModel(account = MONEY_ACCOUNT, isMxnEnabled = true, expected = true),
        AvailabilityModel(account = MONEY_ACCOUNT, isMxnEnabled = false, expected = false),
        AvailabilityModel(account = PAYMENT_ACCOUNT, isMxnEnabled = true, expected = false),
        AvailabilityModel(account = PAYMENT_ACCOUNT, isMxnEnabled = false, expected = false),
    )

    data class AvailabilityModel(
        val account: BankWithdrawSourceAccount,
        val isMxnEnabled: Boolean,
        val expected: Boolean,
    )

    private companion object {
        val MONEY_ACCOUNT = BankWithdrawSourceAccount.Money(UserWalletId("011"), productInstanceId = "instance-1")
        val PAYMENT_ACCOUNT = BankWithdrawSourceAccount.Payment(UserWalletId("011"))
    }
}