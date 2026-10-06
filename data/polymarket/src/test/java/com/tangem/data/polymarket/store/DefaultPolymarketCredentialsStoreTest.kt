package com.tangem.data.polymarket.store

import com.google.common.truth.Truth.assertThat
import com.tangem.common.services.secure.SecureStorage
import com.tangem.data.polymarket.converter.PolymarketApiCredentialsConverter
import com.tangem.data.polymarket.entity.PolymarketApiCredentialsDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.logging.Severity
import com.tangem.utils.logging.TangemLogger
import app.cash.turbine.test
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketCredentialsStoreTest {

    private val secureStorage: SecureStorage = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }

    private val store = DefaultPolymarketCredentialsStore(
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
    fun `GIVEN credentials WHEN store THEN saved as json under the wallet key`() = runTest {
        // Arrange
        val payload = slot<String>()
        every { secureStorage.store(eq(EXPECTED_KEY), capture(payload)) } returns Unit

        // Act
        store.store(userWalletId = USER_WALLET_ID, credentials = CREDENTIALS)

        // Assert
        verify(exactly = 1) { secureStorage.store(EXPECTED_KEY, any()) }
        assertThat(decode(payload.captured)).isEqualTo(CREDENTIALS)
    }

    @Test
    fun `GIVEN credentials WHEN store THEN payload keeps the persisted field names`() = runTest {
        // Arrange
        val payload = slot<String>()
        every { secureStorage.store(eq(EXPECTED_KEY), capture(payload)) } returns Unit

        // Act
        store.store(userWalletId = USER_WALLET_ID, credentials = CREDENTIALS)

        // Assert
        assertThat(json.parseToJsonElement(payload.captured).jsonObject.keys)
            .containsExactly("apiKey", "secret", "passphrase")
    }

    @Test
    fun `GIVEN stored json WHEN get THEN returns deserialized credentials`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns encode(CREDENTIALS)

        // Act
        val actual = store.get(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS)
    }

    @Test
    fun `GIVEN no entry WHEN get THEN returns null`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns null

        // Act
        val actual = store.get(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN corrupted json WHEN get THEN returns null and drops the entry`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns "{not a json"

        // Act
        val actual = store.get(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
        verify(exactly = 1) { secureStorage.delete(EXPECTED_KEY) }
    }

    @Test
    fun `GIVEN corrupted json WHEN get THEN nothing from the payload reaches the logs`() = runTest {
        // Arrange
        val logs = RecordingLogWriter()
        TangemLogger.setLogWriters(listOf(logs))
        every { secureStorage.getAsString(EXPECTED_KEY) } returns """{"secret":"${CREDENTIALS.secret}",,,"""

        // Act
        store.get(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(logs.entries).isNotEmpty()
        assertThat(logs.entries.map { it.second }).containsExactly(null)
        assertThat(logs.entries.none { it.first.contains(CREDENTIALS.secret) }).isTrue()
    }

    @Test
    fun `GIVEN a stored entry WHEN clear THEN the key is deleted`() = runTest {
        // Act
        store.clear(userWalletId = USER_WALLET_ID)

        // Assert
        verify(exactly = 1) { secureStorage.delete(EXPECTED_KEY) }
    }

    private fun encode(credentials: PolymarketApiCredentials): String =
        json.encodeToString(
            PolymarketApiCredentialsDTO.serializer(),
            PolymarketApiCredentialsConverter.convert(credentials),
        )

    private fun decode(payload: String): PolymarketApiCredentials =
        PolymarketApiCredentialsConverter.convertBack(
            json.decodeFromString(PolymarketApiCredentialsDTO.serializer(), payload),
        )

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

    @Test
    fun `GIVEN in-memory storage WHEN store THEN the entry is keyed by the wallet and holds the credentials`() = runTest {
        // Arrange
        val storage = InMemorySecureStorage()
        val storeOverRealStorage = createStore(secureStorage = storage)

        // Act
        storeOverRealStorage.store(userWalletId = USER_WALLET_ID, credentials = CREDENTIALS)

        // Assert
        assertThat(storage.entries.keys).containsExactly(EXPECTED_KEY)
        assertThat(storeOverRealStorage.get(userWalletId = USER_WALLET_ID)).isEqualTo(CREDENTIALS)
    }

    @Test
    fun `GIVEN stored credentials WHEN observed THEN the first value is present`() = runTest {
        // Arrange
        val storage = InMemorySecureStorage()
        storage.entries[EXPECTED_KEY] = encode(CREDENTIALS)

        // Act
        val actual = createStore(secureStorage = storage).hasCredentials(userWalletId = USER_WALLET_ID).first()

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN nothing stored WHEN observed THEN the first value is absent`() = runTest {
        // Act
        val actual = createStore(secureStorage = InMemorySecureStorage())
            .hasCredentials(userWalletId = USER_WALLET_ID)
            .first()

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN an observer WHEN credentials are stored and cleared THEN each change of this wallet is emitted once`() =
        runTest {
            // Arrange
            val credentialsStore = createStore(secureStorage = InMemorySecureStorage())

            credentialsStore.hasCredentials(userWalletId = USER_WALLET_ID).test {
                assertThat(awaitItem()).isFalse()

                // Act
                credentialsStore.store(userWalletId = USER_WALLET_ID, credentials = CREDENTIALS)
                credentialsStore.store(userWalletId = OTHER_WALLET_ID, credentials = CREDENTIALS)
                credentialsStore.clear(userWalletId = USER_WALLET_ID)

                // Assert
                assertThat(awaitItem()).isTrue()
                assertThat(awaitItem()).isFalse()
                expectNoEvents()
            }
        }

    @Test
    fun `GIVEN corrupted json WHEN observed THEN the credentials are reported absent`() = runTest {
        // Arrange
        val storage = InMemorySecureStorage()
        storage.entries[EXPECTED_KEY] = "{not a json"

        // Act
        val actual = createStore(secureStorage = storage).hasCredentials(userWalletId = USER_WALLET_ID).first()

        // Assert
        assertThat(actual).isFalse()
        assertThat(storage.entries).isEmpty()
    }

    @Test
    fun `GIVEN an unreadable storage WHEN observed THEN the credentials are reported absent and later changes still arrive`() =
        runTest {
            // Arrange
            val storage = InMemorySecureStorage()
            storage.failReads = true
            val credentialsStore = createStore(secureStorage = storage)

            credentialsStore.hasCredentials(userWalletId = USER_WALLET_ID).test {
                assertThat(awaitItem()).isFalse()

                // Act
                storage.failReads = false
                credentialsStore.store(userWalletId = USER_WALLET_ID, credentials = CREDENTIALS)

                // Assert
                assertThat(awaitItem()).isTrue()
            }
        }

    private fun createStore(secureStorage: SecureStorage) = DefaultPolymarketCredentialsStore(
        secureStorage = secureStorage,
        json = json,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private class InMemorySecureStorage : SecureStorage {

        val entries = mutableMapOf<String, String>()
        var failReads = false

        override fun store(key: String, value: String) {
            entries[key] = value
        }

        override fun store(data: ByteArray, account: String) {
            entries[account] = data.decodeToString()
        }

        override fun storeKey(key: ByteArray, account: String) = store(key, account)

        override fun getAsString(key: String): String? {
            if (failReads) throw IllegalStateException("Keystore is unavailable")
            return entries[key]
        }

        override fun get(account: String): ByteArray? = entries[account]?.encodeToByteArray()

        override fun delete(account: String) {
            entries.remove(account)
        }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("FE7F9D116CF285B694715DAE477AF6DC1CFCA02DBD9DCDA2EE2AF523A93E920F")
        val OTHER_WALLET_ID = UserWalletId("0A")
        const val EXPECTED_KEY =
            "polymarket_api_credentials_FE7F9D116CF285B694715DAE477AF6DC1CFCA02DBD9DCDA2EE2AF523A93E920F"

        val CREDENTIALS = PolymarketApiCredentials(
            apiKey = "df2b7b32-a2e6-4a3f-9b1c-0f0e5f5f0000",
            secret = "c2VjcmV0LWJ5dGVzLWJhc2U2NA==",
            passphrase = "passphrase-value",
        )
    }
}