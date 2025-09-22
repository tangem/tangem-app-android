package com.tangem.features.account.details.entity

import com.tangem.common.ui.account.CryptoPortfolioIconUM
import com.tangem.core.ui.extensions.TextReference

internal data class AccountDetailsUM(
    val accountName: TextReference,
    val accountIcon: CryptoPortfolioIconUM,
    val archiveMode: ArchiveMode,
    val onCloseClick: () -> Unit,
    val onAccountEditClick: () -> Unit,
    val onManageTokensClick: () -> Unit,
) {

    sealed interface ArchiveMode {
        data object None : ArchiveMode
        data class Available(
            val onArchiveAccountClick: () -> Unit,
        ) : ArchiveMode
    }
}