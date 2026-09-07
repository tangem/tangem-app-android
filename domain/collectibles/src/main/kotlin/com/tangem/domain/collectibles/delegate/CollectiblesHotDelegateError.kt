package com.tangem.domain.collectibles.delegate

sealed interface CollectiblesHotDelegateError {

    /** No delegate yet — call [CollectiblesHotDelegate.provision] first. */
    data object NotProvisioned : CollectiblesHotDelegateError

    data class KeyCreationFailed(val cause: Throwable) : CollectiblesHotDelegateError

    data class SigningFailed(val cause: Throwable) : CollectiblesHotDelegateError
}