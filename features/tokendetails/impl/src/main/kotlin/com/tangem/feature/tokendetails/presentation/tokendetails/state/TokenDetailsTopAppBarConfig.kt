package com.tangem.feature.tokendetails.presentation.tokendetails.state

internal data class TokenDetailsTopAppBarConfig(
    val onBackClick: () -> Unit,
    val tokenDetailsAppBarMenuConfig: TokenDetailsAppBarMenuConfig?,
)