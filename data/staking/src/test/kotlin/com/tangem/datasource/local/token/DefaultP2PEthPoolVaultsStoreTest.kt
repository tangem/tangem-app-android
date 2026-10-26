package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultP2PEthPoolVaultsStoreTest {

    private val dataStore: DataStore<List<P2PEthPoolVault>> = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(dataStore)
    }

    @Test
    fun `GIVEN data store emits nothing WHEN getSync THEN empty list`() = runTest {
        // Arrange
        every { dataStore.data } returns emptyFlow()
        val store = DefaultP2PEthPoolVaultsStore(dataStore)

        // Act
        val actual = store.getSync()

        // Assert
        assertThat(actual).isEqualTo(emptyList<P2PEthPoolVault>())
    }
}