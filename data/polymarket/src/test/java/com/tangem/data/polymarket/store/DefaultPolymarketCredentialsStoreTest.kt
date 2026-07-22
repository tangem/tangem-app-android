package com.tangem.data.polymarket.store

import com.google.common.truth.Truth.assertThat
import com.tangem.common.services.secure.SecureStorage
import com.tangem.data.polymarket.converter.toDomain
import com.tangem.data.polymarket.converter.toDto
import com.tangem.data.polymarket.entity.PolymarketApiCredentialsDTO
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.logging.Severity
import com.tangem.utils.logging.TangemLogger
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
    fun `GIVEN credentials WHEN store THEN saved as json under the address key`() = runTest {
        // Arrange
        val payload = slot<String>()
        every { secureStorage.store(eq(EXPECTED_KEY), capture(payload)) } returns Unit

        // Act
        store.store(ownerAddress = OWNER_ADDRESS, credentials = CREDENTIALS)

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
        store.store(ownerAddress = OWNER_ADDRESS, credentials = CREDENTIALS)

        // Assert
        assertThat(json.parseToJsonElement(payload.captured).jsonObject.keys)
            .containsExactly("apiKey", "secret", "passphrase")
    }

    @Test
    fun `GIVEN stored json WHEN get THEN returns deserialized credentials`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns encode(CREDENTIALS)

        // Act
        val actual = store.get(ownerAddress = OWNER_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS)
    }

    @Test
    fun `GIVEN no entry WHEN get THEN returns null`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns null

        // Act
        val actual = store.get(ownerAddress = OWNER_ADDRESS)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN corrupted json WHEN get THEN returns null and drops the entry`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns "{not a json"

        // Act
        val actual = store.get(ownerAddress = OWNER_ADDRESS)

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
        store.get(ownerAddress = OWNER_ADDRESS)

        // Assert
        assertThat(logs.entries).isNotEmpty()
        assertThat(logs.entries.map { it.second }).containsExactly(null)
        assertThat(logs.entries.none { it.first.contains(CREDENTIALS.secret) }).isTrue()
    }

    private fun encode(credentials: PolymarketApiCredentials): String =
        json.encodeToString(PolymarketApiCredentialsDTO.serializer(), credentials.toDto())

    private fun decode(payload: String): PolymarketApiCredentials =
        json.decodeFromString(PolymarketApiCredentialsDTO.serializer(), payload).toDomain()

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
    fun `GIVEN checksummed address WHEN store and get THEN both use the same lowercased slot`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns encode(CREDENTIALS)

        // Act
        store.store(ownerAddress = CHECKSUMMED_OWNER_ADDRESS, credentials = CREDENTIALS)
        val actual = store.get(ownerAddress = OWNER_ADDRESS)

        // Assert
        verify(exactly = 1) { secureStorage.store(EXPECTED_KEY, any()) }
        assertThat(actual).isEqualTo(CREDENTIALS)
    }

    @Test
    fun `GIVEN in-memory storage WHEN store THEN the entry is keyed by address and holds the credentials`() = runTest {
        // Arrange
        val storage = InMemorySecureStorage()
        val storeOverRealStorage = DefaultPolymarketCredentialsStore(
            secureStorage = storage,
            json = json,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        // Act
        storeOverRealStorage.store(ownerAddress = OWNER_ADDRESS, credentials = CREDENTIALS)

        // Assert
        assertThat(storage.entries.keys).containsExactly(EXPECTED_KEY)
        assertThat(storeOverRealStorage.get(ownerAddress = CHECKSUMMED_OWNER_ADDRESS)).isEqualTo(CREDENTIALS)
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

    @Test
    fun `GIVEN address WHEN clear THEN deletes the entry`() = runTest {
        // Act
        store.clear(ownerAddress = CHECKSUMMED_OWNER_ADDRESS)

        // Assert
        verify(exactly = 1) { secureStorage.delete(EXPECTED_KEY) }
    }

    private companion object {
        const val OWNER_ADDRESS = "0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed"
        const val CHECKSUMMED_OWNER_ADDRESS = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"
        const val EXPECTED_KEY = "polymarket_api_credentials_0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed"

        val CREDENTIALS = PolymarketApiCredentials(
            apiKey = "df2b7b32-a2e6-4a3f-9b1c-0f0e5f5f0000",
            secret = "c2VjcmV0LWJ5dGVzLWJhc2U2NA==",
            passphrase = "passphrase-value",
        )
    }
}