package com.tangem.domain.wallets.models.backup

/**
 * Error received while performing a card command during backup. Reported alongside the card so support can
 * diagnose an interrupted backup from the backend logs.
 *
 * @property code    code of the error
 * @property message text of the error
 */
data class CardBackupError(
    val code: String?,
    val message: String?,
)