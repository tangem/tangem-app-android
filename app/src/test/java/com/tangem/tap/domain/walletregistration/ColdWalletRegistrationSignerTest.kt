package com.tangem.tap.domain.walletregistration

import com.google.common.truth.Truth.assertThat
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.CryptoUtils.generatePublicKey
import com.tangem.crypto.sign
import com.tangem.data.wallets.cold.UserWalletIdPreflightReadFilter
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.scan.CardDTO
import com.tangem.lib.auth.session.WalletSignatureBundle
import com.tangem.operations.attestation.AttestWalletKeyResponse
import com.tangem.operations.preflightread.PreflightReadFilter
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import io.mockk.CapturingSlot
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
internal class ColdWalletRegistrationSignerTest {

    init {
        CryptoUtils.initCrypto()
    }

    private val curve = EllipticCurve.Secp256k1
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val tangemSdk: TangemSdk = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk {
        every { sdk } returns tangemSdk
    }
    private val signer = ColdWalletRegistrationSigner(
        analyticsEventHandler = analyticsEventHandler,
        cardSdkConfigRepository = cardSdkConfigRepository,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val nonceBytes = "nonce-value".toByteArray()
    private val salt = ByteArray(SALT) { 7 }
    private val publicKeySalt = ByteArray(SALT) { 9 }

    private val walletPrivateKey = ByteArray(KEY) { 1 }
    private val walletPublicKey = generatePublicKey(walletPrivateKey, curve)
    private val cardPrivateKey = ByteArray(KEY) { 2 }
    private val cardPublicKey = generatePublicKey(cardPrivateKey, curve)

    private val baseCard = MockScanResponseFactory.create(
        cardConfig = GenericCardConfig(maxWalletCount = 1),
        derivedKeys = emptyMap(),
    ).card

    @BeforeEach
    fun setup() = clearMocks(analyticsEventHandler, tangemSdk)

    @Test
    fun `GIVEN card session signs WHEN own-session signer signs THEN returns the bundle for this wallet only`() =
        runTest {
            // Arrange
            val bundle = mockk<WalletSignatureBundle>()
            val preflightFilter = slot<PreflightReadFilter>()
            stubCardSession(
                preflightFilter = preflightFilter,
            ) { _, callback -> callback(CompletionResult.Success(bundle)) }

            // Act
            val actual = signer.signerFor(MockUserWalletFactory.create()).sign(nonceBytes)

            // Assert
            assertThat(actual).isSameInstanceAs(bundle)
            assertThat(preflightFilter.captured).isInstanceOf(UserWalletIdPreflightReadFilter::class.java)
        }

    @Test
    fun `GIVEN card session fails WHEN own-session signer signs THEN throws the card SDK error`() = runTest {
        // Arrange
        val error = TangemSdkError.UserCancelled()
        stubCardSession { _, callback -> callback(CompletionResult.Failure(error)) }

        // Act
        val actual = runSuspendCatching { signer.signerFor(MockUserWalletFactory.create()).sign(nonceBytes) }
            .exceptionOrNull()

        // Assert
        assertThat(actual).isSameInstanceAs(error)
    }

    @Test
    fun `GIVEN no card read WHEN own-session signer signs THEN fails with MissingPreflightRead`() = runTest {
        // Arrange
        val session = mockk<CardSession> { every { environment.card } returns null }
        stubCardSession { runnable, callback -> runnable.run(session, callback) }

        // Act
        val actual = runSuspendCatching { signer.signerFor(MockUserWalletFactory.create()).sign(nonceBytes) }
            .exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(TangemSdkError.MissingPreflightRead::class.java)
    }

    @Test
    fun `GIVEN attestation fails WHEN ColdWalletAttestationException created THEN keeps the card SDK error`() {
        // Arrange
        val error = TangemSdkError.UserCancelled()

        // Act
        val actual = ColdWalletAttestationException(error)

        // Assert
        assertThat(actual.error).isSameInstanceAs(error)
        assertThat(actual.cause).isSameInstanceAs(error)
    }

    @Test
    fun `firstSecpWalletPublicKey returns the first wallet key when it is secp256k1`() {
        val card = cardWith(wallet(EllipticCurve.Secp256k1, walletPublicKey), wallet(EllipticCurve.Ed25519))

        assertThat(signer.firstSecpWalletPublicKey(card)).isEqualTo(walletPublicKey)
    }

    @Test
    fun `firstSecpWalletPublicKey reports an exception event and aborts when the first wallet is not secp256k1`() {
        // Arrange — first wallet is Ed25519, not the expected secp256k1.
        val card = cardWith(wallet(EllipticCurve.Ed25519), wallet(EllipticCurve.Secp256k1, walletPublicKey))

        // Act
        val error = runCatching { signer.firstSecpWalletPublicKey(card) }.exceptionOrNull()

        // Assert — registration is aborted and the anomaly is reported.
        assertThat(error).isInstanceOf(ColdWalletUnexpectedCurveException::class.java)
        verify(exactly = 1) { analyticsEventHandler.send(any<BackendAuthAnalyticsEvent.UnexpectedWalletCurve>()) }
    }

    @Test
    fun `firstSecpWalletPublicKey aborts without reporting when the card has no wallets`() {
        val card = cardWith()

        val error = runCatching { signer.firstSecpWalletPublicKey(card) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `buildBundle maps card and wallet signatures to RSV and walletStatus to a single byte`() {
        // Real signatures over the exact backend preimages — proves the hash layout + RSV conversion.
        val status = CardWallet.Status.BackedUp // 0x82
        val walletSignature = (nonceBytes + salt).sign(walletPrivateKey, curve)
        val cardMessage = walletPublicKey + nonceBytes + publicKeySalt + byteArrayOf(status.code.toByte())
        val cardSignature = cardMessage.sign(cardPrivateKey, curve)
        val response = response(
            walletSignature = walletSignature,
            cardSignature = cardSignature,
            walletStatus = status,
        )

        val bundle = signer.buildBundle(response, walletPublicKey, cardPublicKey, nonceBytes)

        assertThat(bundle.walletSignature.size).isEqualTo(RSV)
        assertThat(bundle.cardSignature!!.size).isEqualTo(RSV)
        // v = recId + 27 (EVM-legacy), recId in 0..3.
        assertThat(bundle.walletSignature.last().toInt()).isAtLeast(EVM_V_OFFSET)
        assertThat(bundle.walletSignature.last().toInt()).isAtMost(EVM_V_OFFSET + 3)
        assertThat(bundle.cardSignature!!.last().toInt()).isAtLeast(EVM_V_OFFSET)
        assertThat(bundle.cardSignature!!.last().toInt()).isAtMost(EVM_V_OFFSET + 3)
        assertThat(bundle.walletSignatureSalt).isEqualTo(salt)
        assertThat(bundle.cardSignatureSalt).isEqualTo(publicKeySalt)
        assertThat(bundle.walletStatusByte).isEqualTo(0x82.toByte())
    }

    @Test
    fun `buildBundle maps SEED-imported walletStatus to 0xC2`() {
        val status = CardWallet.Status.BackedUpImported // 0xC2
        val cardMessage = walletPublicKey + nonceBytes + publicKeySalt + byteArrayOf(status.code.toByte())
        val bundle = signer.buildBundle(
            response = response(
                walletSignature = (nonceBytes + salt).sign(walletPrivateKey, curve),
                cardSignature = cardMessage.sign(cardPrivateKey, curve),
                walletStatus = status,
            ),
            walletPublicKey = walletPublicKey,
            cardPublicKey = cardPublicKey,
            nonceBytes = nonceBytes,
        )

        assertThat(bundle.walletStatusByte).isEqualTo(0xC2.toByte())
    }

    @Test
    fun `buildBundle without walletStatus excludes it from the card preimage and leaves the byte null`() {
        // COS < 6 (e.g. Wallet 1): walletStatus is null, so the card signs
        // walletPublicKey | challenge | publicKeySalt WITHOUT the status byte.
        val cardMessage = walletPublicKey + nonceBytes + publicKeySalt
        val bundle = signer.buildBundle(
            response = response(
                walletSignature = (nonceBytes + salt).sign(walletPrivateKey, curve),
                cardSignature = cardMessage.sign(cardPrivateKey, curve),
                walletStatus = null,
            ),
            walletPublicKey = walletPublicKey,
            cardPublicKey = cardPublicKey,
            nonceBytes = nonceBytes,
        )

        assertThat(bundle.cardSignature!!.size).isEqualTo(RSV)
        assertThat(bundle.cardSignature!!.last().toInt()).isAtLeast(EVM_V_OFFSET)
        assertThat(bundle.cardSignature!!.last().toInt()).isAtMost(EVM_V_OFFSET + 3)
        assertThat(bundle.cardSignatureSalt).isEqualTo(publicKeySalt)
        assertThat(bundle.walletStatusByte).isNull()
    }

    @Test
    fun `buildBundle without a card signature returns a wallet-signature-only bundle (treated as hot)`() {
        // COS < 2.01: no card signature — register with the wallet signature only, no card fields.
        val bundle = signer.buildBundle(
            response = response(
                walletSignature = (nonceBytes + salt).sign(walletPrivateKey, curve),
                cardSignature = null,
                walletStatus = null,
            ),
            walletPublicKey = walletPublicKey,
            cardPublicKey = cardPublicKey,
            nonceBytes = nonceBytes,
        )

        assertThat(bundle.walletSignature.size).isEqualTo(RSV)
        assertThat(bundle.walletSignatureSalt).isEqualTo(salt)
        assertThat(bundle.cardSignature).isNull()
        assertThat(bundle.cardSignatureSalt).isNull()
        assertThat(bundle.walletStatusByte).isNull()
    }

    private fun cardWith(vararg wallets: CardDTO.Wallet): CardDTO =
        baseCard.copy(cardId = "CARD", wallets = wallets.toList())

    private fun wallet(curve: EllipticCurve, publicKey: ByteArray? = ByteArray(PUBLIC_KEY) { 4 }): CardDTO.Wallet =
        baseCard.wallets.first().copy(curve = curve, publicKey = publicKey)

    private fun response(
        walletSignature: ByteArray,
        cardSignature: ByteArray?,
        walletStatus: CardWallet.Status?,
    ) = AttestWalletKeyResponse(
        cardId = "CARD",
        salt = salt,
        walletSignature = walletSignature,
        challenge = nonceBytes,
        cardSignature = cardSignature,
        publicKeySalt = publicKeySalt,
        walletStatus = walletStatus,
        counter = null,
    )

    private fun stubCardSession(
        preflightFilter: CapturingSlot<PreflightReadFilter> = slot(),
        session: (
            runnable: CardSessionRunnable<WalletSignatureBundle>,
            callback: (CompletionResult<WalletSignatureBundle>) -> Unit,
        ) -> Unit,
    ) {
        val runnable = slot<CardSessionRunnable<WalletSignatureBundle>>()
        every {
            tangemSdk.startSessionWithRunnable(
                runnable = capture(runnable),
                cardId = any(),
                initialMessage = any(),
                accessCode = any(),
                preflightReadFilter = capture(preflightFilter),
                iconScanRes = any(),
                callback = any(),
            )
        } answers {
            session(runnable.captured, lastArg())
        }
    }

    private companion object {
        const val KEY = 32
        const val SALT = 16
        const val RSV = 65
        const val EVM_V_OFFSET = 27
        const val PUBLIC_KEY = 65
    }
}