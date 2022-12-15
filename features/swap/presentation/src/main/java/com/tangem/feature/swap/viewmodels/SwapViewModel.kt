package com.tangem.feature.swap.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SwapViewModel @Inject constructor(
    private val referralInteractor: SwapInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel() {

    // region temp for test
    init {
        viewModelScope.launch {
            runCatching {
                val tokens = referralInteractor.getTokensToSwap("binance-smart-chain")
                val token = tokens[1]
                val token2 = tokens[2]
                if (token is Currency.NonNativeToken && token2 is Currency.NonNativeToken) {
                    findBestQuotes(token.contractAddress, token2.contractAddress)
                }
            }.onFailure {
                Log.e("SwapViewModel", it.message ?: it.cause.toString())
            }
        }
    }

    private suspend fun findBestQuotes(fromTokenAddress: String, toTokenAddress: String) {
        referralInteractor.findBestQuote(
            fromTokenAddress,
            toTokenAddress,
            "1",
        )
    }
    // endregion temp for test
}
