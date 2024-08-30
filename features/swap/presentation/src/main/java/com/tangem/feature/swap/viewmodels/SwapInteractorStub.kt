package com.tangem.feature.swap.viewmodels

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.IncludeFeeInAmount
import com.tangem.feature.swap.domain.models.domain.PermissionOptions
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.*

internal class SwapInteractorStub(private val errorMessage: String) : SwapInteractor {
    override suspend fun getTokensDataState(currency: CryptoCurrency): TokensDataStateExpress = error(errorMessage)

    override suspend fun givePermissionToSwap(
        networkId: String,
        permissionOptions: PermissionOptions,
    ): SwapTransactionState = error(errorMessage)

    override suspend fun findBestQuote(
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        providers: List<SwapProvider>,
        amountToSwap: String,
        selectedFee: FeeType,
    ): Map<SwapProvider, SwapState> = error(errorMessage)

    override suspend fun onSwap(
        swapProvider: SwapProvider,
        swapData: SwapDataModel?,
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        amountToSwap: String,
        includeFeeInAmount: IncludeFeeInAmount,
        fee: TxFee,
    ): SwapTransactionState = error(errorMessage)

    override suspend fun updateQuotesStateWithSelectedFee(
        state: SwapState.QuotesLoadedState,
        selectedFee: FeeType,
        fromToken: CryptoCurrencyStatus,
        amountToSwap: String,
    ): SwapState.QuotesLoadedState = error(errorMessage)

    override fun getTokenBalance(token: CryptoCurrencyStatus): SwapAmount = error(errorMessage)

    override suspend fun selectInitialCurrencyToSwap(
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
    ): CryptoCurrencyStatus = error(errorMessage)

    override fun getNativeToken(networkId: String): CryptoCurrency = error(errorMessage)
}
