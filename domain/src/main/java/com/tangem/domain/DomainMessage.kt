package com.tangem.domain

/**
[REDACTED_AUTHOR]
 */
sealed interface DomainMessage

sealed interface DomainNotification : DomainMessage {
    interface Toast : DomainNotification {}

    interface Snackbar : DomainNotification {}

    interface Dialog : DomainNotification {}

}