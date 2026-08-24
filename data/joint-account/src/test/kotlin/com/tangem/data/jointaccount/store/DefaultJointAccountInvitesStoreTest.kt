package com.tangem.data.jointaccount.store

import com.google.common.truth.Truth.assertThat
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.logging.Severity
import com.tangem.utils.logging.TangemLogger
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultJointAccountInvitesStoreTest {

    private val secureStorage: SecureStorage = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }

    private val store = DefaultJointAccountInvitesStore(
        secureStorage = secureStorage,
        json = json,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(secureStorage)
    }

    @AfterEach
    fun resetLogWriters() {
        TangemLogger.setLogWriters(emptyList())
    }

    @Test
    fun `GIVEN empty storage WHEN store THEN entry keyed by the wallet holds the account invites`() = runTest {
        // Arrange
        val storage = InMemorySecureStorage()
        val store = createStore(storage)

        // Act
        store.store(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID, invites = INVITES)

        // Assert
        assertThat(storage.entries.keys).containsExactly(EXPECTED_KEY)
        assertThat(store.get(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID)).isEqualTo(INVITES)
    }

    @Test
    fun `GIVEN stored account WHEN store another THEN both accounts kept under one wallet entry`() = runTest {
        // Arrange
        val storage = InMemorySecureStorage()
        val store = createStore(storage)
        store.store(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID, invites = INVITES)

        // Act
        store.store(userWalletId = USER_WALLET_ID, cryptoAccountId = OTHER_ACCOUNT_ID, invites = OTHER_INVITES)

        // Assert
        assertThat(storage.entries.keys).containsExactly(EXPECTED_KEY)
        assertThat(store.get(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID)).isEqualTo(INVITES)
        assertThat(store.get(userWalletId = USER_WALLET_ID, cryptoAccountId = OTHER_ACCOUNT_ID))
            .isEqualTo(OTHER_INVITES)
    }

    @Test
    fun `GIVEN stored account WHEN store it again THEN invites replaced`() = runTest {
        // Arrange
        val store = createStore(InMemorySecureStorage())
        store.store(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID, invites = INVITES)

        // Act
        store.store(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID, invites = OTHER_INVITES)

        // Assert
        assertThat(store.get(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID)).isEqualTo(OTHER_INVITES)
    }

    @Test
    fun `GIVEN no entry WHEN get THEN returns null`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns null

        // Act
        val actual = store.get(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN entry without the account WHEN get THEN returns null`() = runTest {
        // Arrange
        val store = createStore(InMemorySecureStorage())
        store.store(userWalletId = USER_WALLET_ID, cryptoAccountId = OTHER_ACCOUNT_ID, invites = OTHER_INVITES)

        // Act
        val actual = store.get(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN corrupted payload WHEN get THEN returns null and drops the entry`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns "{not a json"

        // Act
        val actual = store.get(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID)

        // Assert
        assertThat(actual).isNull()
        verify(exactly = 1) { secureStorage.delete(EXPECTED_KEY) }
    }

    @Test
    fun `GIVEN corrupted payload WHEN get THEN nothing from the payload reaches the logs`() = runTest {
        // Arrange
        val logs = RecordingLogWriter()
        TangemLogger.setLogWriters(listOf(logs))
        every { secureStorage.getAsString(EXPECTED_KEY) } returns """{"$ACCOUNT_ID":["${INVITES.first()}"],,,"""

        // Act
        store.get(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID)

        // Assert
        assertThat(logs.entries).isNotEmpty()
        assertThat(logs.entries.map { it.second }).containsExactly(null)
        assertThat(logs.entries.none { it.first.contains(INVITES.first()) }).isTrue()
    }

    @Test
    fun `GIVEN two stored accounts WHEN clear one THEN the other stays`() = runTest {
        // Arrange
        val store = createStore(InMemorySecureStorage())
        store.store(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID, invites = INVITES)
        store.store(userWalletId = USER_WALLET_ID, cryptoAccountId = OTHER_ACCOUNT_ID, invites = OTHER_INVITES)

        // Act
        store.clear(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID)

        // Assert
        assertThat(store.get(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID)).isNull()
        assertThat(store.get(userWalletId = USER_WALLET_ID, cryptoAccountId = OTHER_ACCOUNT_ID))
            .isEqualTo(OTHER_INVITES)
    }

    @Test
    fun `GIVEN the only stored account WHEN clear it THEN the wallet entry is deleted`() = runTest {
        // Arrange
        val storage = InMemorySecureStorage()
        val store = createStore(storage)
        store.store(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID, invites = INVITES)

        // Act
        store.clear(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID)

        // Assert
        assertThat(storage.entries).isEmpty()
    }

    @Test
    fun `GIVEN stored accounts WHEN clearAll THEN the wallet entry is deleted`() = runTest {
        // Act
        store.clearAll(userWalletId = USER_WALLET_ID)

        // Assert
        verify(exactly = 1) { secureStorage.delete(EXPECTED_KEY) }
    }

    private fun createStore(storage: SecureStorage): DefaultJointAccountInvitesStore {
        return DefaultJointAccountInvitesStore(
            secureStorage = storage,
            json = json,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }

    private class InMemorySecureStorage : SecureStorage {

        val entries = mutableMapOf<String, String>()

        override fun store(key: String, value: String) {
            entries[key] = value
        }

        override fun store(data: ByteArray, account: String) {
            entries[account] = data.decodeToString()
        }

        override fun storeKey(key: ByteArray, account: String) = store(key, account)

        override fun getAsString(key: String): String? = entries[key]

        override fun get(account: String): ByteArray? = entries[account]?.encodeToByteArray()

        override fun delete(account: String) {
            entries.remove(account)
        }
    }

    private class RecordingLogWriter : TangemLogger.LogWriter {

        val entries = mutableListOf<Pair<String, Throwable?>>()

        override fun write(
            severity: Severity,
            tag: String,
            message: String,
            throwable: Throwable?,
            shouldSanitize: Boolean,
        ) {
            entries += message to throwable
        }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("FE7F9D116CF285B694715DAE477AF6DC1CFCA02DBD9DCDA2EE2AF523A93E920F")
        const val EXPECTED_KEY =
            "joint_account_invites_FE7F9D116CF285B694715DAE477AF6DC1CFCA02DBD9DCDA2EE2AF523A93E920F"

        const val ACCOUNT_ID = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"
        const val OTHER_ACCOUNT_ID = "A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A64B2F1C8A9E7D6053"

        val INVITES = listOf(
            "1111111111111111111111111111111111111111111111111111111111111111",
            "2222222222222222222222222222222222222222222222222222222222222222",
        )
        val OTHER_INVITES = listOf(
            "3333333333333333333333333333333333333333333333333333333333333333",
        )
    }
}