package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.usecase.RestoreActiveCardOrdersUseCase
import com.tangem.domain.tangempay.usecase.order.RestoreOrdersUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class TangemPayMainSubscriberTest {

    private val userWallet: UserWallet = mockk { every { walletId } returns USER_WALLET_ID }
    private val restoreOrdersUseCase: RestoreOrdersUseCase = mockk(relaxed = true)
    private val restoreActiveCardOrdersUseCase: RestoreActiveCardOrdersUseCase = mockk(relaxed = true)
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val statuses = MutableSharedFlow<AccountStatus.Payment>(replay = 1)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            restoreOrdersUseCase,
            restoreActiveCardOrdersUseCase,
            tangemPayFeatureToggles,
            paymentAccountStatusSupplier,
        )
        statuses.resetReplayCache()
        every { paymentAccountStatusSupplier(userWalletId = USER_WALLET_ID) } returns statuses
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns true
    }

    @Test
    fun `GIVEN order framework enabled WHEN synced status emitted THEN orders are restored`() = runTest {
        // Arrange
        collectSubscriber()

        // Act
        statuses.emit(status(VERIFIED))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { restoreOrdersUseCase(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN synced status WHEN another synced status emitted THEN orders are not restored again`() = runTest {
        // Arrange
        collectSubscriber()
        statuses.emit(status(VERIFIED))
        advanceUntilIdle()

        // Act
        statuses.emit(status(PaymentAccountStatusValue.IssuingCard(StatusSource.ACTUAL)))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { restoreOrdersUseCase(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN not synced status WHEN session becomes synced THEN orders are restored again`() = runTest {
        // Arrange
        collectSubscriber()
        statuses.emit(status(VERIFIED))
        advanceUntilIdle()
        statuses.emit(status(PaymentAccountStatusValue.Error.NotSynced))
        advanceUntilIdle()

        // Act
        statuses.emit(status(VERIFIED))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { restoreOrdersUseCase(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN not synced status WHEN subscribed THEN orders are not restored`() = runTest {
        // Arrange
        collectSubscriber()

        // Act
        statuses.emit(status(PaymentAccountStatusValue.Error.NotSynced))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { restoreOrdersUseCase(any()) }
    }

    @Test
    fun `GIVEN order framework disabled WHEN synced status emitted THEN orders are not restored`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns false
        collectSubscriber()

        // Act
        statuses.emit(status(VERIFIED))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { restoreOrdersUseCase(any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN order framework toggle WHEN subscribed THEN active card orders are restored`(isEnabled: Boolean) =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns isEnabled

            // Act
            collectSubscriber()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { restoreActiveCardOrdersUseCase(USER_WALLET_ID) }
        }

    private fun status(value: PaymentAccountStatusValue): AccountStatus.Payment =
        AccountStatus.Payment(account = mockk(), value = value)

    private fun TestScope.collectSubscriber() {
        TangemPayMainSubscriber(
            userWallet = userWallet,
            tangemPayWithdrawRepository = mockk(relaxed = true),
            restoreActiveCardOrdersUseCase = restoreActiveCardOrdersUseCase,
            restoreOrdersUseCase = restoreOrdersUseCase,
            tangemPayFeatureToggles = tangemPayFeatureToggles,
            pollTangemPayAccountStatusesUseCase = mockk(relaxed = true),
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            analytics = mockk(relaxed = true),
        ).subscribe(
            coroutineScope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
            dispatchers = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("01")
        val VERIFIED = PaymentAccountStatusValue.Verified(StatusSource.ACTUAL)
    }
}