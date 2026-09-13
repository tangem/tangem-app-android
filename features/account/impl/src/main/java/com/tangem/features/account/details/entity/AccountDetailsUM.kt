package com.tangem.features.account.details.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.TextReference

internal data class AccountDetailsUM(
    val accountName: TextReference,
    val accountIcon: AccountIconUM.CryptoPortfolio,
    val archiveMode: ArchiveMode,
    val isManageTokensAvailable: Boolean,
    val onCloseClick: () -> Unit,
    val onManageTokensClick: () -> Unit,
    val onAccountEditClick: (() -> Unit)?,
    val members: MembersRowUM? = null,
    val networksInfo: NetworksInfoUM? = null,
) {

    @Immutable
    sealed interface ArchiveMode {
        data object None : ArchiveMode
        data class Available(
            val onArchiveAccountClick: () -> Unit,
            val isLoading: Boolean,
        ) : ArchiveMode
    }

    data class MembersRowUM(
        val title: TextReference,
        val membersCount: TextReference,
        val onClick: () -> Unit,
    )

    data class NetworksInfoUM(
        val text: TextReference,
        val linkText: TextReference,
        val onLinkClick: () -> Unit,
    )
}