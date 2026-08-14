package com.tangem.datasource.local.onramp.country

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.onramp.model.OnrampCountry
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultOnrampCurrentCountryByIPStoreTest {

    private val store = DefaultOnrampCurrentCountryByIPStore(store = RuntimeSharedStore())

    @Test
    fun `GIVEN empty store WHEN getSyncOrNull THEN returns null`() = runTest {
        assertThat(store.getSyncOrNull()).isNull()
    }

    @Test
    fun `GIVEN stored value WHEN getSyncOrNull THEN returns it`() = runTest {
        // Arrange
        val value = mockk<OnrampCountry>()
        store.store(value = value)

        // Assert
        assertThat(store.getSyncOrNull()).isEqualTo(value)
    }

    @Test
    fun `GIVEN value stored twice WHEN getSyncOrNull THEN returns the latest`() = runTest {
        // Arrange
        val first = mockk<OnrampCountry>()
        val second = mockk<OnrampCountry>()
        store.store(value = first)
        store.store(value = second)

        // Assert
        assertThat(store.getSyncOrNull()).isEqualTo(second)
    }
}