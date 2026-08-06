package com.tangem.datasource.local.onramp.paymentmethods

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultOnrampPaymentMethodsStoreTest {

    private val store = DefaultOnrampPaymentMethodsStore(store = RuntimeSharedMapStore())

    @Test
    fun `GIVEN empty store WHEN getSyncOrNull THEN returns null`() = runTest {
        assertThat(store.getSyncOrNull(key = "k")).isNull()
    }

    @Test
    fun `GIVEN stored value WHEN getSyncOrNull THEN returns it`() = runTest {
        // Arrange
        val value = listOf(mockk<PaymentMethodDTO>())
        store.store(key = "k", value = value)

        // Assert
        assertThat(store.getSyncOrNull(key = "k")).isEqualTo(value)
    }

    @Test
    fun `GIVEN stored value WHEN get THEN flow emits it`() = runTest {
        // Arrange
        val value = listOf(mockk<PaymentMethodDTO>())
        store.store(key = "k", value = value)

        // Assert
        assertThat(store.get(key = "k").first()).isEqualTo(value)
    }

    @Test
    fun `GIVEN empty store WHEN contains THEN returns false`() = runTest {
        assertThat(store.contains(key = "k")).isFalse()
    }

    @Test
    fun `GIVEN stored value WHEN contains THEN returns true`() = runTest {
        // Arrange
        store.store(key = "k", value = listOf(mockk<PaymentMethodDTO>()))

        // Assert
        assertThat(store.contains(key = "k")).isTrue()
    }

    @Test
    fun `GIVEN stored value WHEN clear THEN store is empty`() = runTest {
        // Arrange
        store.store(key = "k", value = listOf(mockk<PaymentMethodDTO>()))

        // Act
        store.clear()

        // Assert
        assertThat(store.getSyncOrNull(key = "k")).isNull()
        assertThat(store.contains(key = "k")).isFalse()
    }
}