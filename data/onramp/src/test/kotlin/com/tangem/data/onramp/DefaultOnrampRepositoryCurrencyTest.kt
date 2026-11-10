package com.tangem.data.onramp

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultOnrampRepositoryCurrencyTest {

    private val moshi = Moshi.Builder().add(BigDecimalAdapter()).build()
    private val repository = DefaultOnrampRepository(
        onrampApi = mockk(relaxed = true),
        expressApi = mockk(relaxed = true),
        dispatchers = TestingCoroutineDispatcherProvider(),
        appPreferencesStore = createAppPreferencesStore(
            moshi = moshi,
            dispatchers = TestingCoroutineDispatcherProvider(),
            preferencesDataStore = MockStateDataStore<Preferences>(default = emptyPreferences()),
        ),
        growPreferencesKeys = GrowPreferencesKeys(),
        expressUtils = mockk(relaxed = true),
        paymentMethodsStore = mockk(relaxed = true),
        onrampCurrentCountryByIPStore = mockk(relaxed = true),
        pairsStore = mockk(relaxed = true),
        quotesStore = mockk(relaxed = true),
        countriesStore = mockk(relaxed = true),
        currenciesStore = mockk(relaxed = true),
        walletManagersFacade = mockk(relaxed = true),
        dataSignatureVerifier = mockk(relaxed = true),
        expressHistoryDao = mockk(relaxed = true),
        expressHistoryRepository = mockk(relaxed = true),
        moshi = moshi,
    )

    @Test
    fun `GIVEN country saved by the old flow WHEN country cleared THEN its currency is not promoted`() = runTest {
        // Arrange
        repository.saveDefaultCountry(createCountry(EUR))

        // Act
        repository.clearDefaultCountry()

        // Assert
        assertThat(repository.getSelectedCurrencySync()).isNull()
        assertThat(repository.getDefaultCountrySync()).isNull()
    }

    @Test
    fun `GIVEN user selected currency WHEN country cleared THEN selected currency kept`() = runTest {
        // Arrange
        repository.saveDefaultCountry(createCountry(EUR))
        repository.saveSelectedCurrency(USD)

        // Act
        repository.clearDefaultCountry()

        // Assert
        assertThat(repository.getSelectedCurrencySync()).isEqualTo(USD)
    }

    @Test
    fun `GIVEN automatic default currency WHEN saved THEN it is not treated as user selection`() = runTest {
        // Arrange
        repository.saveDefaultCountry(createCountry(EUR))

        // Act
        repository.saveDefaultCurrency(USD)

        // Assert
        assertThat(repository.getSelectedCurrencySync()).isNull()
        assertThat(repository.getDefaultCurrencySync()).isEqualTo(USD)
    }

    private fun createCountry(currency: OnrampCurrency) = OnrampCountry(
        id = "ip",
        name = "Country",
        code = "CC",
        image = "https://flag.png",
        alpha3 = "CCC",
        continent = "Europe",
        defaultCurrency = currency,
        isOnrampAvailable = true,
    )

    private class BigDecimalAdapter {
        @ToJson
        fun toJson(value: BigDecimal): String = value.toPlainString()

        @FromJson
        fun fromJson(value: String): BigDecimal = BigDecimal(value)
    }

    private companion object {
        val EUR = OnrampCurrency(name = "Euro", code = "EUR", image = null, precision = 2, unit = "€")
        val USD = OnrampCurrency(name = "US Dollar", code = "USD", image = null, precision = 2, unit = "$")
    }
}