package com.tangem.feature.tokendetails.presentation.tokendetails.state.components

data class TokenDetailsPullToRefreshConfig(val isRefreshing: Boolean, val onRefresh: () -> Unit)