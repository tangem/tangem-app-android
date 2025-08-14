package com.tangem.features.account.details.entity

import com.tangem.features.account.common.CryptoPortfolioIconUM

data class AccountDetailsUM(
    val accountName: String,
    val accountIcon: CryptoPortfolioIconUM,
    val onCloseClick: () -> Unit,
    val onAccountEditClick: () -> Unit,
    val onManageTokensClick: () -> Unit,
    val onArchiveAccountClick: () -> Unit,
)