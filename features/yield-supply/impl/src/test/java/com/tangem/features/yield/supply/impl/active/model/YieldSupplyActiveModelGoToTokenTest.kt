package com.tangem.features.yield.supply.impl.active.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.details.NavigationAction
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.features.yield.supply.impl.incentives.claim.result.entity.YieldSupplyClaimResultUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyActiveModelGoToTokenTest : YieldSupplyActiveModelTestBase() {

    @BeforeEach
    fun setUp() {
        clearMocks(appRouter)
        coEvery { getYieldBoostStatusUseCase(any(), any()) } returns YieldBoostStatus.NotStarted.right()
    }

    @Test
    fun `GIVEN the token screen in the stack WHEN go to token is tapped THEN it pops back to that screen`() = runTest {
        // Arrange
        val tokenRoute = AppRoute.CurrencyDetails(
            userWalletId = userWalletId,
            currency = token,
            navigationAction = mockk<NavigationAction>(),
            shouldShowMarketBlock = false,
        )
        every { appRouter.stack } returns listOf(AppRoute.Wallet, tokenRoute, AppRoute.Wallet)
        val model = createModel(dispatchers = createTestingCoroutineDispatcherProvider())
        advanceUntilIdle()
        model.onClaimSubmitted(submittedResult())

        // Act
        model.onGoToTokenClick()

        // Assert
        verify(exactly = 1) { appRouter.popTo(route = tokenRoute, onComplete = any()) }
        verify(exactly = 0) { appRouter.replaceCurrent(route = any(), onComplete = any()) }
        assertThat(model.claimResultFlow.value).isNull()

        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no screen of this token WHEN go to token is tapped THEN Yield Mode is replaced with it`(
        model: NoTokenScreenModel,
    ) = runTest {
        // Arrange
        every { appRouter.stack } returns model.stack
        val expectedRoute = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = token)
        val activeModel = createModel(dispatchers = createTestingCoroutineDispatcherProvider())
        advanceUntilIdle()

        // Act
        activeModel.onGoToTokenClick()

        // Assert
        verify(exactly = 1) { appRouter.replaceCurrent(route = expectedRoute, onComplete = any()) }
        verify(exactly = 0) { appRouter.popTo(route = any<AppRoute>(), onComplete = any()) }

        activeModel.onDestroy()
    }

    internal data class NoTokenScreenModel(val reason: String, val stack: List<AppRoute>) {
        override fun toString(): String = reason
    }

    private fun provideTestModels() = listOf(
        NoTokenScreenModel(reason = "opened from the main screen", stack = listOf(AppRoute.Wallet)),
        NoTokenScreenModel(
            reason = "another token screen is beneath",
            stack = listOf(AppRoute.Wallet, AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = otherToken)),
        ),
        NoTokenScreenModel(
            reason = "the same token of another wallet is beneath",
            stack = listOf(
                AppRoute.Wallet,
                AppRoute.CurrencyDetails(userWalletId = UserWalletId(stringValue = "FEDCBA9876543210"), currency = token),
            ),
        ),
    )

    private val otherToken: CryptoCurrency.Token = mockk(relaxed = true) {
        every { id } returns CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(NETWORK_ID),
            suffix = CryptoCurrency.ID.Suffix.RawID("0xusdc"),
        )
    }

    private fun submittedResult() = YieldSupplyClaimResultUM(
        status = YieldSupplyClaimResultUM.Status.Submitted,
        total = stringReference("$45.50"),
        serviceFee = null,
        networkFee = null,
    )
}