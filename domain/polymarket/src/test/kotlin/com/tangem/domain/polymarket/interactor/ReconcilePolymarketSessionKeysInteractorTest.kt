package com.tangem.domain.polymarket.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyAssessor
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeySnapshot
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

internal class ReconcilePolymarketSessionKeysInteractorTest {

    private val polymarketRepository: PolymarketRepository = mockk()
    private val sessionKeyRepository: PolymarketSessionKeyRepository = mockk()
    private val sessionKeyStore: PolymarketSessionKeyStore = mockk()
    private val deviceIdProvider: PolymarketDeviceIdProvider = mockk()
    private val tradingStore: PolymarketSessionKeyTradingStore = mockk(relaxed = true)
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase = mockk()

    private val interactor = ReconcilePolymarketSessionKeysInteractor(
        polymarketRepository = polymarketRepository,
        sessionKeyRepository = sessionKeyRepository,
        sessionKeyStore = sessionKeyStore,
        deviceIdProvider = deviceIdProvider,
        tradingStore = tradingStore,
        deriveAddresses = deriveAddresses,
        getApiCredentials = getApiCredentials,
        clock = Clock.fixed(NOW, ZoneOffset.UTC),
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            polymarketRepository,
            sessionKeyRepository,
            sessionKeyStore,
            deviceIdProvider,
            tradingStore,
            deriveAddresses,
            getApiCredentials,
        )
        coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns KEY
        coEvery { deviceIdProvider.deviceId() } returns DEVICE_ID
        coEvery { deriveAddresses.stored(USER_WALLET_ID) } returns ADDRESSES
        coEvery { getApiCredentials(USER_WALLET_ID) } returns OWNER_CREDENTIALS
        coEvery { tradingStore.isReadyOnExchange(any(), any(), any()) } returns false
        every { tradingStore.isRefused(any(), any()) } returns false
    }

    @Test
    fun `GIVEN a foreign signer WHEN reconcile THEN the reconciliation is kept and returned`() = runTest {
        // Arrange
        val signers = listOf(signer(KEY), signer(FOREIGN))
        givenWallet(listOf(key()))
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns signers.right()

        // Act
        val actual = interactor(USER_WALLET_ID)

        // Assert
        val expectedAssessment = PolymarketSessionKeyAssessor.assess(
            PolymarketSessionKeySnapshot(
                sessionKeys = listOf(key()),
                localKeyAddress = KEY,
                deviceId = DEVICE_ID,
                signers = signers,
                now = NOW,
            ),
        )
        assertThat(actual).isEqualTo(
            PolymarketSessionKeyOverview(
                wallet = wallet(listOf(key())),
                localKeyAddress = KEY,
                assessment = expectedAssessment,
                isTradable = false,
            ).right(),
        )
        verify(exactly = 1) { tradingStore.setReconciliation(USER_WALLET_ID, expectedAssessment.reconciliation) }
    }

    @Test
    fun `GIVEN the own active key is listed and not yet ready WHEN reconcile THEN it is marked ready`() = runTest {
        // Arrange
        givenWallet(listOf(key()))
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns listOf(signer(KEY)).right()

        // Act
        interactor(USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) { tradingStore.restoreReadyOnExchange(USER_WALLET_ID, KEY, VALID_UNTIL) }
        coVerify(exactly = 0) { tradingStore.markReadyOnExchange(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the own listed key was refused by the CLOB WHEN reconcile THEN it is not marked ready`() = runTest {
        // Arrange
        givenWallet(listOf(key()))
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns listOf(signer(KEY)).right()
        every { tradingStore.isRefused(USER_WALLET_ID, KEY) } returns true

        // Act
        interactor(USER_WALLET_ID)

        // Assert
        coVerify(exactly = 0) { tradingStore.restoreReadyOnExchange(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the own key is listed with another expiry WHEN reconcile THEN it is marked ready`() = runTest {
        // Arrange
        givenWallet(listOf(key()))
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
            listOf(signer(KEY, validUntil = VALID_UNTIL.plusSeconds(60))).right()

        // Act
        interactor(USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) { tradingStore.restoreReadyOnExchange(USER_WALLET_ID, KEY, VALID_UNTIL) }
        coVerify(exactly = 0) { tradingStore.markReadyOnExchange(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the own key is listed expiring within ten minutes WHEN reconcile THEN it is not marked ready`() =
        runTest {
            // Arrange
            givenWallet(listOf(key()))
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                listOf(signer(KEY, validUntil = NOW.plus(Duration.ofMinutes(10)))).right()

            // Act
            interactor(USER_WALLET_ID)

            // Assert
            coVerify(exactly = 0) { tradingStore.restoreReadyOnExchange(any(), any(), any()) }
        }

    @Test
    fun `GIVEN no owner credentials WHEN reconcile THEN signers are not read and reconciliation is unavailable`() =
        runTest {
            // Arrange
            givenWallet(listOf(key()))
            coEvery { getApiCredentials(USER_WALLET_ID) } returns null

            // Act
            val actual = interactor(USER_WALLET_ID)

            // Assert
            assertThat(actual.getOrNull()?.assessment?.reconciliation)
                .isEqualTo(PolymarketSessionKeyReconciliation.Unavailable)
            coVerify(exactly = 0) { sessionKeyRepository.getSessionSigners(any()) }
            verify(exactly = 1) {
                tradingStore.setReconciliation(USER_WALLET_ID, PolymarketSessionKeyReconciliation.Unavailable)
            }
        }

    @Test
    fun `GIVEN the own key is listed and ready WHEN reconcile THEN it is tradable`() = runTest {
        // Arrange
        givenWallet(listOf(key()))
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns listOf(signer(KEY)).right()
        coEvery { tradingStore.isReadyOnExchange(USER_WALLET_ID, KEY, VALID_UNTIL) } returns true

        // Act
        val actual = interactor(USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.isTradable).isTrue()
        coVerify(exactly = 1) { polymarketRepository.getWalletStatusByWalletId(any()) }
    }

    @Test
    fun `GIVEN the own key was refused by the CLOB WHEN reconcile THEN it is not tradable`() = runTest {
        // Arrange
        givenWallet(listOf(key()))
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns listOf(signer(KEY)).right()
        coEvery { tradingStore.isReadyOnExchange(USER_WALLET_ID, KEY, VALID_UNTIL) } returns true
        every { tradingStore.isRefused(USER_WALLET_ID, KEY) } returns true

        // Act
        val actual = interactor(USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.isTradable).isFalse()
    }

    @Test
    fun `GIVEN no key on this device WHEN reconcile THEN it is not tradable`() = runTest {
        // Arrange
        givenWallet(listOf(key()))
        coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns null
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns listOf(signer(KEY)).right()
        coEvery { tradingStore.isReadyOnExchange(any(), any(), any()) } returns true

        // Act
        val actual = interactor(USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.isTradable).isFalse()
    }

    @Test
    fun `GIVEN the wallet cannot be read WHEN reconcile THEN the error is returned and nothing is kept`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returns
            PolymarketWalletError.Network.left()

        // Act
        val actual = interactor(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketWalletError.Network.left())
        verify(exactly = 0) { tradingStore.setReconciliation(any(), any()) }
    }

    @Test
    fun `GIVEN the key store throws WHEN reconcile THEN an unexpected error is returned and nothing is kept`() =
        runTest {
            // Arrange
            givenWallet(listOf(key()))
            coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } throws IllegalStateException("keystore locked")

            // Act
            val actual = interactor(USER_WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketWalletError.Unexpected(httpCode = null, detail = "keystore locked").left(),
            )
            verify(exactly = 0) { tradingStore.setReconciliation(any(), any()) }
        }

    @Test
    fun `GIVEN the trading store throws WHEN reconcile THEN an unexpected error is returned`() = runTest {
        // Arrange
        givenWallet(listOf(key()))
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns listOf(signer(KEY)).right()
        every { tradingStore.setReconciliation(any(), any()) } throws IllegalStateException("store closed")

        // Act
        val actual = interactor(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketWalletError.Unexpected(httpCode = null, detail = "store closed").left())
    }

    @Test
    fun `GIVEN the owner addresses cannot be read WHEN reconcile THEN the overview is built without signers`() =
        runTest {
            // Arrange
            givenWallet(listOf(key()))
            coEvery { deriveAddresses.stored(USER_WALLET_ID) } throws IllegalStateException("keystore locked")

            // Act
            val actual = interactor(USER_WALLET_ID)

            // Assert
            assertThat(actual.getOrNull()?.assessment?.reconciliation)
                .isEqualTo(PolymarketSessionKeyReconciliation.Unavailable)
            coVerify(exactly = 0) { sessionKeyRepository.getSessionSigners(any()) }
            verify(exactly = 1) {
                tradingStore.setReconciliation(USER_WALLET_ID, PolymarketSessionKeyReconciliation.Unavailable)
            }
        }

    @Test
    fun `GIVEN the owner credentials cannot be read WHEN reconcile THEN the overview is built without signers`() =
        runTest {
            // Arrange
            givenWallet(listOf(key()))
            coEvery { getApiCredentials(USER_WALLET_ID) } throws IllegalStateException("keystore locked")

            // Act
            val actual = interactor(USER_WALLET_ID)

            // Assert
            assertThat(actual.getOrNull()?.assessment?.reconciliation)
                .isEqualTo(PolymarketSessionKeyReconciliation.Unavailable)
            coVerify(exactly = 0) { sessionKeyRepository.getSessionSigners(any()) }
        }

    @Test
    fun `GIVEN the tradability cannot be read WHEN reconcile THEN the overview is kept and not tradable`() = runTest {
        // Arrange
        givenWallet(listOf(key()))
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns emptyList<PolymarketSessionSigner>()
            .right()
        every { tradingStore.isRefused(USER_WALLET_ID, KEY) } throws IllegalStateException("store closed")

        // Act
        val actual = interactor(USER_WALLET_ID)

        // Assert
        assertThat(actual.isRight()).isTrue()
        assertThat(actual.getOrNull()?.isTradable).isFalse()
    }

    private fun givenWallet(sessionKeys: List<PolymarketSessionKey>) {
        coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returns
            wallet(sessionKeys).right()
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0x2222222222222222222222222222222222222222"
        const val KEY = "0x5e55000000000000000000000000000000000001"
        const val FOREIGN = "0x5e55000000000000000000000000000000000003"
        const val DEVICE_ID = "device"
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)
        val VALID_UNTIL: Instant = NOW.plus(Duration.ofDays(170))
        val ADDRESSES = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = DEPOSIT_WALLET,
            userWalletId = USER_WALLET_ID,
        )
        val OWNER_CREDENTIALS = PolymarketApiCredentials(apiKey = "owner", secret = "s", passphrase = "p")
        val OWNER_IDENTITY = PolymarketClobIdentity.Owner(address = OWNER, credentials = OWNER_CREDENTIALS)

        fun wallet(sessionKeys: List<PolymarketSessionKey>) = PolymarketWalletState(
            depositWalletAddress = DEPOSIT_WALLET,
            status = PolymarketWalletStatus.READY_TO_TRADE,
            sessionKeys = sessionKeys,
        )

        fun key(status: PolymarketSessionKeyStatus = PolymarketSessionKeyStatus.ACTIVE) = PolymarketSessionKey(
            address = KEY,
            deviceId = DEVICE_ID,
            deviceName = "Pixel",
            isEnabled = true,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = VALID_UNTIL,
            renewAfter = null,
            authorizedAt = NOW.minus(Duration.ofDays(10)),
            createdAt = NOW.minus(Duration.ofDays(10)),
        )

        fun signer(address: String, validUntil: Instant = VALID_UNTIL) =
            PolymarketSessionSigner(address = address, scopes = listOf("CLOB"), validUntil = validUntil)
    }
}