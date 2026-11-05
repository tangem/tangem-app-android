package com.tangem.features.earn.impl.fake

import arrow.core.getOrElse
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.EarnAmount
import com.tangem.features.earn.product.api.session.EarnFeeSelection
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.features.earn.product.api.session.WithdrawMode
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FakeEarnSessionFactoriesTest {

    private val params = EarnProduct.Params(
        userWalletId = UserWalletId("0123456789abcdef"),
        currency = mockk<CryptoCurrency>(),
    )
    private val feeSelection = EarnFeeSelection(
        fee = Fee.Common(Amount(currencySymbol = "ETH", value = BigDecimal("0.00033"), decimals = 18)),
        extended = null,
    )
    private val amount = EarnAmount.Exact(BigDecimal("0.5"))

    @Test
    fun `GIVEN fake factories WHEN keys are read THEN only lending and vault are present`() {
        // Act
        val actual = fakeEarnSessionFactories.keys

        // Assert
        assertThat(actual).containsExactly("lending", "vault")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN factory key WHEN created session prepares withdraw THEN withdraw mode matches the product`(
        model: WithdrawModeModel,
    ) = runTest {
        // Arrange
        val session = fakeEarnSessionFactories.getValue(model.key).create(params)

        // Act
        val plan = session.prepare(Withdraw(), amount, feeSelection)
            .getOrElse { error("Expected a plan but prepare failed with $it") }

        // Assert
        assertThat(plan.withdrawMode).isInstanceOf(model.expectedMode)
    }

    private fun provideTestModels() = listOf(
        WithdrawModeModel(key = "lending", expectedMode = WithdrawMode.Instant::class.java),
        WithdrawModeModel(key = "vault", expectedMode = WithdrawMode.Queued::class.java),
    )

    internal data class WithdrawModeModel(val key: String, val expectedMode: Class<out WithdrawMode>) {
        override fun toString(): String = key
    }
}