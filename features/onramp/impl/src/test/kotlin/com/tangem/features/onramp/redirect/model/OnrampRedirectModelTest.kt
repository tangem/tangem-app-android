package com.tangem.features.onramp.redirect.model

import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.redirect.OnrampRedirectComponent
import com.tangem.features.onramp.success.OnrampSuccessScreenListener
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampRedirectModelTest {

    private val userWalletId = UserWalletId(stringValue = "0011")
    private val successTrigger = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    private val onBack: () -> Unit = mockk(relaxed = true)
    private val onSuccess: (String) -> Unit = mockk(relaxed = true)

    @Test
    fun `GIVEN redirected to provider WHEN purchase succeeds THEN host opens success for the transaction`() = runTest {
        // Arrange
        val model = createModel()
        model.latestOnrampTransaction = mockk<OnrampTransaction> { every { txId } returns TX_ID }

        // Act
        successTrigger.emit(true)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onSuccess(TX_ID) }
        verify(exactly = 0) { onBack() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN redirected to provider WHEN purchase is not completed THEN redirect screen is closed`() = runTest {
        // Arrange
        val model = createModel()
        model.latestOnrampTransaction = mockk<OnrampTransaction> { every { txId } returns TX_ID }

        // Act
        successTrigger.emit(false)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onBack() }
        verify(exactly = 0) { onSuccess(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no redirect yet WHEN success arrives THEN redirect screen is closed without success`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        successTrigger.emit(true)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onBack() }
        verify(exactly = 0) { onSuccess(any()) }
        model.onDestroy()
    }

    private fun TestScope.createModel(): OnrampRedirectModel {
        val userWallet: UserWallet = mockk(relaxed = true) { every { walletId } returns userWalletId }
        val getWalletsUseCase: GetWalletsUseCase = mockk { every { invokeSync() } returns listOf(userWallet) }
        val successListener: OnrampSuccessScreenListener = mockk {
            every { onrampSuccessTriggerFlow } returns successTrigger
        }

        return OnrampRedirectModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            urlOpener = mockk(relaxed = true),
            getOnrampRedirectUrlUseCase = mockk(relaxed = true),
            messageSender = mockk(relaxed = true),
            analyticsEventHandler = mockk(relaxed = true),
            onrampSuccessScreenListener = successListener,
            getAppThemeModeUseCase = mockk(relaxed = true),
            router = mockk(relaxed = true),
            paramsContainer = MutableParamsContainer(
                OnrampRedirectComponent.Params(
                    userWalletId = userWalletId,
                    onBack = onBack,
                    onSuccess = onSuccess,
                    cryptoCurrency = mockk(relaxed = true),
                    onrampProviderWithQuote = mockk(relaxed = true),
                ),
            ),
            getWalletsUseCase = getWalletsUseCase,
        ).also { advanceUntilIdle() }
    }

    private companion object {
        const val TX_ID = "tx_id"
    }
}