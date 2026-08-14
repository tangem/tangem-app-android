package com.tangem.datasource.local.onramp.pairs

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.onramp.model.OnrampPair
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultOnrampPairsStoreTest {

    private val store = DefaultOnrampPairsStore(store = RuntimeSharedMapStore())

    @Test
    fun `GIVEN empty store WHEN getSyncOrNull THEN returns null`() = runTest {
        assertThat(store.getSyncOrNull(key = "k")).isNull()
    }

    @Test
    fun `GIVEN stored value WHEN getSyncOrNull THEN returns it`() = runTest {
        // Arrange
        val value = listOf(mockk<OnrampPair>())
        store.store(key = "k", value = value)

        // Assert
        assertThat(store.getSyncOrNull(key = "k")).isEqualTo(value)
    }

    @Test
    fun `GIVEN stored value WHEN clear THEN store is empty`() = runTest {
        // Arrange
        store.store(key = "k", value = listOf(mockk<OnrampPair>()))

        // Act
        store.clear()

        // Assert
        assertThat(store.getSyncOrNull(key = "k")).isNull()
    }
}