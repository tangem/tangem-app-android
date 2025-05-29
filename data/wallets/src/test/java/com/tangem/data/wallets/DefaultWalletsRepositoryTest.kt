package com.tangem.data.wallets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.WalletResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.domain.wallets.models.UserWallet
import org.junit.Before
import org.junit.Test

class DefaultWalletsRepositoryTest {
    private lateinit var repository: DefaultWalletsRepository
    private val preferencesDataStore = mockk<DataStore<Preferences>>(relaxed = true)
    private val appPreferenceStore = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = preferencesDataStore,
    )
    private lateinit var tangemTechApi: TangemTechApi
    private lateinit var dispatchers: CoroutineDispatcherProvider

    private val testWalletId = UserWalletId("1234567890abcdef")

    @Before
    fun setup() {
        tangemTechApi = mockk()
        dispatchers = TestingCoroutineDispatcherProvider()
        repository = DefaultWalletsRepository(
            appPreferencesStore = appPreferenceStore,
            tangemTechApi = tangemTechApi,
            userWalletsStore = mockk(),
            seedPhraseNotificationVisibilityStore = mockk(),
            dispatchers = dispatchers,
            authProvider = mockk(),
        )
    }

    @Test
    fun `GIVEN local storage has value WHEN isNotificationsEnabled THEN should return local value`() = runTest {
        // GIVEN
        val expectedPreferences = """{"${testWalletId.stringValue}":true}"""
        val preferences = mockk<Preferences>()
        coEvery { preferencesDataStore.data } returns flowOf(preferences)
        coEvery { preferences[PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] } returns expectedPreferences

        // WHEN
        val result = repository.isNotificationsEnabled(testWalletId)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN local storage is empty WHEN isNotificationsEnabled THEN should return false`() = runTest {
        // GIVEN
        val preferences = mockk<Preferences>()
        coEvery { preferencesDataStore.data } returns flowOf(preferences)
        coEvery { preferences[PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] } returns "{}"

        // WHEN
        val result = repository.isNotificationsEnabled(testWalletId)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN enabled status WHEN setNotificationsEnabled THEN should update local storage`() = runTest {
        // GIVEN
        val preferences = mockk<Preferences>()
        coEvery { preferencesDataStore.data } returns flowOf(preferences)
        coEvery { preferences[PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] } returns "{}"
        coEvery { preferencesDataStore.updateData(any()) } returns mockk()

        // WHEN
        repository.setNotificationsEnabled(testWalletId, isEnabled = true)

        // THEN
        coVerify(exactly = 1) { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN disabled status WHEN setNotificationsEnabled THEN should update local storage`() = runTest {
        // GIVEN
        val preferences = mockk<Preferences>()
        coEvery { preferencesDataStore.data } returns flowOf(preferences)
        coEvery { preferences[PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] } returns "{}"
        coEvery { preferencesDataStore.updateData(any()) } returns mockk()

        // WHEN
        repository.setNotificationsEnabled(testWalletId, isEnabled = false)

        // THEN
        coVerify(exactly = 1) { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN API returns wallets WHEN getWalletsInfo THEN should return converted wallets and update cache if requested`() = runTest {
        // GIVEN
        val applicationId = "test_app_id"
        val wallet1Id = "1234567890abcdef"
        val wallet2Id = "fedcba0987654321"
        val walletResponses = listOf(
            WalletResponse(
                id = wallet1Id,
                notifyStatus = true,
            ),
            WalletResponse(
                id = wallet2Id,
                notifyStatus = false,
            ),
        )
        coEvery { tangemTechApi.getWallets(applicationId) } returns ApiResponse.Success(walletResponses)
        coEvery { preferencesDataStore.updateData(any()) } returns mockk()

        // WHEN
        val result = repository.getWalletsInfo(applicationId, updateCache = true)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result[0].walletId.stringValue).isEqualTo(wallet1Id)
        assertThat(result[0].isNotificationsEnabled).isTrue()
        assertThat(result[1].walletId.stringValue).isEqualTo(wallet2Id)
        assertThat(result[1].isNotificationsEnabled).isFalse()

        coVerify(exactly = 1) { tangemTechApi.getWallets(applicationId) }
        coVerify(exactly = 2) { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN API returns wallets WHEN getWalletsInfo with updateCache false THEN should return converted wallets without updating cache`() = runTest {
        // GIVEN
        val applicationId = "test_app_id"
        val wallet1Id = "1234567890abcdef"
        val walletResponses = listOf(
            WalletResponse(
                id = wallet1Id,
                notifyStatus = true,
            ),
        )
        coEvery { tangemTechApi.getWallets(applicationId) } returns ApiResponse.Success(walletResponses)

        // WHEN
        val result = repository.getWalletsInfo(applicationId, updateCache = false)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].walletId.stringValue).isEqualTo(wallet1Id)
        assertThat(result[0].isNotificationsEnabled).isTrue()

        coVerify(exactly = 1) { tangemTechApi.getWallets(applicationId) }
        coVerify(exactly = 0) { preferencesDataStore.updateData(any()) }
    }

    @Test
    fun `GIVEN user wallets and application ID WHEN associateWallets THEN should convert and send to API`() = runTest {
        // GIVEN
        val applicationId = "test_app_id"
        val wallet1Id = "1234567890abcdef"
        val wallet2Id = "fedcba0987654321"
        val card1PublicKey = "card1_public_key"
        val card2PublicKey = "card2_public_key"

        val userWallets = listOf(
            mockk<UserWallet> {
                every { cardsInWallet } returns setOf(card1PublicKey)
                every { walletId } returns UserWalletId(wallet1Id)
                every { name } returns "Wallet 1"
            },
            mockk<UserWallet> {
                every { cardsInWallet } returns setOf(card2PublicKey)
                every { walletId } returns UserWalletId(wallet2Id)
                every { name } returns "Wallet 2"
            },
        )

        val publicKeys = mapOf(
            card1PublicKey to "public_key_1",
            card2PublicKey to "public_key_2",
        )

        val authProvider = mockk<AuthProvider> {
            every { getCardsPublicKeys() } returns publicKeys
        }

        repository = DefaultWalletsRepository(
            appPreferencesStore = appPreferenceStore,
            tangemTechApi = tangemTechApi,
            userWalletsStore = mockk(),
            seedPhraseNotificationVisibilityStore = mockk(),
            dispatchers = dispatchers,
            authProvider = authProvider,
        )

        coEvery {
            tangemTechApi.associateApplicationIdWithWallets(
                eq(applicationId),
                any(),
            )
        } returns ApiResponse.Success(Unit)

        // WHEN
        repository.associateWallets(applicationId, userWallets)

        // THEN
        coVerify(exactly = 1) {
            tangemTechApi.associateApplicationIdWithWallets(
                eq(applicationId),
                match { body ->
                    body.size == 2 &&
                        body.any {
                            it.walletId == wallet1Id && it.cards.any { card -> card.cardPublicKey == "public_key_1" } &&
                                it.name == "Wallet 1"
                        } &&
                        body.any {
                            it.walletId == wallet2Id && it.cards.any { card -> card.cardPublicKey == "public_key_2" } &&
                                it.name == "Wallet 2"
                        }
                },
            )
        }
    }
}