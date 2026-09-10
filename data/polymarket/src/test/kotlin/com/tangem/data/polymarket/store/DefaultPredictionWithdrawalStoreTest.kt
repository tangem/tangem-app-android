package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.entity.PredictionWithdrawalRecordDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PredictionWithdrawalBatch
import com.tangem.domain.polymarket.model.PredictionWithdrawalRecord
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException

internal class DefaultPredictionWithdrawalStoreTest {

    @Test
    fun `GIVEN a saved record WHEN read back THEN it survives the trip through the DTO`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithWithdrawalRecordDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        store.save(WALLET_A, RECORD)

        // Assert
        assertThat(store.get(WALLET_A)).isEqualTo(RECORD)
        assertThat(persisted.data.first()).containsExactly(WALLET_A.stringValue, RECORD_DTO)
    }

    @Test
    fun `GIVEN a record left by a previous process WHEN get races the preload THEN the record is returned`() = runTest {
        // Arrange
        val persisted = MockStateDataStore(default = mapOf(WALLET_A.stringValue to RECORD_DTO))
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        val actual = store.get(WALLET_A)

        // Assert
        assertThat(actual).isEqualTo(RECORD)
    }

    @Test
    fun `GIVEN nothing saved WHEN get THEN it is null`() = runTest {
        // Act
        val actual = createStore(testScope = this).get(WALLET_A)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a saved record WHEN clear THEN it is dropped from both stores`() = runTest {
        // Arrange
        val persisted = MockStateDataStore<WalletIdWithWithdrawalRecordDTO>(default = emptyMap())
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        store.save(WALLET_A, RECORD)
        store.save(WALLET_B, RECORD)

        // Act
        store.clear(WALLET_A)

        // Assert
        assertThat(store.get(WALLET_A)).isNull()
        assertThat(store.get(WALLET_B)).isEqualTo(RECORD)
        assertThat(persisted.data.first()).containsExactly(WALLET_B.stringValue, RECORD_DTO)
    }

    @Test
    fun `GIVEN a record saved before the cache loads WHEN it lands THEN the preload does not undo it`() = runTest {
        // Arrange
        val persisted = MockStateDataStore(default = mapOf(WALLET_B.stringValue to RECORD_DTO))
        val store = createStore(testScope = this, persistenceDataStore = persisted)
        val acknowledged = RECORD.copy(transactionId = "0x4f2c")

        // Act
        store.save(WALLET_A, acknowledged)

        // Assert
        assertThat(store.get(WALLET_A)).isEqualTo(acknowledged)
        assertThat(store.get(WALLET_B)).isEqualTo(RECORD)
    }

    @Test
    fun `GIVEN the persisted file cannot be read WHEN get THEN it throws`() = runTest {
        // Arrange
        val cause = IOException("unreadable")
        val persisted: DataStore<WalletIdWithWithdrawalRecordDTO> = mockk()
        every { persisted.data } returns flow { throw cause }
        val store = createStore(testScope = this, persistenceDataStore = persisted)

        // Act
        val actual = runCatching { store.get(WALLET_A) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(PredictionWithdrawalStoreUnreadableException::class.java)
        assertThat(actual).hasCauseThat().isSameInstanceAs(cause)
    }

    private fun createStore(
        testScope: TestScope,
        persistenceDataStore: DataStore<WalletIdWithWithdrawalRecordDTO> = MockStateDataStore(emptyMap()),
    ) = DefaultPredictionWithdrawalStore(
        runtimeStore = RuntimeSharedStore(),
        persistenceDataStore = persistenceDataStore,
        scope = TestAppCoroutineScope(testScope),
    )

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")

        val RECORD = PredictionWithdrawalRecord(
            batch = PredictionWithdrawalBatch(
                ownerAddress = "0xabc0000000000000000000000000000000000001",
                depositWalletAddress = "0xdef0000000000000000000000000000000000002",
                nonce = "9",
                deadline = "1787346200",
                call = PolymarketApprovalCall(
                    target = "0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB",
                    value = "0",
                    data = "0xa9059cbb",
                ),
                signature = "0xdeadbeef",
            ),
            transactionId = null,
        )

        val RECORD_DTO = PredictionWithdrawalRecordDTO(
            ownerAddress = "0xabc0000000000000000000000000000000000001",
            depositWalletAddress = "0xdef0000000000000000000000000000000000002",
            nonce = "9",
            deadline = "1787346200",
            target = "0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB",
            value = "0",
            data = "0xa9059cbb",
            signature = "0xdeadbeef",
            transactionId = null,
        )
    }
}