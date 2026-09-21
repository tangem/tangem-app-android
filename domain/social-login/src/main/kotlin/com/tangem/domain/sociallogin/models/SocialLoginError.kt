package com.tangem.domain.sociallogin.models

/** Errors of social login: provider sign-in, backend recovery binding and cloud backup storage */
sealed interface SocialLoginError {

    
    data object SignInCanceled : SocialLoginError

    
    data class SignInFailed(val cause: Throwable? = null) : SocialLoginError

    /** No provider account to sign in with — the way out is adding one, not retrying */
    data object NoProviderAccount : SocialLoginError

    /** The provider cannot be used on this device — e.g. no Google services (FR-13) */
    data object ProviderUnavailable : SocialLoginError

    
    data object CloudUnavailable : SocialLoginError

    /** Could not reach the backend or the cloud storage because of connectivity issues */
    data object NetworkError : SocialLoginError

    /** No wallet is bound to this provider identity, so the flow must create one instead (FR-02) */
    data object BindingNotFound : SocialLoginError

    /**

     * (FR-14): the identity already points at another wallet, or the wallet at another identity.
     */
    data object BindingConflict : SocialLoginError

    
    data class RecoveryKeyDecryptionFailed(val cause: Throwable? = null) : SocialLoginError

    
    data object BackupNotFound : SocialLoginError

    /**
     * The file is not a usable social backup: a malformed structure or a failed AES-GCM authentication —
     * a wrong recovery key and a tampered file are indistinguishable here by design (CPR-03).
     */
    data object InvalidBackupFile : SocialLoginError

    /** The file declares a format version this app cannot read (CPR-05) */
    data class UnsupportedBackupVersion(val version: Int) : SocialLoginError

    /** The backup needs a passphrase a social restore cannot ask for; the seed alone builds another wallet */
    data object PassphraseNotSupported : SocialLoginError

    
    data class BackupWriteFailed(val cause: Throwable? = null) : SocialLoginError

    
    data class ServerError(val cause: Throwable? = null) : SocialLoginError

    /**
     * Reading or writing the local social recovery state failed. The stored flag keeps its previous
     * value, so the operation that was recording progress must not report success.
     */
    data class PersistenceFailed(val cause: Throwable? = null) : SocialLoginError

    data class Unknown(val cause: Throwable? = null) : SocialLoginError
}