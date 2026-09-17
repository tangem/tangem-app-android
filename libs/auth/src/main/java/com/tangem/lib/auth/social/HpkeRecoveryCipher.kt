package com.tangem.lib.auth.social

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.hpke.HPKE

/**
 * HPKE (RFC 9180) base mode over `DHKEM(X25519, HKDF-SHA256) / HKDF-SHA256 / AES-256-GCM`, the envelope
 * the recovery key travels in.
 *
 * X25519 rather than P-256: a 32-byte public key has no point format to agree on and no validity check
 * to get wrong, which is what keeps Android, iOS and the backend from drifting apart.
 *
 * The recipient key pair is ephemeral and in-memory — the API levels this app supports offer no way to
 * decrypt with an Android Keystore EC key, so the device key cannot serve here. The AAD is empty
 * because DHKEM already binds the recipient key through the KEM context; [INFO] carries the domain
 * separation instead.
 */
internal object HpkeRecoveryCipher {

    /** Byte-identical on Android, iOS and the backend */
    private val INFO = "tangem/social-recovery/v1".toByteArray(Charsets.UTF_8)

    private val EMPTY_AAD = ByteArray(0)

    const val PUBLIC_KEY_SIZE_BYTES = 32

    private fun hpke(): HPKE = HPKE(
        HPKE.mode_base,
        HPKE.kem_X25519_SHA256,
        HPKE.kdf_HKDF_SHA256,
        HPKE.aead_AES_GCM256,
    )

    fun generateKeyPair(): AsymmetricCipherKeyPair = hpke().generatePrivateKey()

    fun serializePublicKey(keyPair: AsymmetricCipherKeyPair): ByteArray = hpke().serializePublicKey(keyPair.public)

    fun seal(recipientPublicKey: ByteArray, plaintext: ByteArray): HpkeEnvelope {
        val hpke = hpke()
        val sealed = hpke.seal(
            hpke.deserializePublicKey(recipientPublicKey),
            INFO,
            EMPTY_AAD,
            plaintext,
            null,
            null,
            null,
        )
        // BouncyCastle hands back [ciphertext, encapsulation], the reverse of the wire order
        return HpkeEnvelope(enc = sealed[1], ct = sealed[0])
    }

    /** Throws when the envelope does not authenticate */
    fun open(recipientKeyPair: AsymmetricCipherKeyPair, envelope: HpkeEnvelope): ByteArray = hpke().open(
        envelope.enc,
        recipientKeyPair,
        INFO,
        EMPTY_AAD,
        envelope.ct,
        null,
        null,
        null,
    )
}

/**
 * @property enc encapsulated KEM key, 32 bytes for X25519
 * @property ct  ciphertext with the 16-byte GCM tag appended
 */
internal class HpkeEnvelope(val enc: ByteArray, val ct: ByteArray)