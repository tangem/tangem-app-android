package com.tangem.features.account.archived.entity

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.account.common.CryptoPortfolioIconUM
import kotlinx.collections.immutable.ImmutableList

internal sealed interface AccountArchivedUM {
    val onCloseClick: () -> Unit

    data class Loading(override val onCloseClick: () -> Unit) : AccountArchivedUM
    data class Error(
        override val onCloseClick: () -> Unit,
        val onRetryClick: () -> Unit,
    ) : AccountArchivedUM
    data class Content(
        override val onCloseClick: () -> Unit,
        val accounts: ImmutableList<ArchivedAccountUM>,
    ) : AccountArchivedUM
}

internal data class ArchivedAccountUM(
    val accountId: String,
    val accountName: String,
    val accountIcon: CryptoPortfolioIconUM,
    val tokensInfo: TextReference,
    val onClick: (accountId: String) -> Unit,
)