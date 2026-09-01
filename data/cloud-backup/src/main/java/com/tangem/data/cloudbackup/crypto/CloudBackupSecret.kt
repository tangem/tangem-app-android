package com.tangem.data.cloudbackup.crypto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.CharBuffer

/**
 * Decrypted cloud-backup payload — the plaintext that sits INSIDE [CloudBackupFileData.crypto].
 *
 * This is the cross-platform "secret" contract (UC-07 §4.1): both platforms must produce and parse
 * exactly this JSON, otherwise decryption succeeds but parsing fails.
 *
 * The BIP39 passphrase itself is deliberately not backed up — it never leaves the device (CPR-07). Only
 * [passphraseRequired] travels with the backup, so the restore flow knows to ask for it; it is mandatory,
 * hence no default value.
 *
 * @property mnemonic           space-separated BIP39 words
 * @property passphraseRequired [PASSPHRASE_REQUIRED] for a wallet with a passphrase, [PASSPHRASE_NOT_REQUIRED] without
 */
@Serializable
internal class CloudBackupSecret(
    @SerialName("mnemonic") @Serializable(with = MnemonicSerializer::class) val mnemonic: CharArray,
    @SerialName("passphraseRequired") val passphraseRequired: Int,
) {

    /** `null` when [passphraseRequired] carries a value outside of the contract */
    val isPassphraseRequired: Boolean?
        get() = when (passphraseRequired) {
            PASSPHRASE_REQUIRED -> true
            PASSPHRASE_NOT_REQUIRED -> false
            else -> null
        }

    companion object {

        const val PASSPHRASE_REQUIRED = 1
        const val PASSPHRASE_NOT_REQUIRED = 0

        private val PREFIX = """{"mnemonic":"""".toByteArray(Charsets.US_ASCII)
        private val INFIX = """","passphraseRequired":""".toByteArray(Charsets.US_ASCII)
        private val SUFFIX = "}".toByteArray(Charsets.US_ASCII)

        private const val ASCII_ZERO = '0'.code.toByte()

        /**
         * Serializes the payload straight into bytes, so the mnemonic never becomes an immutable String
         * on the way out — unlike `encodeToString`, whose result could not be wiped. The output matches
         * the contract byte for byte (see `CloudBackupSecretTest`); the caller wipes the returned array.
         *
         * Returns `null` when [mnemonic] holds a character that JSON would require escaping, which no
         * BIP39 word list contains — such input is not a mnemonic and must not be silently mangled.
         */
        fun encode(mnemonic: CharArray, isPassphraseRequired: Boolean): ByteArray? {
            if (mnemonic.any { it == '"' || it == '\\' || it < ' ' }) return null

            val mnemonicBytes = mnemonic.encodeToUtf8()
            val flag = if (isPassphraseRequired) PASSPHRASE_REQUIRED else PASSPHRASE_NOT_REQUIRED
            val payload = ByteArray(PREFIX.size + mnemonicBytes.size + INFIX.size + 1 + SUFFIX.size)

            var offset = 0
            for (part in arrayOf(PREFIX, mnemonicBytes, INFIX)) {
                part.copyInto(payload, offset)
                offset += part.size
            }
            payload[offset++] = (ASCII_ZERO + flag).toByte()
            SUFFIX.copyInto(payload, offset)

            mnemonicBytes.fill(0)
            return payload
        }

        private fun CharArray.encodeToUtf8(): ByteArray {
            val encoded = Charsets.UTF_8.encode(CharBuffer.wrap(this))
            val bytes = ByteArray(encoded.remaining())
            encoded.get(bytes)
            if (encoded.hasArray()) encoded.array().fill(0)
            return bytes
        }
    }
}

/**
 * Reads the mnemonic into a [CharArray] instead of keeping the String the JSON parser produces, so
 * nothing but the wipeable copy outlives parsing.
 */
internal object MnemonicSerializer : KSerializer<CharArray> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("mnemonic", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CharArray = decoder.decodeString().toCharArray()

    override fun serialize(encoder: Encoder, value: CharArray) = encoder.encodeString(String(value))
}