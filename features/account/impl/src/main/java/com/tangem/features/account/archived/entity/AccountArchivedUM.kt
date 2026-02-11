package com.tangem.features.account.archived.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
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
    val accountName: TextReference,
    val accountIconUM: AccountIconUM.CryptoPortfolio,
    val tokensInfo: TextReference,
    val networksInfo: TextReference,
    val isLoading: Boolean,
    val onClick: () -> Unit,
)