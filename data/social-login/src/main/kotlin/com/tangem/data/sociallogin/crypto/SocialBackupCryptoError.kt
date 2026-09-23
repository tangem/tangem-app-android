package com.tangem.data.sociallogin.crypto

/** Why a `social-backup.json` could not be produced or consumed */
internal sealed interface SocialBackupCryptoError {

    /**
     * Kept apart from [InvalidFormat]: it has to be rejected before decryption, and an
     * unreadable version is an app problem rather than a broken backup, so the message differs.
     */
    data class UnsupportedVersion(val version: Int) : SocialBackupCryptoError

    /** Malformed structure, or a failed AES-GCM authentication — indistinguishable by design */
    data class InvalidFormat(val reason: String) : SocialBackupCryptoError
}