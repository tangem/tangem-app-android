package com.tangem.domain.addressbook.model

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
 *   "auth_tag": "…"
 * }
 * ```
 */
@Serializable
data class AddressBookBlob(
    val version: String = CURRENT_VERSION, // TODO Will come from BE in [REDACTED_TASK_KEY]
    val walletId: String,
    val updatedAt: String,
    val nonce: String,
    val ciphertext: String,
    @SerialName("auth_tag") val authTag: String,
) {

    companion object {
        const val CURRENT_VERSION = "1.0"
    }
}