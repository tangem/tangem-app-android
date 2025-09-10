package com.tangem.features.account.details.entity

import com.tangem.common.ui.account.CryptoPortfolioIconUM
import com.tangem.core.ui.extensions.TextReference

internal data class AccountDetailsUM(
    val accountName: TextReference,
    val accountIcon: CryptoPortfolioIconUM,
    val onCloseClick: () -> Unit,
    val onAccountEditClick: () -> Unit,
    val onManageTokensClick: () -> Unit,
    val onArchiveAccountClick: () -> Unit,
)