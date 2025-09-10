package com.tangem.features.account.details.entity

import com.tangem.common.ui.account.CryptoPortfolioIconUM

internal data class AccountDetailsUM(
    val accountName: String,
    val accountIcon: CryptoPortfolioIconUM,
    val onCloseClick: () -> Unit,
    val onAccountEditClick: () -> Unit,
    val onManageTokensClick: () -> Unit,
    val onArchiveAccountClick: () -> Unit,
)