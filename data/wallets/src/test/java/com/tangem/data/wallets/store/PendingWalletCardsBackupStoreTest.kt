package com.tangem.data.wallets.store

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.tangemTech.models.WalletCardDTO
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PendingWalletCardsBackupStoreTest {

    private val dataStore = MockStateDataStore<PendingWalletCardsBackups>(default = emptyList())

    private val store = PendingWalletCardsBackupStore(dataStore = dataStore)

    @BeforeEach
    fun clearStore() {
        runBlocking { dataStore.updateData { emptyList() } }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Enqueue {

        @Test
        fun `GIVEN nothing queued WHEN enqueue THEN the entry is stored`() = runTest {
            // Arrange
            val entry = entry(id = "1")

            // Act
            store.enqueue(entry)

            // Assert
            assertThat(store.getAll()).containsExactly(entry)
        }

        @Test
        fun `GIVEN entries queued WHEN enqueue THEN they are kept in the order they were made`() = runTest {
            // Arrange
            val entries = listOf(entry(id = "1"), entry(id = "2"), entry(id = "3"))

            // Act
            entries.forEach { store.enqueue(it) }

            // Assert
            assertThat(store.getAll()).containsExactlyElementsIn(entries).inOrder()
        }

        @Test
        fun `GIVEN the queue is full WHEN enqueue THEN the oldest entry is dropped`() = runTest {
            // Arrange
            repeat(times = MAX_PENDING) { store.enqueue(entry(id = it.toString())) }
            val newest = entry(id = "newest")

            // Act
            store.enqueue(newest)

            // Assert
            val actual = store.getAll()
            assertThat(actual).hasSize(MAX_PENDING)
            assertThat(actual.first()).isEqualTo(entry(id = "1"))
            assertThat(actual.last()).isEqualTo(newest)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Remove {

        @Test
        fun `GIVEN entries queued WHEN remove THEN only the matching one is gone`() = runTest {
            // Arrange
            listOf(entry(id = "1"), entry(id = "2"), entry(id = "3")).forEach { store.enqueue(it) }

            // Act
            store.remove(id = "2")

            // Assert
            assertThat(store.getAll()).containsExactly(entry(id = "1"), entry(id = "3")).inOrder()
        }

        @Test
        fun `GIVEN the id is unknown WHEN remove THEN the queue is untouched`() = runTest {
            // Arrange
            store.enqueue(entry(id = "1"))

            // Act
            store.remove(id = "absent")

            // Assert
            assertThat(store.getAll()).containsExactly(entry(id = "1"))
        }
    }

    @Test
    fun `GIVEN nothing was ever queued WHEN getAll THEN an empty list is returned`() = runTest {
        assertThat(store.getAll()).isEmpty()
    }

    /**
     * The queue only earns its keep by outliving the process, and it does that as JSON on disk — so the
     * entry has to survive the round-trip the real DataStore puts it through.
     */
    @Test
    fun `GIVEN a queued entry WHEN it is written and read back THEN it is unchanged`() {
        // Arrange
        val entry = entry(id = "1")

        // Act
        val actual = Json.decodeFromString<PendingWalletCardsBackup>(Json.encodeToString(entry))

        // Assert
        assertThat(actual).isEqualTo(entry)
    }

    private companion object {

        const val MAX_PENDING = 50

        fun entry(id: String) = PendingWalletCardsBackup(
            id = id,
            walletId = "wallet-$id",
            cards = listOf(
                PendingWalletCardsBackup.Card(
                    cardId = "AC0100000000000$id",
                    cardPublicKey = "0AFF",
                    role = WalletCardDTO.Role.PRIMARY,
                    backupStatus = WalletCardDTO.BackupStatus.CARD_LINKED,
                    curves = listOf("secp256k1"),
                ),
            ),
            usedSeed = false,
        )
    }
}