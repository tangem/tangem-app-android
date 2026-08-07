package com.tangem.tap.domain.walletregistration

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.CryptoUtils.generatePublicKey
import com.tangem.crypto.sign
import com.tangem.operations.attestation.AttestWalletKeyResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ColdWalletRegistrationSignerTest {

    init {
        CryptoUtils.initCrypto()
    }

    private val curve = EllipticCurve.Secp256k1
    private val signer = ColdWalletRegistrationSigner()

    private val nonceBytes = "nonce-value".toByteArray()
    private val salt = ByteArray(SALT) { 7 }
    private val publicKeySalt = ByteArray(SALT) { 9 }

    private val walletPrivateKey = ByteArray(KEY) { 1 }
    private val walletPublicKey = generatePublicKey(walletPrivateKey, curve)
    private val cardPrivateKey = ByteArray(KEY) { 2 }
    private val cardPublicKey = generatePublicKey(cardPrivateKey, curve)

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
    }
}