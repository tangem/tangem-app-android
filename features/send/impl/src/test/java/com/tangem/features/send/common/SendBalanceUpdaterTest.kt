package com.tangem.features.send.common

import arrow.core.right
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class SendBalanceUpdaterTest {

    private val getTxHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase = mockk()
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk(relaxed = true)

    private val userWalletId = UserWalletId(stringValue = "0123456789")
    private val userWallet: UserWallet = mockk {
        every { walletId } returns userWalletId
    }
    private val sentNetwork: Network = mockk()
    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true) {
        every { network } returns sentNetwork
    }

    @Test
    fun `GIVEN a send WHEN updates are scheduled THEN non-portfolio accounts refresh the sent network`() = runTest {
        // Arrange
        coEvery { getTxHistoryItemsCountUseCase(userWalletId, cryptoCurrency) } returns 0.right()
        val updater = createUpdater(testScope = this)

        // Act
        updater.scheduleUpdates()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { nonPortfolioCurrenciesProvider.refresh(userWalletId, sentNetwork) }
    }

    private fun createUpdater(testScope: TestScope) = SendBalanceUpdater(
        fetchPendingTransactionsUseCase = mockk(relaxed = true),
        updateDelayedNetworkStatusUseCase = mockk(relaxed = true),
        getTxHistoryItemsCountUseCase = getTxHistoryItemsCountUseCase,
        txHistoryContentUpdateEmitter = mockk(relaxed = true),
        refreshAllNFTUseCase = mockk(relaxed = true),
        nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
        coroutineScope = TestAppCoroutineScope(testScope = testScope),
        userWallet = userWallet,
        cryptoCurrency = cryptoCurrency,
    )
}