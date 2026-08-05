package com.tangem.data.pay

import com.google.common.truth.Truth.assertThat
import com.tangem.data.pay.repository.TangemPayRequestPerformer
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.data.pay.store.TangemPayCustomerInfoStore
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.datasource.local.visa.TangemPayTxHistoryItemsStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
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
    private val tangemPayStorage: TangemPayStorage = mockk()
    // A real store, so the assertions can look at what actually stayed in the cache
    private val customerInfoStore = TangemPayCustomerInfoStore()
    private val requestPerformer: TangemPayRequestPerformer = mockk()

    private val cleaner = TangemPayUserWalletDataCleaner(
        paymentAccountStatusesStore = paymentAccountStatusesStore,
        txHistoryItemsStore = txHistoryItemsStore,
        tangemPayStorage = tangemPayStorage,
        customerInfoStore = customerInfoStore,
        requestPerformer = requestPerformer,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(paymentAccountStatusesStore, txHistoryItemsStore, tangemPayStorage, requestPerformer)
        customerInfoStore.clear()
        coEvery { paymentAccountStatusesStore.remove(any<List<UserWalletId>>()) } just Runs
        coEvery { txHistoryItemsStore.remove(any<List<String>>()) } just Runs
        coEvery { tangemPayStorage.getCustomerWalletAddress(any()) } returns null
        coEvery { tangemPayStorage.clearAll(any(), any()) } just Runs
        coEvery { tangemPayStorage.clearIsTangemPayDeactivated(any()) } just Runs
        every { requestPerformer.removeCachedCustomerWalletAddresses(any()) } just Runs
    }

    @Test
    fun `GIVEN wallets WHEN clear THEN each store is cleared once with all ids in a single call`() = runTest {
        // Act
        cleaner.clear(listOf(WALLET_A, WALLET_B))

        // Assert
        coVerify(exactly = 1) { paymentAccountStatusesStore.remove(listOf(WALLET_A, WALLET_B)) }
        coVerify(exactly = 1) { txHistoryItemsStore.remove(listOf(WALLET_A.stringValue, WALLET_B.stringValue)) }
        coVerify(exactly = 1) { requestPerformer.removeCachedCustomerWalletAddresses(listOf(WALLET_A, WALLET_B)) }
    }

    @Test
    fun `GIVEN cached customer info WHEN clear THEN only the removed wallets are dropped from the cache`() = runTest {
        // Arrange
        customerInfoStore.store(mapOf(WALLET_A to CUSTOMER_INFO, WALLET_B to CUSTOMER_INFO))

        // Act
        cleaner.clear(listOf(WALLET_A))

        // Assert
        assertThat(customerInfoStore.get().value.keys).containsExactly(WALLET_B)
    }

    @Test
    fun `GIVEN stored address WHEN clear THEN clearAll receives it and runs after the address is read`() = runTest {
        // Arrange
        coEvery { tangemPayStorage.getCustomerWalletAddress(WALLET_A) } returns ADDRESS

        // Act
        cleaner.clear(listOf(WALLET_A))

        // Assert
        coVerifyOrder {
            tangemPayStorage.getCustomerWalletAddress(WALLET_A)
            tangemPayStorage.clearAll(userWalletId = WALLET_A, customerWalletAddress = ADDRESS)
        }
    }

    @Test
    fun `GIVEN no stored address WHEN clear THEN clearAll is still called with a null address`() = runTest {
        // Arrange
        coEvery { tangemPayStorage.getCustomerWalletAddress(WALLET_A) } returns null

        // Act
        cleaner.clear(listOf(WALLET_A))

        // Assert
        coVerify(exactly = 1) { tangemPayStorage.clearAll(userWalletId = WALLET_A, customerWalletAddress = null) }
    }

    @Test
    fun `GIVEN wallets WHEN clear THEN the deactivated marker is dropped for every wallet`() = runTest {
        // Act
        cleaner.clear(listOf(WALLET_A, WALLET_B))

        // Assert
        coVerify(exactly = 1) { tangemPayStorage.clearIsTangemPayDeactivated(WALLET_A) }
        coVerify(exactly = 1) { tangemPayStorage.clearIsTangemPayDeactivated(WALLET_B) }
    }

    @Test
    fun `GIVEN several wallets WHEN clear THEN stored data is cleared for each of them`() = runTest {
        // Arrange
        coEvery { tangemPayStorage.getCustomerWalletAddress(WALLET_A) } returns ADDRESS
        coEvery { tangemPayStorage.getCustomerWalletAddress(WALLET_B) } returns OTHER_ADDRESS

        // Act
        cleaner.clear(listOf(WALLET_A, WALLET_B))

        // Assert
        coVerify(exactly = 1) { tangemPayStorage.clearAll(userWalletId = WALLET_A, customerWalletAddress = ADDRESS) }
        coVerify(exactly = 1) {
            tangemPayStorage.clearAll(userWalletId = WALLET_B, customerWalletAddress = OTHER_ADDRESS)
        }
    }

    @Test
    fun `GIVEN no wallets WHEN clear THEN nothing is read from the storage`() = runTest {
        // Act
        cleaner.clear(emptyList())

        // Assert
        coVerify(exactly = 0) { tangemPayStorage.getCustomerWalletAddress(any()) }
        coVerify(exactly = 0) { tangemPayStorage.clearAll(any(), any()) }
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
        val CUSTOMER_INFO: CustomerInfo = mockk()
        const val ADDRESS = "0xaddress_a"
        const val OTHER_ADDRESS = "0xaddress_b"
    }
}