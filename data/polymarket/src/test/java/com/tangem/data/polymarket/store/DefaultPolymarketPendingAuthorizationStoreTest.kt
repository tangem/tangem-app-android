package com.tangem.data.polymarket.store

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.polymarket.sessionkey.InMemorySecureStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorization
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.security.GeneralSecurityException
import java.security.UnrecoverableKeyException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketPendingAuthorizationStoreTest {

    private val secureStorage = InMemorySecureStorage()
    private var inFlightUntil = MockStateDataStore<Map<String, String>>(default = emptyMap())
    private var now = Instant.ofEpochSecond(IN_FLIGHT_SECONDS)

    private val store
        get() = DefaultPolymarketPendingAuthorizationStore(
            secureStorage = secureStorage,
            inFlightUntil = inFlightUntil,
            json = Json { ignoreUnknownKeys = true },
            dispatchers = TestingCoroutineDispatcherProvider(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

    @BeforeEach
    fun resetStorage() {
        inFlightUntil = MockStateDataStore(default = emptyMap())
        now = Instant.ofEpochSecond(IN_FLIGHT_SECONDS)
        secureStorage.entries.clear()
        secureStorage.failWrites = false
        secureStorage.failReads = false
        secureStorage.failDeletes = false
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a pending authorization WHEN stored THEN get returns the same one`(model: RoundTripModel) = runTest {
        // Arrange
        val pending = pending(purpose = model.purpose, replaces = model.replacesSessionKeyAddress)

        // Act
        val stored = store.store(USER_WALLET_ID, pending)
        val actual = store.get(USER_WALLET_ID)

        // Assert
        assertThat(stored).isEqualTo(Unit.right())
        assertThat(actual).isEqualTo(pending.right())
    }

    @Test
    fun `GIVEN a stored pending authorization WHEN cleared THEN get returns null`() = runTest {
        // Arrange
        store.store(USER_WALLET_ID, pending())

        // Act
        store.clear(USER_WALLET_ID)

        // Assert
        assertThat(store.get(USER_WALLET_ID)).isEqualTo(null.right())
        assertThat(secureStorage.entries).isEmpty()
    }

    @Test
    fun `GIVEN nothing stored WHEN get THEN null`() = runTest {
        // Act
        val actual = store.get(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(null.right())
    }

    @Test
    fun `GIVEN a corrupt payload WHEN get THEN null and the key is deleted`() = runTest {
        // Arrange
        secureStorage.entries[STORAGE_KEY] = "{not a pending authorization"

        // Act
        val actual = store.get(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(null.right())
        assertThat(secureStorage.entries).isEmpty()
    }

    @Test
    fun `GIVEN the storage key is permanently invalidated WHEN get THEN null and the key is deleted`() = runTest {
        // Arrange
        store.store(USER_WALLET_ID, pending())
        secureStorage.failReads = true
        secureStorage.readFailure = IllegalStateException("Decryption failed", UnrecoverableKeyException("Key lost"))

        // Act
        val actual = store.get(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(null.right())
        assertThat(secureStorage.entries).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("provideTransientReadFailures")
    fun `GIVEN the storage is temporarily unreadable WHEN get THEN Failure and the key is kept`(
        failure: Exception,
    ) = runTest {
        // Arrange
        store.store(USER_WALLET_ID, pending())
        secureStorage.failReads = true
        secureStorage.readFailure = failure

        // Act
        val actual = store.get(USER_WALLET_ID)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PolymarketSessionKeyError.Failure::class.java)
        assertThat(secureStorage.entries.keys).containsExactly(STORAGE_KEY)
    }

    @Test
    fun `GIVEN the storage rejects writes WHEN store THEN Failure`() = runTest {
        // Arrange
        secureStorage.failWrites = true

        // Act
        val actual = store.store(USER_WALLET_ID, pending())

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PolymarketSessionKeyError.Failure::class.java)
        assertThat(actual.isLeft()).isTrue()
        assertThat(secureStorage.entries).isEmpty()
    }

    @Test
    fun `GIVEN the storage rejects deletes WHEN clear THEN it does not throw`() = runTest {
        // Arrange
        store.store(USER_WALLET_ID, pending())
        secureStorage.failDeletes = true

        // Act
        store.clear(USER_WALLET_ID)

        // Assert
        assertThat(secureStorage.entries.keys).containsExactly(STORAGE_KEY)
    }

    @Test
    fun `GIVEN the payload reads as absent while the pair may still land WHEN get THEN Failure and the marker is kept`() =
        runTest {
            // Arrange
            store.store(USER_WALLET_ID, pending())
            secureStorage.entries.clear()

            // Act
            val actual = store.get(USER_WALLET_ID)

            // Assert
            assertThat(actual.leftOrNull()).isInstanceOf(PolymarketSessionKeyError.Failure::class.java)
            assertThat(inFlightUntil.data.first()).containsExactly(MARKER_KEY, LAST_DEADLINE)
        }

    @Test
    fun `GIVEN the payload reads as absent after both deadlines WHEN get THEN null and the marker is cleared`() =
        runTest {
            // Arrange
            store.store(USER_WALLET_ID, pending())
            secureStorage.entries.clear()
            now = Instant.ofEpochSecond(LAST_DEADLINE.toLong())

            // Act
            val actual = store.get(USER_WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(null.right())
            assertThat(inFlightUntil.data.first()).isEmpty()
        }

    @Test
    fun `GIVEN a stored pair WHEN stored THEN the marker holds the later deadline`() = runTest {
        // Act
        store.store(USER_WALLET_ID, pending())

        // Assert
        assertThat(inFlightUntil.data.first()).containsExactly(MARKER_KEY, LAST_DEADLINE)
    }

    @Test
    fun `GIVEN the payload write fails WHEN store THEN the marker is cleared`() = runTest {
        // Arrange
        secureStorage.failWrites = true

        // Act
        store.store(USER_WALLET_ID, pending())

        // Assert
        assertThat(inFlightUntil.data.first()).isEmpty()
        assertThat(store.get(USER_WALLET_ID)).isEqualTo(null.right())
    }

    @Test
    fun `GIVEN a stored pair WHEN cleared THEN the marker is cleared too`() = runTest {
        // Arrange
        store.store(USER_WALLET_ID, pending())

        // Act
        store.clear(USER_WALLET_ID)

        // Assert
        assertThat(inFlightUntil.data.first()).isEmpty()
    }

    private fun provideTestModels() = listOf(
        RoundTripModel(PolymarketPendingAuthorization.Purpose.EXTEND, replacesSessionKeyAddress = null),
        RoundTripModel(PolymarketPendingAuthorization.Purpose.EXTEND, replacesSessionKeyAddress = REPLACED),
        RoundTripModel(PolymarketPendingAuthorization.Purpose.REPLACE, replacesSessionKeyAddress = null),
        RoundTripModel(PolymarketPendingAuthorization.Purpose.REPLACE, replacesSessionKeyAddress = REPLACED),
    )

    private fun pending(
        purpose: PolymarketPendingAuthorization.Purpose = PolymarketPendingAuthorization.Purpose.EXTEND,
        replaces: String? = null,
    ): PolymarketPendingAuthorization {
        val call = PolymarketApprovalCall(target = DEPOSIT_WALLET, value = "0", data = "0xabcd")
        return PolymarketPendingAuthorization(
            revocation = PolymarketSessionKeyRevocation(
                ownerAddress = OWNER,
                depositWalletAddress = DEPOSIT_WALLET,
                sessionKeyAddress = SESSION_KEY,
                nonce = "7",
                deadline = "1789430600",
                call = call,
                signature = "0xrevoke",
            ),
            authorization = PolymarketSessionKeyAuthorization(
                ownerAddress = OWNER,
                depositWalletAddress = DEPOSIT_WALLET,
                sessionKeyAddress = SESSION_KEY,
                deviceId = "device",
                deviceName = "Pixel",
                validUntil = "1805000000",
                nonce = "8",
                deadline = "1789431200",
                call = call,
                signature = "0xsig",
                replacesSessionKeyAddress = replaces,
            ),
            purpose = purpose,
        )
    }

    private fun provideTransientReadFailures() = listOf(
        IllegalStateException("Keystore is locked"),
        GeneralSecurityException("Keystore operation failed"),
    )

    data class RoundTripModel(
        val purpose: PolymarketPendingAuthorization.Purpose,
        val replacesSessionKeyAddress: String?,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val STORAGE_KEY = "polymarket_pending_authorization_011"
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0x2222222222222222222222222222222222222222"
        const val SESSION_KEY = "0x5e55000000000000000000000000000000000001"
        const val REPLACED = "0x5e55000000000000000000000000000000000002"
        const val MARKER_KEY = "011"
        const val LAST_DEADLINE = "1789431200"
        const val IN_FLIGHT_SECONDS = 1_789_430_000L
    }
}