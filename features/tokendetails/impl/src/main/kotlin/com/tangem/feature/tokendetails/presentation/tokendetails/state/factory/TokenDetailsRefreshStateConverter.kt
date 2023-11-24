package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class TokenDetailsRefreshStateConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
) : Converter<Boolean, TokenDetailsState> {

    override fun convert(value: Boolean): TokenDetailsState {
        val state = currentStateProvider()
        return state.createPullToRefresh(value)
    }

    private fun TokenDetailsState.createPullToRefresh(isRefreshing: Boolean): TokenDetailsState {
        return copy(pullToRefreshConfig = pullToRefreshConfig.copy(isRefreshing = isRefreshing))
    }
}