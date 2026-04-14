package com.tangem.features.commonfeatures.api.addtoportfolio

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager.AnalyticsParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

interface AddToPortfolioManager : AddToPortfolioManagerInternal {

    val onDismiss: Channel<Unit>
    val onSuccessAdded: Channel<Result>

    val portfolioFetcher: PortfolioFetcher
    val state: StateFlow<State>

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>)
    fun setTokenParams(token: TokenMarketParams)

    sealed interface State {
        data object Loading : State
        data class Ready(
            val availableToAddData: AvailableToAddData,
        ) : State {
            val isAvailableToAdd: Boolean get() = availableToAddData.isAvailableToAdd
            val isSinglePortfolio: Boolean get() = availableToAddData.isSinglePortfolio
        }
    }

    @Serializable
    data class AnalyticsParams(
        val source: String?,
    )

    interface Factory {
        fun create(scope: CoroutineScope, settings: Settings, analyticsParams: AnalyticsParams): AddToPortfolioManager
    }

    /**
     * Immutable settings
     */
    data class Settings(
        val shouldSkipTokenActionsScreen: Boolean = false,
    ) {
        companion object {
            val DefaultMarket = Settings(
                shouldSkipTokenActionsScreen = false,
            )
            val ChooseToken = Settings(
                shouldSkipTokenActionsScreen = true,
            )
        }
    }

    /**
     * Mutable parameters
     * Updates may trigger reload [State]
     */
    data class Params(
        val networks: List<TokenMarketInfo.Network>,
        val token: TokenMarketParams,
    )

    data class Result(
        val wallet: UserWallet,
        val account: AccountStatus.CryptoPortfolio,
        val addedCurrency: CryptoCurrencyStatus,
    )
}

/**
 * primary for internal impl usage, but you can also use it externally
 */
interface AddToPortfolioManagerInternal {
    val paramsFlow: SharedFlow<AddToPortfolioManager.Params>
    val settings: AddToPortfolioManager.Settings
    val analyticsParams: AnalyticsParams

    suspend fun token(): TokenMarketParams = paramsFlow.first().token

    fun onDismiss()
    fun onSuccessAdded(result: AddToPortfolioManager.Result)
}