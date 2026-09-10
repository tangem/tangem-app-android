package com.tangem.datasource.local.markets

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.store.datasource.markets.models.response.CoinCategoriesResponse.Category
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultCoinCategoriesSnapshotStoreTest {

    private val appPreferencesStore = createAppPreferencesStore(
        moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
    )

    private val store = DefaultCoinCategoriesSnapshotStore(appPreferencesStore)

    @Test
    fun `GIVEN nothing stored WHEN read THEN returns null`() = runTest {
        // Act
        val actual = store.getSyncOrNull(country = "US", environment = "PROD")

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN snapshot stored WHEN read with the same stamps THEN returns categories`() = runTest {
        // Arrange
        val categories = listOf(category(code = "commodities"))
        store.store(country = "US", environment = "PROD", categories = categories)

        // Act
        val actual = store.getSyncOrNull(country = "US", environment = "PROD")

        // Assert
        assertThat(actual).isEqualTo(categories)
    }

    @Test
    fun `GIVEN snapshot stored WHEN read for another country THEN returns null`() = runTest {
        // Arrange
        store.store(country = "US", environment = "PROD", categories = listOf(category(code = "commodities")))

        // Act
        val actual = store.getSyncOrNull(country = "DE", environment = "PROD")

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN snapshot stored WHEN read for another environment THEN returns null`() = runTest {
        // Arrange
        store.store(country = "US", environment = "PROD", categories = listOf(category(code = "commodities")))

        // Act
        val actual = store.getSyncOrNull(country = "US", environment = "DEV")

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN snapshot stored WHEN a newer one is stored THEN the newer one is read back`() = runTest {
        // Arrange
        store.store(country = "US", environment = "PROD", categories = listOf(category(code = "commodities")))
        val newer = listOf(category(code = "bonds"))

        // Act
        store.store(country = "DE", environment = "PROD", categories = newer)

        // Assert
        assertThat(store.getSyncOrNull(country = "DE", environment = "PROD")).isEqualTo(newer)
        assertThat(store.getSyncOrNull(country = "US", environment = "PROD")).isNull()
    }

    private fun category(code: String) = Category(
        id = "1",
        code = code,
        displayName = code.replaceFirstChar(Char::uppercase),
        displayOrder = 0,
        tokensCount = 17,
        isRestricted = false,
        isVisible = true,
        sectors = emptyList(),
    )
}