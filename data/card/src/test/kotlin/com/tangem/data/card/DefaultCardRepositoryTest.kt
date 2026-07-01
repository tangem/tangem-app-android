package com.tangem.data.card

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.card.UsedCardInfo
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.datasource.local.preferences.utils.storeObjectList
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Tests for [DefaultCardRepository].
 *
 * Uses a real [AppPreferencesStore] backed by an in-memory [MockStateDataStore] and a real [Moshi]
 * instance, so the JSON round-trip through preferences is exercised end-to-end.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultCardRepositoryTest {

    // Only the in-memory store's content is mutable, so it is the single thing reset per test.
    private val dataStore = MockStateDataStore(default = emptyPreferences())
    private val appPreferencesStore = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = dataStore,
    )
    private val repository = DefaultCardRepository(appPreferencesStore)

    @BeforeEach
    fun resetStore() {
        runBlocking { dataStore.updateData { emptyPreferences() } }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WasCardScanned {

        @Test
        fun `GIVEN card present WHEN wasCardScanned THEN emits true`() = runTest {
            // Arrange
            seedCards(UsedCardInfo(cardId = CARD_ID))

            // Act
            val result = repository.wasCardScanned(CARD_ID).first()

            // Assert
            assertThat(result).isTrue()
        }

        @Test
        fun `GIVEN card absent WHEN wasCardScanned THEN emits false`() = runTest {
            // Act
            val result = repository.wasCardScanned(CARD_ID).first()

            // Assert
            assertThat(result).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SetCardWasScanned {

        @Test
        fun `GIVEN empty store WHEN setCardWasScanned THEN creates entry with isScanned true`() = runTest {
            // Act
            repository.setCardWasScanned(CARD_ID)

            // Assert
            assertThat(storedCards()).containsExactly(UsedCardInfo(cardId = CARD_ID, isScanned = true))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StartCardActivation {

        @Test
        fun `GIVEN empty store WHEN startCardActivation THEN creates entry with isActivationStarted true`() = runTest {
            // Act
            repository.startCardActivation(CARD_ID)

            // Assert
            assertThat(storedCards()).containsExactly(UsedCardInfo(cardId = CARD_ID, isActivationStarted = true))
        }

        @Test
        fun `GIVEN other cards present WHEN editing one card THEN others are preserved`() = runTest {
            // Arrange
            val other = UsedCardInfo(cardId = OTHER_CARD_ID, isScanned = true)
            seedCards(other)

            // Act
            repository.startCardActivation(CARD_ID)

            // Assert
            assertThat(storedCards()).containsExactly(
                other,
                UsedCardInfo(cardId = CARD_ID, isActivationStarted = true),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FinishCardActivation {

        @Test
        fun `GIVEN empty store WHEN finishCardActivation with backup error THEN entry marked finished with error`() =
            runTest {
                // Act
                repository.finishCardActivation(cardId = CARD_ID, hasBackupError = true)

                // Assert
                assertThat(storedCards()).containsExactly(
                    UsedCardInfo(
                        cardId = CARD_ID,
                        isActivationStarted = true,
                        isActivationFinished = true,
                        hasBackupError = true,
                    ),
                )
            }

        @Test
        fun `GIVEN empty store WHEN finishCardActivation without backup error THEN entry marked finished without error`() =
            runTest {
                // Act
                repository.finishCardActivation(cardId = CARD_ID, hasBackupError = false)

                // Assert
                assertThat(storedCards()).containsExactly(
                    UsedCardInfo(
                        cardId = CARD_ID,
                        isActivationStarted = true,
                        isActivationFinished = true,
                        hasBackupError = false,
                    ),
                )
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsActivationStarted {

        @Test
        fun `GIVEN activation started WHEN isActivationStarted THEN true`() = runTest {
            // Arrange
            seedCards(UsedCardInfo(cardId = CARD_ID, isActivationStarted = true))

            // Act & Assert
            assertThat(repository.isActivationStarted(CARD_ID)).isTrue()
        }

        @Test
        fun `GIVEN card absent WHEN isActivationStarted THEN false`() = runTest {
            assertThat(repository.isActivationStarted(CARD_ID)).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsActivationFinished {

        @Test
        fun `GIVEN activation finished WHEN isActivationFinished THEN true`() = runTest {
            // Arrange
            seedCards(UsedCardInfo(cardId = CARD_ID, isActivationFinished = true))

            // Act & Assert
            assertThat(repository.isActivationFinished(CARD_ID)).isTrue()
        }

        @Test
        fun `GIVEN card absent WHEN isActivationFinished THEN false`() = runTest {
            assertThat(repository.isActivationFinished(CARD_ID)).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsActivationInProgress {

        @ParameterizedTest
        @ProvideTestModels
        fun isActivationInProgress(model: ActivationInProgressModel) = runTest {
            // Arrange
            model.stored?.let { seedCards(it) }

            // Act
            val result = repository.isActivationInProgress(CARD_ID)

            // Assert
            assertThat(result).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ActivationInProgressModel(stored = null, expected = false),
            ActivationInProgressModel(
                stored = UsedCardInfo(cardId = CARD_ID, isActivationStarted = false, isActivationFinished = false),
                expected = false,
            ),
            ActivationInProgressModel(
                stored = UsedCardInfo(cardId = CARD_ID, isActivationStarted = true, isActivationFinished = false),
                expected = true,
            ),
            ActivationInProgressModel(
                stored = UsedCardInfo(cardId = CARD_ID, isActivationStarted = true, isActivationFinished = true),
                expected = false,
            ),
            ActivationInProgressModel(
                stored = UsedCardInfo(cardId = CARD_ID, isActivationStarted = false, isActivationFinished = true),
                expected = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HasBackupError {

        @Test
        fun `GIVEN backup error WHEN hasBackupError THEN true`() = runTest {
            // Arrange
            seedCards(UsedCardInfo(cardId = CARD_ID, hasBackupError = true))

            // Act & Assert
            assertThat(repository.hasBackupError(CARD_ID)).isTrue()
        }

        @Test
        fun `GIVEN no backup error WHEN hasBackupError THEN false`() = runTest {
            // Arrange
            seedCards(UsedCardInfo(cardId = CARD_ID, hasBackupError = false))

            // Act & Assert
            assertThat(repository.hasBackupError(CARD_ID)).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TangemTos {

        @Test
        fun `GIVEN nothing stored WHEN isTangemTOSAccepted THEN false by default`() = runTest {
            assertThat(repository.isTangemTOSAccepted()).isFalse()
        }

        @Test
        fun `GIVEN TOS accepted WHEN isTangemTOSAccepted THEN true`() = runTest {
            // Arrange
            repository.acceptTangemTOS()

            // Act & Assert
            assertThat(repository.isTangemTOSAccepted()).isTrue()
        }
    }

    private suspend fun seedCards(vararg cards: UsedCardInfo) {
        appPreferencesStore.storeObjectList(key = PreferencesKeys.USED_CARDS_INFO_KEY, value = cards.toList())
    }

    private suspend fun storedCards(): List<UsedCardInfo> {
        return appPreferencesStore.getObjectListSync(key = PreferencesKeys.USED_CARDS_INFO_KEY)
    }

    internal data class ActivationInProgressModel(val stored: UsedCardInfo?, val expected: Boolean)

    private companion object {
        const val CARD_ID = "card-1"
        const val OTHER_CARD_ID = "card-2"
    }
}