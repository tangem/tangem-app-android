package com.tangem.domain.polymarket.sessionkey

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketSessionCredentialsError
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderSigningRoute
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

internal class PolymarketOrderSigningRouterTest {

    private val repository: PolymarketRepository = mockk()
    private val sessionKeyRepository: PolymarketSessionKeyRepository = mockk()
    private val sessionKeyStore: PolymarketSessionKeyStore = mockk()
    private val tradingStore: PolymarketSessionKeyTradingStore = mockk(relaxUnitFun = true)
    private val sessionCredentialsProvider: PolymarketSessionCredentialsProvider = mockk()
    private val deviceIdProvider: PolymarketDeviceIdProvider = mockk()
    private val credentialsStore: PolymarketCredentialsStore = mockk()

    private val router = PolymarketOrderSigningRouter(
        polymarketRepository = repository,
        sessionKeyRepository = sessionKeyRepository,
        sessionKeyStore = sessionKeyStore,
        tradingStore = tradingStore,
        sessionCredentialsProvider = sessionCredentialsProvider,
        deviceIdProvider = deviceIdProvider,
        getApiCredentials = GetPolymarketApiCredentialsUseCase(credentialsStore = credentialsStore),
        clock = Clock.fixed(NOW, ZoneOffset.UTC),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            repository,
            sessionKeyRepository,
            sessionKeyStore,
            tradingStore,
            sessionCredentialsProvider,
            deviceIdProvider,
            credentialsStore,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Route {

        @BeforeEach
        fun arrangeTradableKey() {
            coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns SESSION_KEY
            every { tradingStore.isRefused(USER_WALLET_ID, SESSION_KEY) } returns false
            every { tradingStore.isMissingOnExchange(USER_WALLET_ID, SESSION_KEY) } returns false
            coEvery { repository.getWalletStatusByWalletId(WALLET_ID) } returns walletState().right()
            coEvery { tradingStore.isReadyOnExchange(USER_WALLET_ID, SESSION_KEY, VALID_UNTIL) } returns true
            coEvery { sessionCredentialsProvider.get(USER_WALLET_ID) } returns SESSION_IDENTITY.right()
        }

        @Test
        fun `GIVEN every condition holds WHEN route THEN signs with the session key`() = runTest {
            // Act
            val actual = router.route(addresses = ADDRESSES)

            // Assert
            assertThat(actual).isEqualTo(PredictionOrderSigningRoute.Session(identity = SESSION_IDENTITY))
        }

        @Test
        fun `GIVEN no session key on the device WHEN route THEN signs with the owner key and reads nothing`() =
            runTest {
                // Arrange
                coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns null

                // Act
                val actual = router.route(addresses = ADDRESSES)

                // Assert
                assertThat(actual).isEqualTo(PredictionOrderSigningRoute.Owner)
                coVerify(exactly = 0) { repository.getWalletStatusByWalletId(any()) }
                coVerify(exactly = 0) { sessionCredentialsProvider.get(any()) }
            }

        @Test
        fun `GIVEN the exchange refused the key WHEN route THEN signs with the owner key`() = runTest {
            // Arrange
            every { tradingStore.isRefused(USER_WALLET_ID, SESSION_KEY) } returns true

            // Act
            val actual = router.route(addresses = ADDRESSES)

            // Assert
            assertThat(actual).isEqualTo(PredictionOrderSigningRoute.Owner)
            coVerify(exactly = 0) { repository.getWalletStatusByWalletId(any()) }
        }

        @Test
        fun `GIVEN the last reconciliation found the key gone WHEN route THEN signs with the owner key`() = runTest {
            // Arrange
            every { tradingStore.isMissingOnExchange(USER_WALLET_ID, SESSION_KEY) } returns true

            // Act
            val actual = router.route(addresses = ADDRESSES)

            // Assert
            assertThat(actual).isEqualTo(PredictionOrderSigningRoute.Owner)
        }

        @Test
        fun `GIVEN the wallet cannot be read WHEN route THEN signs with the owner key`() = runTest {
            // Arrange
            coEvery { repository.getWalletStatusByWalletId(WALLET_ID) } returns
                PolymarketWalletError.Network.left()

            // Act
            val actual = router.route(addresses = ADDRESSES)

            // Assert
            assertThat(actual).isEqualTo(PredictionOrderSigningRoute.Owner)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the BFF state WHEN route THEN signs with the session key only when it lets the key trade`(
            model: BffModel,
        ) = runTest {
            // Arrange
            coEvery { repository.getWalletStatusByWalletId(WALLET_ID) } returns model.state.right()
            coEvery { tradingStore.isReadyOnExchange(USER_WALLET_ID, SESSION_KEY, any()) } returns true

            // Act
            val actual = router.route(addresses = ADDRESSES)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            BffModel(
                case = "tradable record",
                state = walletState(),
                expected = PredictionOrderSigningRoute.Session(identity = SESSION_IDENTITY),
            ),
            BffModel(
                case = "record matched ignoring case",
                state = walletState(keys = listOf(sessionKey(address = SESSION_KEY.uppercase().replace("0X", "0x")))),
                expected = PredictionOrderSigningRoute.Session(identity = SESSION_IDENTITY),
            ),
            BffModel(
                case = "wallet not ready to trade",
                state = walletState(status = PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
                expected = PredictionOrderSigningRoute.Owner,
            ),
            BffModel(
                case = "no record of the key",
                state = walletState(keys = listOf(sessionKey(address = OTHER_KEY))),
                expected = PredictionOrderSigningRoute.Owner,
            ),
            BffModel(
                case = "record pending",
                state = walletState(keys = listOf(sessionKey(status = PolymarketSessionKeyStatus.PENDING))),
                expected = PredictionOrderSigningRoute.Owner,
            ),
            BffModel(
                case = "record revoked",
                state = walletState(keys = listOf(sessionKey(status = PolymarketSessionKeyStatus.REVOKED))),
                expected = PredictionOrderSigningRoute.Owner,
            ),
            BffModel(
                case = "record disabled",
                state = walletState(keys = listOf(sessionKey(isEnabled = false))),
                expected = PredictionOrderSigningRoute.Owner,
            ),
            BffModel(
                case = "record without validUntil",
                state = walletState(keys = listOf(sessionKey(validUntil = null))),
                expected = PredictionOrderSigningRoute.Owner,
            ),
            BffModel(
                case = "record expiring exactly in ten minutes",
                state = walletState(keys = listOf(sessionKey(validUntil = NOW + Duration.ofMinutes(10)))),
                expected = PredictionOrderSigningRoute.Owner,
            ),
            BffModel(
                case = "record valid for ten minutes and a second",
                state = walletState(
                    keys = listOf(sessionKey(validUntil = NOW + Duration.ofMinutes(10).plusSeconds(1))),
                ),
                expected = PredictionOrderSigningRoute.Session(identity = SESSION_IDENTITY),
            ),
        )

        @Test
        fun `GIVEN Polymarket has not listed the key since its authorization WHEN route THEN signs with the owner key`() =
            runTest {
                // Arrange
                coEvery { tradingStore.isReadyOnExchange(USER_WALLET_ID, SESSION_KEY, VALID_UNTIL) } returns false

                // Act
                val actual = router.route(addresses = ADDRESSES)

                // Assert
                assertThat(actual).isEqualTo(PredictionOrderSigningRoute.Owner)
                coVerify(exactly = 0) { sessionCredentialsProvider.get(any()) }
            }

        @Test
        fun `GIVEN the session credentials cannot be derived WHEN route THEN signs with the owner key`() = runTest {
            // Arrange
            coEvery { sessionCredentialsProvider.get(USER_WALLET_ID) } returns
                PolymarketSessionCredentialsError.Auth(cause = PolymarketAuthError.Network).left()

            // Act
            val actual = router.route(addresses = ADDRESSES)

            // Assert
            assertThat(actual).isEqualTo(PredictionOrderSigningRoute.Owner)
        }

        @Test
        fun `GIVEN the session credentials of another key WHEN route THEN signs with the owner key`() = runTest {
            // Arrange
            coEvery { sessionCredentialsProvider.get(USER_WALLET_ID) } returns
                SESSION_IDENTITY.copy(address = OTHER_KEY).right()

            // Act
            val actual = router.route(addresses = ADDRESSES)

            // Assert
            assertThat(actual).isEqualTo(PredictionOrderSigningRoute.Owner)
        }
    }

    @Nested
    inner class ReportRefused {

        @Test
        fun `GIVEN a refusal WHEN reportRefused THEN marks the key refused without reading the wallet`() = runTest {
            // Act
            router.reportRefused(addresses = ADDRESSES, identity = SESSION_IDENTITY, error = UNAUTHORIZED)

            // Assert
            coVerify(exactly = 1) { tradingStore.markRefused(USER_WALLET_ID, SESSION_KEY) }
            coVerify(exactly = 0) { repository.getWalletStatusByWalletId(any()) }
        }

        @Test
        fun `GIVEN a local signing failure WHEN reportSigningFailed THEN marks the key refused`() = runTest {
            // Arrange
            val failure = PredictionOrderFailure.Signing(
                error = PolymarketSigningError.SessionKey(error = PolymarketSessionKeyError.KeyMissing),
            )

            // Act
            router.reportSigningFailed(addresses = ADDRESSES, identity = SESSION_IDENTITY, failure = failure)

            // Assert
            coVerify(exactly = 1) { tradingStore.markRefused(USER_WALLET_ID, SESSION_KEY) }
            coVerify(exactly = 0) { repository.getWalletStatusByWalletId(any()) }
        }
    }

    @Nested
    inner class Reconcile {

        @BeforeEach
        fun arrangeReconciliation() {
            coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns SESSION_KEY
            coEvery { deviceIdProvider.deviceId() } returns DEVICE_ID
            coEvery { credentialsStore.get(USER_WALLET_ID) } returns OWNER_CREDENTIALS
        }

        @Test
        fun `GIVEN the key is gone from Polymarket WHEN reconcile THEN records it missing on the exchange`() =
            runTest {
                // Arrange
                coEvery { repository.getWalletStatusByWalletId(WALLET_ID) } returns walletState(
                    keys = listOf(sessionKey(authorizedAt = NOW - Duration.ofDays(1))),
                ).right()
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                    emptyList<PolymarketSessionSigner>().right()

                // Act
                router.reconcile(addresses = ADDRESSES)

                // Assert
                verify(exactly = 1) {
                    tradingStore.setReconciliation(
                        USER_WALLET_ID,
                        PolymarketSessionKeyReconciliation.Checked(
                            foreignSigners = emptyList(),
                            missingOnExchange = setOf(SESSION_KEY),
                        ),
                    )
                }
            }

        @Test
        fun `GIVEN the signers cannot be read WHEN reconcile THEN keeps an unavailable reconciliation`() = runTest {
            // Arrange
            coEvery { repository.getWalletStatusByWalletId(WALLET_ID) } returns walletState().right()
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                PolymarketAuthError.Network.left()

            // Act
            router.reconcile(addresses = ADDRESSES)

            // Assert
            verify(exactly = 1) {
                tradingStore.setReconciliation(USER_WALLET_ID, PolymarketSessionKeyReconciliation.Unavailable)
            }
        }

        @Test
        fun `GIVEN the wallet cannot be read WHEN reconcile THEN reconciles nothing`() = runTest {
            // Arrange
            coEvery { repository.getWalletStatusByWalletId(WALLET_ID) } returns PolymarketWalletError.Network.left()

            // Act
            router.reconcile(addresses = ADDRESSES)

            // Assert
            verify(exactly = 0) { tradingStore.setReconciliation(any(), any()) }
            coVerify(exactly = 0) { sessionKeyRepository.getSessionSigners(any()) }
        }
    }

    data class BffModel(
        val case: String,
        val state: PolymarketWalletState,
        val expected: PredictionOrderSigningRoute,
    )

    private companion object {
        const val WALLET_ID = "011"
        const val SESSION_KEY = "0x5e55000000000000000000000000000000000001"
        const val OTHER_KEY = "0x01d0000000000000000000000000000000000009"
        const val OWNER = "0x00000000000000000000000000000000000000aa"
        const val DEPOSIT_WALLET = "0x00000000000000000000000000000000000000bb"
        const val DEVICE_ID = "device"

        val USER_WALLET_ID = UserWalletId(WALLET_ID)
        val ADDRESSES = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = DEPOSIT_WALLET,
            userWalletId = USER_WALLET_ID,
        )
        val NOW: Instant = Instant.ofEpochSecond(1_790_000_000)
        val VALID_UNTIL: Instant = NOW + Duration.ofDays(30)
        val SESSION_IDENTITY = PolymarketClobIdentity.Session(
            address = SESSION_KEY,
            credentials = PolymarketApiCredentials(apiKey = "session", secret = "c2VjcmV0", passphrase = "pass"),
        )
        val OWNER_CREDENTIALS = PolymarketApiCredentials(apiKey = "owner", secret = "c2VjcmV0", passphrase = "pass")
        val OWNER_IDENTITY = PolymarketClobIdentity.Owner(address = OWNER, credentials = OWNER_CREDENTIALS)
        val UNAUTHORIZED = PredictionPlaceOrderError.Unauthorized(detail = "Unauthorized/Invalid api key")

        fun sessionKey(
            address: String = SESSION_KEY,
            status: PolymarketSessionKeyStatus = PolymarketSessionKeyStatus.ACTIVE,
            isEnabled: Boolean = true,
            validUntil: Instant? = VALID_UNTIL,
            authorizedAt: Instant? = NOW - Duration.ofDays(1),
        ) = PolymarketSessionKey(
            address = address,
            deviceId = DEVICE_ID,
            deviceName = "Pixel 9",
            isEnabled = isEnabled,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = validUntil,
            renewAfter = validUntil?.minus(Duration.ofDays(3)),
            authorizedAt = authorizedAt,
            createdAt = NOW - Duration.ofDays(2),
        )

        fun walletState(
            status: PolymarketWalletStatus = PolymarketWalletStatus.READY_TO_TRADE,
            keys: List<PolymarketSessionKey> = listOf(sessionKey()),
        ) = PolymarketWalletState(depositWalletAddress = DEPOSIT_WALLET, status = status, sessionKeys = keys)
    }
}