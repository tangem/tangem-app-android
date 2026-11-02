package com.tangem.features.onramp.hottokens.portfolio.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddToPortfolioComponent
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampAddToPortfolioModelTest : OnrampModelTestBase() {
    @Test
    fun `GIVEN add failure WHEN retried successfully THEN progress resets and success is reported once`() = runTest {
        // Arrange
        val params = mockk<OnrampAddToPortfolioComponent.Params>(relaxed = true) {
            every { userWalletId } returns this@OnrampAddToPortfolioModelTest.userWalletId
            every { cryptoCurrency } returns this@OnrampAddToPortfolioModelTest.cryptoCurrency
        }
        val wallet = mockk<GetUserWalletUseCase> {
            every { this@mockk.invoke(userWalletId) } returns
                mockk<com.tangem.domain.models.wallet.UserWallet>(relaxed = true).right()
        }
        val manage = mockk<ManageCryptoCurrenciesUseCase>()
        coEvery { manage(accountId = any(), add = cryptoCurrency) } returnsMany
            listOf(IllegalStateException("offline").left(), Unit.right())
        val model = OnrampAddToPortfolioModel(
            MutableParamsContainer(params), testDispatchers(), manage, wallet,
        )

        // Act
        model.state.value.addButtonUM.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.addButtonUM.isProgress).isFalse()
        verify(exactly = 0) { params.onSuccessAdding(any()) }

        // Act
        model.state.value.addButtonUM.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { manage(AccountId.forMainCryptoPortfolio(userWalletId), add = cryptoCurrency) }
        verify(exactly = 1) { params.onSuccessAdding(cryptoCurrency.id) }
        model.onDestroy()
    }
}