package com.tangem.datasource.local.appcurrency

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.local.appcurrency.implementation.DefaultAvailableAppCurrenciesStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultAvailableAppCurrenciesStoreTest {

    private val store = DefaultAvailableAppCurrenciesStore(store = RuntimeSharedMapStore())

    @Test
    fun `GIVEN empty store WHEN getSyncOrNull THEN returns null`() = runTest {
        assertThat(store.getSyncOrNull(key = "BTC")).isNull()
        assertThat(store.getAllSyncOrNull()).isNull()
    }

    @Test
    fun `GIVEN stored response WHEN getSyncOrNull by code THEN returns currency`() = runTest {
        // Arrange
        store.store(response = response(currency(id = "bitcoin", code = "BTC")))

        // Act
        val actual = store.getSyncOrNull(key = "BTC")

        // Assert
        assertThat(actual?.id).isEqualTo("bitcoin")
    }

    @Test
    fun `GIVEN stored response WHEN getAllSyncOrNull THEN returns all currencies`() = runTest {
        // Arrange
        store.store(
            response = response(
                currency(id = "bitcoin", code = "BTC"),
                currency(id = "ethereum", code = "ETH"),
            ),
        )

        // Assert
        assertThat(store.getAllSyncOrNull()).hasSize(2)
    }

    @Test
    fun `GIVEN imageHost WHEN store response THEN icon urls are formatted from id`() = runTest {
        // Arrange
        store.store(
            response = response(currency(id = "bitcoin", code = "BTC")).copy(imageHost = "https://host/"),
        )

        // Act
        val actual = store.getSyncOrNull(key = "BTC")

        // Assert
        assertThat(actual?.iconSmallUrl).isEqualTo("https://host/small/bitcoin.png")
        assertThat(actual?.iconMediumUrl).isEqualTo("https://host/medium/bitcoin.png")
    }

    private fun response(vararg currencies: CurrenciesResponse.Currency) = CurrenciesResponse(
        currencies = currencies.toList(),
        imageHost = null,
    )

    private fun currency(id: String, code: String) = CurrenciesResponse.Currency(
        id = id,
        code = code,
        name = id,
        rateBTC = "1",
        unit = "$",
        type = "fiat",
    )
}