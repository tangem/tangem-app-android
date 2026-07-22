package com.tangem.domain.addressbook.model

import com.tangem.domain.addressbook.model.AddressBookBlob.Companion.CURRENT_VERSION
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Self-describing envelope around an AES-256-GCM encrypted [AddressBook]. Produced and consumed by
 * [com.tangem.domain.addressbook.crypto.AddressBookCipher]; safe to persist or sync off-device.
 *
 * The [ciphertext] holds the serialized [AddressBook]; the metadata ([walletId], [updatedAt]) stays
 * in clear text so the blob can be routed/sorted without decrypting it. [nonce], [ciphertext] and
 * [authTag] are lowercase hex strings.
 *
 * Being `@Serializable`, the blob serializes into exactly:
 * ```json
 * {
 *   "version": "1.0",
 *   "walletId": "…",
 *   "updatedAt": "2026-05-22T09:00:00.000Z",
 *   "nonce": "…",
 *   "ciphertext": "…",
 *   "authTag": "…"
 * }
 * ```
 */
@Serializable
data class AddressBookBlob(
    @SerialName("version")
    val version: String = CURRENT_VERSION,
    @SerialName("walletId")
    val walletId: String,
    @SerialName("updatedAt")
    val updatedAt: String,
    @SerialName("nonce")
    val nonce: String,
    @SerialName("ciphertext")
    val ciphertext: String,
    @SerialName("authTag")
    val authTag: String,
) {

    /**
     * Whether this build can safely read and write the blob — i.e. its [version] is not newer than the
     * contract this app supports ([CURRENT_VERSION]). See [isVersionCompatible].
     */
    val isVersionCompatible: Boolean get() = isVersionCompatible(version)

    companion object {
        const val CURRENT_VERSION = "1.0"

        /**
         * A blob is compatible when its contract version is **not higher** than [CURRENT_VERSION]. The version
         * is treated as a plain number (major/minor are not distinguished — any higher value is incompatible):
         * - lower (`0.9`) — the app understands a newer contract; reads work and a write upgrades the backend
         *   copy to [CURRENT_VERSION].
         * - equal — same contract.
         * - higher (`1.1`, `2.0`) — the backend contract is newer than this build understands, so the book must
         *   be treated as read-only-opaque (not read, not written).
         *
         * A version that cannot be parsed as a number is treated as incompatible — safer to refuse a blob we
         * cannot reason about.
         */
        fun isVersionCompatible(version: String): Boolean {
            val parsed = version.trim().toDoubleOrNull() ?: return false
            return parsed <= CURRENT_VERSION.toDouble()
        }
    }
}