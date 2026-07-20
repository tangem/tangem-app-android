package com.tangem.data.polymarket.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketCredentialsStoreTest {

    private val secureStorage: SecureStorage = mockk(relaxed = true)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(PolymarketApiCredentials::class.java)

    private val store = DefaultPolymarketCredentialsStore(
        secureStorage = secureStorage,
        moshi = moshi,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(secureStorage)
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
        assertThat(adapter.fromJson(payload.captured)).isEqualTo(CREDENTIALS)
    }

    @Test
    fun `GIVEN stored json WHEN get THEN returns deserialized credentials`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns adapter.toJson(CREDENTIALS)

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
    fun `GIVEN checksummed address WHEN store and get THEN both use the same lowercased slot`() = runTest {
        // Arrange
        every { secureStorage.getAsString(EXPECTED_KEY) } returns adapter.toJson(CREDENTIALS)

        // Act
        store.store(ownerAddress = CHECKSUMMED_OWNER_ADDRESS, credentials = CREDENTIALS)
        val actual = store.get(ownerAddress = OWNER_ADDRESS)

        // Assert
        verify(exactly = 1) { secureStorage.store(EXPECTED_KEY, any()) }
        assertThat(actual).isEqualTo(CREDENTIALS)
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