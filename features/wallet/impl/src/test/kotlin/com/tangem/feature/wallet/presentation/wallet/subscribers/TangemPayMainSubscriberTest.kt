package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.usecase.order.RestoreOrdersUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayMainSubscriberTest {

    private val userWallet: UserWallet = mockk { every { walletId } returns USER_WALLET_ID }
    private val restoreOrdersUseCase: RestoreOrdersUseCase = mockk(relaxed = true)
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(restoreOrdersUseCase, tangemPayFeatureToggles, paymentAccountStatusSupplier)
        every { paymentAccountStatusSupplier(userWalletId = USER_WALLET_ID) } returns emptyFlow()
    }

    @Test
    fun `GIVEN order framework enabled WHEN subscribed THEN orders are restored`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns true

        // Act
        collectSubscriber()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { restoreOrdersUseCase(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN order framework disabled WHEN subscribed THEN orders are not restored`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns false

        // Act
        collectSubscriber()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { restoreOrdersUseCase(any()) }
    }

    private fun TestScope.collectSubscriber() {
        TangemPayMainSubscriber(
            userWallet = userWallet,
            tangemPayWithdrawRepository = mockk(relaxed = true),
            restoreActiveCardOrdersUseCase = mockk(relaxed = true),
            restoreOrdersUseCase = restoreOrdersUseCase,
            tangemPayFeatureToggles = tangemPayFeatureToggles,
            pollTangemPayAccountStatusesUseCase = mockk(relaxed = true),
            moneyAccountFeatureToggles = mockk(relaxed = true),
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            analytics = mockk(relaxed = true),
        ).subscribe(
            coroutineScope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
            dispatchers = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("01")
    }
}