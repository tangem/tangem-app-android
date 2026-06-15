package com.tangem.features.send.deeplink

import arrow.core.right
import com.tangem.common.routing.AppRouter
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.model.PendingOfframp
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultSellRedirectDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val offrampRepository: OfframpRepository = mockk()

    private val userWalletId = UserWalletId("0011223344556677")
    private val currencyId = "bitcoin"
    private val requestId = "request-id-001"
    private val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }

    @BeforeEach
    fun setup() {
        clearMocks(appRouter, getSelectedWalletSyncUseCase, singleAccountListSupplier, offrampRepository)
        every { getSelectedWalletSyncUseCase() } returns userWallet.right()
        // Returning null here means the (legitimate) currency lookup yields nothing, so a passed gate stops before
        // navigation. We assert the gate via whether the currency lookup is reached at all.
        coEvery { singleAccountListSupplier.getSyncOrNull(any<UserWalletId>()) } returns null
    }

    @Test
    fun `GIVEN matching pending offramp WHEN deeplink handled THEN request passes the gate`() = runTest {
        coEvery { offrampRepository.consumePendingOfframp(requestId, userWalletId, currencyId) } returns pendingOfframp()

        createHandler(validParams())
        advanceUntilIdle()

        coVerify(exactly = 1) { offrampRepository.consumePendingOfframp(requestId, userWalletId, currencyId) }
        coVerify(exactly = 1) { singleAccountListSupplier.getSyncOrNull(userWalletId) }
    }

    @Test
    fun `GIVEN no request_id WHEN deeplink handled THEN rejected without touching the store`() = runTest {
        createHandler(validParams() - REQUEST_ID_KEY)
        advanceUntilIdle()

        coVerify(exactly = 0) { offrampRepository.consumePendingOfframp(any(), any(), any()) }
        coVerify(exactly = 0) { singleAccountListSupplier.getSyncOrNull(any<UserWalletId>()) }
        verify(exactly = 0) { appRouter.push(any()) }
    }

    @Test
    fun `GIVEN no matching pending offramp WHEN deeplink handled THEN rejected`() = runTest {
        coEvery { offrampRepository.consumePendingOfframp(requestId, userWalletId, currencyId) } returns null

        createHandler(validParams())
        advanceUntilIdle()

        coVerify(exactly = 0) { singleAccountListSupplier.getSyncOrNull(any<UserWalletId>()) }
        verify(exactly = 0) { appRouter.push(any()) }
    }

    private fun TestScope.createHandler(queryParams: Map<String, String>) = DefaultSellRedirectDeepLinkHandler(
        scope = this,
        queryParams = queryParams,
        appRouter = appRouter,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        singleAccountListSupplier = singleAccountListSupplier,
        offrampRepository = offrampRepository,
    )

    private fun pendingOfframp() = PendingOfframp(
        requestId = requestId,
        userWalletId = userWalletId,
        currencyId = currencyId,
        createdAt = 0L,
    )

    private fun validParams() = mapOf(
        "currency_id" to currencyId,
        "transactionId" to "tx-001",
        "baseCurrencyAmount" to "1.5",
        "depositWalletAddress" to "depositAddress",
        REQUEST_ID_KEY to requestId,
    )

    private companion object {
        const val REQUEST_ID_KEY = "request_id"
    }
}