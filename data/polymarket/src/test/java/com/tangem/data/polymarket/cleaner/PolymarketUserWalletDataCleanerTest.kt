package com.tangem.data.polymarket.cleaner

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PolymarketUserWalletDataCleanerTest {

    private val credentialsStore: PolymarketCredentialsStore = mockk(relaxUnitFun = true)

    private val cleaner = PolymarketUserWalletDataCleaner(credentialsStore = credentialsStore)

    @BeforeEach
    fun resetMocks() {
        clearMocks(credentialsStore)
    }

    @Test
    fun `GIVEN two removed wallets WHEN clear THEN the store is cleared for both`() = runTest {
        // Act
        cleaner.clear(listOf(WALLET_A, WALLET_B))

        // Assert
        coVerify(exactly = 1) { credentialsStore.clear(WALLET_A) }
        coVerify(exactly = 1) { credentialsStore.clear(WALLET_B) }
    }

    @Test
    fun `GIVEN an empty list WHEN clear THEN the store is untouched`() = runTest {
        // Act
        cleaner.clear(emptyList())

        // Assert
        coVerify(exactly = 0) { credentialsStore.clear(any()) }
    }

    @Test
    fun `GIVEN a wallet that never onboarded WHEN clear THEN it does not read before deleting`() = runTest {
        // Act
        cleaner.clear(listOf(WALLET_A))

        // Assert — deleting an absent key is already a no-op, so a guarding read would be dead weight
        coVerify(exactly = 1) { credentialsStore.clear(WALLET_A) }
        coVerify(exactly = 0) { credentialsStore.get(any()) }
    }

    @Test
    fun `GIVEN one wallet fails to clear WHEN clear THEN the others are still cleared`() = runTest {
        // Arrange
        coEvery { credentialsStore.clear(WALLET_A) } throws IllegalStateException("keystore unavailable")

        // Act
        cleaner.clear(listOf(WALLET_A, WALLET_B))

        // Assert
        coVerify(exactly = 1) { credentialsStore.clear(WALLET_B) }
    }

    /**
     * The defect this task exists for: TangemPay's credentials survived deletion because no test asserted
     * their absence afterwards.
     */
    @Test
    fun `GIVEN credentials stored for a wallet WHEN clear THEN a subsequent read returns null`() = runTest {
        // Arrange — a store that actually holds entries, not a verifying mock
        val entries = mutableMapOf(WALLET_A to CREDENTIALS)
        val store = object : PolymarketCredentialsStore {
            override suspend fun store(userWalletId: UserWalletId, credentials: PolymarketApiCredentials) {
                entries[userWalletId] = credentials
            }

            override suspend fun get(userWalletId: UserWalletId): PolymarketApiCredentials? = entries[userWalletId]

            override suspend fun clear(userWalletId: UserWalletId) {
                entries.remove(userWalletId)
            }
        }

        // Act
        PolymarketUserWalletDataCleaner(credentialsStore = store).clear(listOf(WALLET_A))

        // Assert
        assertThat(store.get(WALLET_A)).isNull()
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")

        val CREDENTIALS = PolymarketApiCredentials(
            apiKey = "key",
            secret = "c2VjcmV0",
            passphrase = "passphrase",
        )
    }
}