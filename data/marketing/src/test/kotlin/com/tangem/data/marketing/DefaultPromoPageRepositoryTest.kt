package com.tangem.data.marketing

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultPromoPageRepositoryTest {

    private val repository = DefaultPromoPageRepository(
        appPreferencesStore = createAppPreferencesStore(
            dispatchers = TestingCoroutineDispatcherProvider(),
            preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
        ),
    )

    @Test
    fun `GIVEN two saved pages WHEN observePage THEN the last one is kept`() = runTest {
        // Arrange
        repository.setPage(FIRST_PAGE)
        repository.setPage(SECOND_PAGE)

        // Act
        val actual = repository.observePage().first()

        // Assert
        assertThat(actual).isEqualTo(SECOND_PAGE)
    }

    @Test
    fun `GIVEN kept page WHEN setPage with null THEN the page is dropped`() = runTest {
        // Arrange
        repository.setPage(FIRST_PAGE)

        // Act
        repository.setPage(null)

        // Assert
        assertThat(repository.observePage().first()).isNull()
    }

    @Test
    fun `GIVEN kept page WHEN consumePage twice THEN the page is returned once and dropped`() = runTest {
        // Arrange
        repository.setPage(FIRST_PAGE)

        // Act
        val first = repository.consumePage()
        val second = repository.consumePage()

        // Assert
        assertThat(listOf(first, second)).containsExactly(FIRST_PAGE, null).inOrder()
        assertThat(repository.observePage().first()).isNull()
    }

    private companion object {
        const val FIRST_PAGE = "https://promo.tangem.com/first/"
        const val SECOND_PAGE = "https://promo.tangem.com/second/"
    }
}