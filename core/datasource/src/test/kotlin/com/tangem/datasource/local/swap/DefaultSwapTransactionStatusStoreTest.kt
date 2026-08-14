package com.tangem.datasource.local.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultSwapTransactionStatusStoreTest {

    private val store = DefaultSwapTransactionStatusStore(store = RuntimeSharedMapStore())

    @Test
    fun `GIVEN empty store WHEN getTransactionStatus THEN returns null`() = runTest {
        // Act
        val actual = store.getTransactionStatus(txId = "tx1")

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN status set WHEN getTransactionStatus THEN returns it`() = runTest {
        // Arrange
        store.setTransactionStatus(txId = "tx1", status = ExpressAnalyticsStatus.InProgress)

        // Act
        val actual = store.getTransactionStatus(txId = "tx1")

        // Assert
        assertThat(actual).isEqualTo(ExpressAnalyticsStatus.InProgress)
    }

    @Test
    fun `GIVEN status set twice WHEN getTransactionStatus THEN returns the latest`() = runTest {
        // Arrange
        store.setTransactionStatus(txId = "tx1", status = ExpressAnalyticsStatus.InProgress)
        store.setTransactionStatus(txId = "tx1", status = ExpressAnalyticsStatus.Done)

        // Act
        val actual = store.getTransactionStatus(txId = "tx1")

        // Assert
        assertThat(actual).isEqualTo(ExpressAnalyticsStatus.Done)
    }

    @Test
    fun `GIVEN statuses for several tx WHEN getTransactionStatus THEN each is independent`() = runTest {
        // Arrange
        store.setTransactionStatus(txId = "tx1", status = ExpressAnalyticsStatus.Done)
        store.setTransactionStatus(txId = "tx2", status = ExpressAnalyticsStatus.Fail)

        // Assert
        assertThat(store.getTransactionStatus(txId = "tx1")).isEqualTo(ExpressAnalyticsStatus.Done)
        assertThat(store.getTransactionStatus(txId = "tx2")).isEqualTo(ExpressAnalyticsStatus.Fail)
    }
}