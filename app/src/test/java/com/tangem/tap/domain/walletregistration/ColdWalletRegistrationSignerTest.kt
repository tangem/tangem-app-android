package com.tangem.tap.domain.walletregistration

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.CryptoUtils.generatePublicKey
import com.tangem.crypto.sign
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.operations.attestation.AttestWalletKeyResponse
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
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
    private val signer = ColdWalletRegistrationSigner(analyticsEventHandler)

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
    fun setup() = clearMocks(analyticsEventHandler)

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

    private companion object {
        const val KEY = 32
        const val SALT = 16
        const val RSV = 65
        const val EVM_V_OFFSET = 27
        const val PUBLIC_KEY = 65
    }
}