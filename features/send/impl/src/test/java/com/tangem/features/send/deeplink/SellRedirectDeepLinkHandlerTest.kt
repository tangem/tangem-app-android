package com.tangem.features.send.deeplink

import android.net.Uri
import arrow.core.right
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.model.PendingOfframp
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.test.core.ProvideTestModels
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SellRedirectDeepLinkHandlerTest {

    private val router: Router = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val offrampRepository: OfframpRepository = mockk()

    private val userWalletId = UserWalletId("0011223344556677")
    private val currencyId = "bitcoin"
    private val requestId = "request-id-001"
    private val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }

    @BeforeEach
    fun setup() {
        clearMocks(router, getSelectedWalletSyncUseCase, singleAccountListSupplier, offrampRepository)
        every { getSelectedWalletSyncUseCase() } returns userWallet.right()
        // Returning null here means the (legitimate) currency lookup yields nothing, so a passed gate stops before
        // navigation. We assert the gate via whether the currency lookup is reached at all.
        coEvery { singleAccountListSupplier.getSyncOrNull(any<UserWalletId>()) } returns null
    }

    @Test
    fun `GIVEN matching pending offramp WHEN deeplink handled THEN request passes the gate`() = runTest {
        coEvery { offrampRepository.resolvePendingOfframp(requestId, userWalletId, currencyId) } returns pendingOfframp()

        handle(validParams())
        advanceUntilIdle()

        coVerify(exactly = 1) { offrampRepository.resolvePendingOfframp(requestId, userWalletId, currencyId) }
        coVerify(exactly = 1) { singleAccountListSupplier.getSyncOrNull(userWalletId) }
    }

    @Test
    fun `GIVEN matching pending offramp WHEN deeplink handled twice THEN gate passes both times`() = runTest {
        // The pending sell is not single-use: resolving it does not remove it, so re-opening the same deeplink must
        // pass the gate again. Guards against reintroducing single-use behavior in the handler.
        coEvery { offrampRepository.resolvePendingOfframp(requestId, userWalletId, currencyId) } returns pendingOfframp()

        handle(validParams())
        advanceUntilIdle()
        handle(validParams())
        advanceUntilIdle()

        coVerify(exactly = 2) { offrampRepository.resolvePendingOfframp(requestId, userWalletId, currencyId) }
        coVerify(exactly = 2) { singleAccountListSupplier.getSyncOrNull(userWalletId) }
    }

    @Test
    fun `GIVEN no request_id WHEN deeplink handled THEN rejected without touching the store`() = runTest {
        handle(validParams() - REQUEST_ID_KEY)
        advanceUntilIdle()

        coVerify(exactly = 0) { offrampRepository.resolvePendingOfframp(any(), any(), any()) }
        coVerify(exactly = 0) { singleAccountListSupplier.getSyncOrNull(any<UserWalletId>()) }
        verify(exactly = 0) { router.push(any()) }
    }

    @Test
    fun `GIVEN no matching pending offramp WHEN deeplink handled THEN rejected`() = runTest {
        coEvery { offrampRepository.resolvePendingOfframp(requestId, userWalletId, currencyId) } returns null

        handle(validParams())
        advanceUntilIdle()

        coVerify(exactly = 0) { singleAccountListSupplier.getSyncOrNull(any<UserWalletId>()) }
        verify(exactly = 0) { router.push(any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN non-positive or invalid amount WHEN deeplink handled THEN rejected`(amount: String) = runTest {
        // Arrange
        coEvery { offrampRepository.resolvePendingOfframp(requestId, userWalletId, currencyId) } returns pendingOfframp()

        // Act
        handle(validParams() + ("baseCurrencyAmount" to amount))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { offrampRepository.resolvePendingOfframp(any(), any(), any()) }
        verify(exactly = 0) { router.push(any()) }
    }

    private fun provideTestModels() = listOf("-10", "0", "abc")

    private val handler = SellRedirectDeepLinkHandler(
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        singleAccountListSupplier = singleAccountListSupplier,
        offrampRepository = offrampRepository,
    )

    private fun TestScope.handle(queryParams: Map<String, String>) {
        handler.handle(
            DeepLinkRequest(
                uri = mockk<Uri>(),
                queryParams = queryParams,
                scope = this,
                isFromOnNewIntent = false,
                router = router,
            ),
        )
    }

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