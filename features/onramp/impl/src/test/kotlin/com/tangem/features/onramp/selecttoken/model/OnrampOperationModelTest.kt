package com.tangem.features.onramp.selecttoken.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.selecttoken.OnrampOperationComponent
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampOperationModelTest : OnrampModelTestBase() {
    @Test
    fun `GIVEN buy operation WHEN token clicked THEN selected wallet and currency are routed to onramp`() = runTest {
        // Arrange
        val wallet = mockk<UserWallet> { every { walletId } returns userWalletId }
        val wallets = mockk<GetWalletsUseCase> { every { invokeSync() } returns listOf(wallet) }
        val status = mockk<CryptoCurrencyStatus> { every { currency } returns cryptoCurrency }
        val model = OnrampOperationModel(
            paramsContainer = MutableParamsContainer(OnrampOperationComponent.Params.Buy(userWalletId)),
            getWalletsUseCase = wallets,
            dispatchers = testDispatchers(),
            router = router,
            analyticsEventHandler = analyticsEventHandler,
            getSelectedAppCurrencyUseCase = mockk(),
            getOfframpUrlUseCase = mockk(),
            urlOpener = mockk(),
            isDemoCardUseCase = mockk(),
            messageSender = mockk(),
            rampStateManager = mockk(),
        )

        // Act
        model.onTokenClick(status)
        model.onTokenListInitialized()
        advanceUntilIdle()

        // Assert
        verify {
            router.push(AppRoute.Onramp(
                source = OnrampSource.ACTION_BUTTONS, userWalletId = userWalletId, currency = cryptoCurrency,
            ))
        }
        assertThat(model.state.value.isHotCryptoVisible).isTrue()
        model.state.value.onBackClick()
        verify { router.pop() }
        model.onDestroy()
    }
}