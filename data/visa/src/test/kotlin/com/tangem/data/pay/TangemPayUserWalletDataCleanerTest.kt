package com.tangem.data.pay

import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.datasource.local.visa.TangemPayTxHistoryItemsStore
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayUserWalletDataCleanerTest {

    private val paymentAccountStatusesStore: PaymentAccountStatusesStore = mockk()
    private val txHistoryItemsStore: TangemPayTxHistoryItemsStore = mockk()

    private val cleaner = TangemPayUserWalletDataCleaner(
        paymentAccountStatusesStore = paymentAccountStatusesStore,
        txHistoryItemsStore = txHistoryItemsStore,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(paymentAccountStatusesStore, txHistoryItemsStore)
        coEvery { paymentAccountStatusesStore.remove(any<List<UserWalletId>>()) } just Runs
        coEvery { txHistoryItemsStore.remove(any<List<String>>()) } just Runs
    }

    @Test
    fun `GIVEN wallets WHEN clear THEN each store is cleared once with all ids in a single call`() = runTest {
        // Act
        cleaner.clear(listOf(WALLET_A, WALLET_B))

        // Assert
        coVerify(exactly = 1) { paymentAccountStatusesStore.remove(listOf(WALLET_A, WALLET_B)) }
        coVerify(exactly = 1) { txHistoryItemsStore.remove(listOf(WALLET_A.stringValue, WALLET_B.stringValue)) }
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}