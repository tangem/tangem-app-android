package com.tangem.features.tangempay.model

import arrow.core.Either
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.features.tangempay.components.TangemPayVaBankingDetailsErrorComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayVaBankingDetailsErrorModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")

    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val onDismiss: () -> Unit = mockk(relaxed = true)
    private val onContactSupport: () -> Unit = mockk(relaxed = true)
    private val onResolved: (VirtualAccountOnramp) -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(paymentAccountStatusFetcher, paymentAccountStatusSupplier, onDismiss, onContactSupport, onResolved)
    }

    @Test
    fun `GIVEN refetch resolves to available WHEN retry THEN onResolved called`() = runTest {
        // Arrange
        val onramp = VirtualAccountOnramp.Available(productInstanceId = "pi_1", bankCredentials = mockk())
        coEvery { paymentAccountStatusFetcher.invoke(userWalletId) } returns Unit.right()
        stubSupplier(onramp)
        val model = createModel()

        // Act
        model.uiState.value.onRetryClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onResolved(onramp) }
    }

    @Test
    fun `GIVEN refetch still fails WHEN retry THEN onResolved not called and loading reset`() = runTest {
        // Arrange
        coEvery { paymentAccountStatusFetcher.invoke(userWalletId) } returns Unit.right()
        stubSupplier(VirtualAccountOnramp.BankCredentialsError)
        val model = createModel()

        // Act
        model.uiState.value.onRetryClick()
        advanceUntilIdle()

        // Assert
        verify { onResolved wasNot Called }
        assertThat(model.uiState.value.isRetryLoading).isFalse()
    }

    @Test
    fun `GIVEN refetch in progress WHEN retry twice THEN fetch invoked once and loading shown`() = runTest {
        // Arrange
        val pending = CompletableDeferred<Either<Throwable, Unit>>()
        coEvery { paymentAccountStatusFetcher.invoke(userWalletId) } coAnswers { pending.await() }
        stubSupplier(VirtualAccountOnramp.BankCredentialsError)
        val model = createModel()

        // Act
        model.uiState.value.onRetryClick() // starts loading, fetch suspends
        advanceUntilIdle()
        model.uiState.value.onRetryClick() // gated by isRetryLoading — must be ignored
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isRetryLoading).isTrue()
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(userWalletId) }

        pending.complete(Unit.right()) // let the in-flight call finish cleanly
        advanceUntilIdle()
    }

    private fun stubSupplier(onramp: VirtualAccountOnramp) {
        val loaded = mockk<PaymentAccountStatusValue.Loaded>()
        every { loaded.virtualAccount } returns onramp
        val status = mockk<AccountStatus.Payment>()
        every { status.value } returns loaded
        every { paymentAccountStatusSupplier.invoke(userWalletId) } returns flowOf(status)
    }

    private fun TestScope.createModel() = TangemPayVaBankingDetailsErrorModel(
        paramsContainer = MutableParamsContainer(
            TangemPayVaBankingDetailsErrorComponent.Params(
                userWalletId = userWalletId,
                onDismiss = onDismiss,
                onContactSupport = onContactSupport,
                onResolved = onResolved,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
    )

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}