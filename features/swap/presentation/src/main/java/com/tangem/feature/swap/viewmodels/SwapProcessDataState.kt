package com.tangem.feature.swap.viewmodels

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.RequestApproveStateData
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokensDataStateExpress
import com.tangem.feature.swap.domain.models.ui.TxFee
import com.tangem.feature.swap.models.ApproveType

data class SwapProcessDataState(
    // Initial network id
    val networkId: String,
    val fromCryptoCurrency: CryptoCurrencyStatus? = null,
    val toCryptoCurrency: CryptoCurrencyStatus? = null,
    // Amount from input
    val amount: String? = null,
    val approveDataModel: RequestApproveStateData? = null,
    val approveType: ApproveType? = null,
    val swapDataModel: SwapDataModel? = null,
    val selectedFee: TxFee? = null, // todo
    val tokensDataState: TokensDataStateExpress? = null,
    val selectedProvider: SwapProvider? = null,
    val lastLoadedSwapStates: Map<SwapProvider, SwapState> = emptyMap(),
) {

    fun getCurrentLoadedSwapState(): SwapState.QuotesLoadedState? {
        return lastLoadedSwapStates[selectedProvider] as? SwapState.QuotesLoadedState
    }
}
