package com.tangem.data.staking

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.domain.staking.model.UnsubmittedTransactionMetadata
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.grow.datasource.stakekit.StakeKitApi
import com.tangem.grow.datasource.stakekit.models.request.SubmitTransactionHashRequestBody
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DefaultStakeKitTransactionHashRepositoryTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val stakeKitApi: StakeKitApi = mockk()
    private val keys = GrowPreferencesKeys()
    private val prefs = createAppPreferencesStore(
        dispatchers = dispatchers,
        preferencesDataStore = MockStateDataStore(emptyPreferences()),
    )

    private val repository = DefaultStakeKitTransactionHashRepository(
        stakeKitApi = stakeKitApi,
        appPreferencesStore = prefs,
        growPreferencesKeys = keys,
        dispatchers = dispatchers,
    )

    private suspend fun seed(vararg items: UnsubmittedTransactionMetadata) {
        prefs.editData { it.setObjectList(keys.unsubmittedTransactionsKey, items.toList()) }
    }

    private suspend fun stored(): List<UnsubmittedTransactionMetadata> =
        prefs.getObjectListSync(keys.unsubmittedTransactionsKey)

    private fun bodyOf(hash: String) = SubmitTransactionHashRequestBody(hash = hash)

    @Test
    fun `GIVEN hash WHEN submitHash THEN api called with hash body`() = runTest {
        // Arrange
        coEvery { stakeKitApi.submitTransactionHash(any(), any()) } returns ApiResponse.Success(Unit)

        // Act
        repository.submitHash("tx-a", "0xa")

        // Assert
        coVerify { stakeKitApi.submitTransactionHash("tx-a", bodyOf("0xa")) }
    }

    @Test
    fun `GIVEN empty prefs WHEN storeUnsubmittedHash twice THEN both kept in order`() = runTest {
        // Act
        repository.storeUnsubmittedHash(A)
        repository.storeUnsubmittedHash(B)

        // Assert
        assertThat(stored()).containsExactly(A, B).inOrder()
    }

    @Test
    fun `GIVEN no saved hashes WHEN sendUnsubmittedHashes THEN api not called`() = runTest {
        // Act
        repository.sendUnsubmittedHashes()

        // Assert
        coVerify(exactly = 0) { stakeKitApi.submitTransactionHash(any(), any()) }
    }

    @Test
    fun `GIVEN saved hashes and api success WHEN sendUnsubmittedHashes THEN all submitted and removed`() = runTest {
        // Arrange
        seed(A, B)
        coEvery { stakeKitApi.submitTransactionHash(any(), any()) } returns ApiResponse.Success(Unit)

        // Act
        repository.sendUnsubmittedHashes()

        // Assert
        assertThat(stored()).isEmpty()
        coVerifyOrder {
            stakeKitApi.submitTransactionHash("tx-a", bodyOf("0xa"))
            stakeKitApi.submitTransactionHash("tx-b", bodyOf("0xb"))
        }
    }

    @Test
    fun `GIVEN first submit throws WHEN sendUnsubmittedHashes THEN failed hash kept and next removed`() = runTest {
        // Arrange
        seed(A, B)
        coEvery { stakeKitApi.submitTransactionHash("tx-a", any()) } throws IllegalStateException("boom")
        coEvery { stakeKitApi.submitTransactionHash("tx-b", any()) } returns ApiResponse.Success(Unit)

        // Act
        repository.sendUnsubmittedHashes()

        // Assert
        assertThat(stored()).containsExactly(A)
    }

    @Nested
    inner class RetryRoundTrip {

        @Test
        fun `GIVEN retry throws WHEN sendUnsubmittedHashes called again with success THEN hash submitted on second retry and removed`() =
            runTest {
                // Arrange
                repository.storeUnsubmittedHash(A)
                coEvery { stakeKitApi.submitTransactionHash(any(), any()) } throws
                    IllegalStateException("boom") andThen ApiResponse.Success(Unit)

                // Act
                repository.sendUnsubmittedHashes()
                val afterFirstRetry = stored()
                repository.sendUnsubmittedHashes()

                // Assert
                assertThat(afterFirstRetry).containsExactly(A)
                assertThat(stored()).isEmpty()
                coVerify(exactly = 2) { stakeKitApi.submitTransactionHash("tx-a", any()) }
            }

        @Test
        fun `GIVEN two entries with same transaction id WHEN sendUnsubmittedHashes succeeds THEN both submitted and both removed`() =
            runTest {
                // Arrange
                repository.storeUnsubmittedHash(A)
                val duplicate = UnsubmittedTransactionMetadata(transactionHash = "0xa2", transactionId = "tx-a")
                repository.storeUnsubmittedHash(duplicate)
                coEvery { stakeKitApi.submitTransactionHash(any(), any()) } returns ApiResponse.Success(Unit)

                // Act
                repository.sendUnsubmittedHashes()

                // Assert
                coVerify(exactly = 2) { stakeKitApi.submitTransactionHash("tx-a", any()) }
                assertThat(stored()).isEmpty()
            }
    }

    private companion object {

        val A = UnsubmittedTransactionMetadata(transactionHash = "0xa", transactionId = "tx-a")
        val B = UnsubmittedTransactionMetadata(transactionHash = "0xb", transactionId = "tx-b")
    }
}