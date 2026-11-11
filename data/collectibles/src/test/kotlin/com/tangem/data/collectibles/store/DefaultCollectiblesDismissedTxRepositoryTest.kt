package com.tangem.data.collectibles.store

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultCollectiblesDismissedTxRepositoryTest {

    private val store = MockStateDataStore(DismissedTxsDTO())
    private val repository = DefaultCollectiblesDismissedTxRepository(store = store)

    @Test
    fun `GIVEN nothing dismissed WHEN a tx is saved as dismissed THEN its id is read back`() = runTest {
        // Act
        repository.saveAsDismissed("tx-1")

        // Assert
        assertThat(repository.getDismissedTxIds().first()).containsExactly("tx-1")
    }

    @Test
    fun `GIVEN the store is full WHEN another tx is saved as dismissed THEN the oldest id is dropped`() = runTest {
        // Arrange
        repeat(MAX_STORED) { index -> repository.saveAsDismissed("tx-$index") }

        // Act
        repository.saveAsDismissed("tx-new")

        // Assert
        val actual = store.data.first().txIds
        assertThat(actual).hasSize(MAX_STORED)
        assertThat(actual.first()).isEqualTo("tx-1")
        assertThat(actual.last()).isEqualTo("tx-new")
    }

    @Test
    fun `GIVEN a dismissed tx WHEN it is saved again THEN it moves to the newest end`() = runTest {
        // Arrange
        repository.saveAsDismissed("tx-1")
        repository.saveAsDismissed("tx-2")

        // Act
        repository.saveAsDismissed("tx-1")

        // Assert
        assertThat(store.data.first().txIds).containsExactly("tx-2", "tx-1").inOrder()
    }

    private companion object {
        const val MAX_STORED = 100
    }
}