package com.tangem.data.cloudbackup.crypto

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.CharBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.text.Normalizer
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts/decrypts the backup payload with a password: Argon2id key stretching (RFC 9106 primary
 * variant — memory-hard and side-channel resistant) + AES-256-GCM authenticated encryption.
 *
 * GCM is authenticated, so a wrong password or a tampered file is detected by the tag check during
 * decryption — no separate MAC is stored. Argon2id is a standard, so a matching iOS side (e.g. libsodium
 * `crypto_pwhash`) interoperates given identical parameters (type=id, version=0x13, parallelism=1,
 * memory in KiB) and the same NFC password normalization (CPR-06). The result is wrapped by [CloudBackupFileData] into a small JSON keystore whose
 * metadata (wallet name, date) stays readable without the password.
 */
internal class CloudBackupCipher(
    private val random: SecureRandom = SecureRandom(),
) {

    /**
     * Encrypts [secret] with [password].
     *
     * @param metadata plaintext file metadata (wallet name, id, creation date) stored unencrypted so the
     * backups list can be rendered before decryption — required so a backup is never uploaded without it
     * @param params Argon2id difficulty, [Argon2Params.Default] by default
     */
    fun encrypt(
        secret: ByteArray,
        password: CharArray,
        metadata: Metadata,
        params: Argon2Params = Argon2Params.Default,
    ): CloudBackupFileData {
        val salt = randomBytes(SALT_SIZE_BYTES)
        val nonce = randomBytes(GCM_NONCE_SIZE_BYTES)

        val key = deriveArgon2id(
            password = password,
            salt = salt,
            params = params,
            version = ARGON2_VERSION,
            dklen = DERIVED_KEY_SIZE_BYTES,
        )
        // AES-GCM appends the authentication tag to the ciphertext; the spec stores it separately
        val sealed = try {
            aesGcm(mode = Cipher.ENCRYPT_MODE, key = key, iv = nonce, input = secret)
        } finally {
            key.fill(0)
        }
        val ciphertext = sealed.copyOfRange(0, sealed.size - GCM_TAG_SIZE_BYTES)
        val tag = sealed.copyOfRange(sealed.size - GCM_TAG_SIZE_BYTES, sealed.size)

        return CloudBackupFileData(
            version = VERSION,
            id = UUID.randomUUID().toString(),
            name = metadata.name,
            walletId = metadata.walletId,
            createdAt = metadata.createdAt,
            crypto = CloudBackupFileData.CryptoData(
                cipher = CIPHER_AES_256_GCM,
                cipherparams = CloudBackupFileData.CipherParams(nonce = nonce.toHex()),
                ciphertext = ciphertext.toHex(),
                tag = tag.toHex(),
                kdf = KDF_ARGON2ID,
                kdfparams = CloudBackupFileData.KdfParams(
                    version = ARGON2_VERSION,
                    memory = params.memoryKib,
                    iterations = params.iterations,
                    parallelism = params.parallelism,
                    dklen = DERIVED_KEY_SIZE_BYTES,
                    salt = salt.toHex(),
                ),
            ),
        )
    }

    /** Decrypts [data] with [password]; the GCM tag check authenticates the payload */
    fun decrypt(data: CloudBackupFileData, password: CharArray): Either<CloudBackupCryptoError, ByteArray> = either {
        ensure(data.version == VERSION) {
            CloudBackupCryptoError.InvalidFormat("Unsupported version: ${data.version}")
        }
        ensure(data.crypto.cipher == CIPHER_AES_256_GCM) {
            CloudBackupCryptoError.InvalidFormat("Unsupported cipher: ${data.crypto.cipher}")
        }
        ensure(data.crypto.kdf == KDF_ARGON2ID) {
            CloudBackupCryptoError.InvalidFormat("Unsupported kdf: ${data.crypto.kdf}")
        }

        val kdfparams = data.crypto.kdfparams
        val salt = ensureNotNull(kdfparams.salt.hexToBytesOrNull()) {
            CloudBackupCryptoError.InvalidFormat("Malformed hex: salt")
        }
        val nonce = ensureNotNull(data.crypto.cipherparams.nonce.hexToBytesOrNull()) {
            CloudBackupCryptoError.InvalidFormat("Malformed hex: nonce")
        }
        val ciphertext = ensureNotNull(data.crypto.ciphertext.hexToBytesOrNull()) {
            CloudBackupCryptoError.InvalidFormat("Malformed hex: ciphertext")
        }
        val tag = ensureNotNull(data.crypto.tag.hexToBytesOrNull()) {
            CloudBackupCryptoError.InvalidFormat("Malformed hex: tag")
        }

        validateArgon2Params(kdfparams)

        val key = catchingCrypto("kdf parameters") {
            deriveArgon2id(
                password = password,
                salt = salt,
                params = Argon2Params(
                    memoryKib = kdfparams.memory,
                    iterations = kdfparams.iterations,
                    parallelism = kdfparams.parallelism,
                ),
                version = kdfparams.version,
                dklen = kdfparams.dklen,
            )
        }

        // GCM verifies the tag while decrypting: a wrong password or a tampered file fails the tag check.
        // Java's AES-GCM expects the tag appended to the ciphertext, so re-join them.
        try {
            aesGcm(mode = Cipher.DECRYPT_MODE, key = key, iv = nonce, input = ciphertext + tag)
        } catch (e: AEADBadTagException) {
            raise(CloudBackupCryptoError.WrongPassword)
        } catch (e: GeneralSecurityException) {
            raise(CloudBackupCryptoError.InvalidFormat("Decryption failed: ${e.message}"))
        } finally {
            key.fill(0)
        }
    }

    // untrusted params — reject out-of-range values before the KDF to avoid OOM / negative-size crashes
    private fun Raise<CloudBackupCryptoError>.validateArgon2Params(params: CloudBackupFileData.KdfParams) {
        ensure(params.version == ARGON2_VERSION) {
            CloudBackupCryptoError.InvalidFormat("Unsupported argon2 version: ${params.version}")
        }
        ensure(params.dklen in MIN_DKLEN..MAX_DKLEN) {
            CloudBackupCryptoError.InvalidFormat("Invalid dklen: ${params.dklen}")
        }
        ensure(params.memory in MIN_MEMORY_KIB..MAX_MEMORY_KIB) {
            CloudBackupCryptoError.InvalidFormat("Invalid memory: ${params.memory}")
        }
        ensure(params.iterations in MIN_ITERATIONS..MAX_ITERATIONS) {
            CloudBackupCryptoError.InvalidFormat("Invalid iterations: ${params.iterations}")
        }
        ensure(params.parallelism in MIN_PARALLELISM..MAX_PARALLELISM) {
            CloudBackupCryptoError.InvalidFormat("Invalid parallelism: ${params.parallelism}")
        }
    }

    /**
     * Runs a crypto primitive over parameters taken from an untrusted file and maps a rejection
     * (e.g. out-of-range Argon2 params, bad key/iv sizes) to [CloudBackupCryptoError.InvalidFormat]
     * so a malformed backup fails gracefully instead of crashing the caller.
     */
    private inline fun <T> Raise<CloudBackupCryptoError>.catchingCrypto(what: String, block: () -> T): T {
        return try {
            block()
        } catch (e: GeneralSecurityException) {
            raise(CloudBackupCryptoError.InvalidFormat("Invalid $what: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            raise(CloudBackupCryptoError.InvalidFormat("Invalid $what: ${e.message}"))
        }
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    private fun deriveArgon2id(
        password: CharArray,
        salt: ByteArray,
        params: Argon2Params,
        version: Int,
        dklen: Int,
    ): ByteArray {
        val argonParams = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(version)
            .withSalt(salt)
            .withMemoryAsKB(params.memoryKib)
            .withIterations(params.iterations)
            .withParallelism(params.parallelism)
            .build()
        val generator = Argon2BytesGenerator().apply { init(argonParams) }
        val out = ByteArray(dklen)
        val passwordBytes = password.toNfcUtf8Bytes()
        try {
            generator.generateBytes(passwordBytes, out)
        } finally {
            passwordBytes.fill(0)
        }
        return out
    }

    private fun aesGcm(mode: Int, key: ByteArray, iv: ByteArray, input: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(mode, SecretKeySpec(key, KEY_ALGORITHM_AES), GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
        return cipher.doFinal(input)
    }

    /**
     * NFC-normalizes (CPR-06) then UTF-8 encodes the password so a passphrase entered in a different
     * Unicode form (e.g. "é" as U+00E9 vs "e" + combining U+0301) derives the same key across platforms.
     *
     * Fast path avoids materializing a String: for already-NFC input (all ASCII passwords) we encode the
     * [CharArray] directly, keeping it wipeable. Only genuinely denormalized input takes the rare fallback
     * where [Normalizer] has no CharArray API and a short-lived String is unavoidable.
     */
    private fun CharArray.toNfcUtf8Bytes(): ByteArray {
        val buffer = CharBuffer.wrap(this)
        val normalized: CharSequence = if (Normalizer.isNormalized(buffer, Normalizer.Form.NFC)) {
            buffer
        } else {
            Normalizer.normalize(buffer, Normalizer.Form.NFC)
        }
        val encoded = Charsets.UTF_8.encode(CharBuffer.wrap(normalized))
        val result = ByteArray(encoded.remaining()).also(encoded::get)
        if (encoded.hasArray()) encoded.array().fill(0)
        return result
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and BYTE_MASK)
    }

    private fun String.hexToBytesOrNull(): ByteArray? {
        // restrict to ASCII 0-9/a-f: Char.isDigit() also accepts non-ASCII Unicode digits,
        // which would then blow up toInt(radix = 16) with an uncaught NumberFormatException
        val isValid = length % 2 == 0 && all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }
        if (!isValid) return null
        return chunked(size = 2) { it.toString().toInt(radix = 16).toByte() }.toByteArray()
    }

    /**
     * Plaintext backup metadata kept outside the encrypted payload (see [CloudBackupFileData]) so the

     */
    data class Metadata(
        val name: String,
        val walletId: String,
        val createdAt: String,
    )

    /**
     * Argon2id difficulty parameters.
     *
     * @property memoryKib   memory cost in KiB (libsodium memlimit / 1024)
     * @property iterations  time cost / passes (libsodium opslimit)
     * @property parallelism lanes; kept at 1 for libsodium `crypto_pwhash` interop
     */
    data class Argon2Params(
        val memoryKib: Int,
        val iterations: Int,
        val parallelism: Int,
    ) {

        companion object {
            /** ~64 MiB, 3 passes, 1 lane — a libsodium crypto_pwhash-compatible preset */
            val Default = Argon2Params(memoryKib = 65_536, iterations = 3, parallelism = 1)
        }
    }

    companion object {
        const val CIPHER_AES_256_GCM = "aes-256-gcm"
        const val KDF_ARGON2ID = "argon2id"
        const val VERSION = 1

        private const val DERIVED_KEY_SIZE_BYTES = 32
        private const val SALT_SIZE_BYTES = 16
        private const val GCM_NONCE_SIZE_BYTES = 12
        private const val GCM_TAG_SIZE_BITS = 128
        private const val GCM_TAG_SIZE_BYTES = GCM_TAG_SIZE_BITS / 8
        private val ARGON2_VERSION = Argon2Parameters.ARGON2_VERSION_13
        private const val KEY_ALGORITHM_AES = "AES"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val BYTE_MASK = 0xFF

        private const val MIN_DKLEN = 16
        private const val MAX_DKLEN = 64
        private const val MIN_MEMORY_KIB = 8
        private const val MAX_MEMORY_KIB = 262_144
        private const val MIN_ITERATIONS = 1
        private const val MAX_ITERATIONS = 64
        private const val MIN_PARALLELISM = 1
        private const val MAX_PARALLELISM = 64
    }
}