package com.tangem.features.commonfeatures.api.addtoportfolio

import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager.AnalyticsParams
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

interface AddToPortfolioManager : AddToPortfolioManagerInternal {

    val onDismiss: Channel<Unit>
    val onSuccessAdded: Channel<Result>
    val onAddedTokenClick: Channel<Result>

    val state: StateFlow<State>

    /**
     * default is [LaunchMode.DirectAdd]
     */
    fun updateLaunchMode(launchMode: LaunchMode)

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>)
    fun setTokenParams(token: RawMarketToken)
    fun setTokenParams(token: TokenMarketParams) = setTokenParams(
        RawMarketToken(
            id = token.id,
            name = token.name,
            symbol = token.symbol,
        ),
    )

    sealed interface State {
        data object Loading : State
        data class Ready(val availableToAddData: AvailableToAddData) : State {
            val isAvailableToAdd: Boolean get() = availableToAddData.isAvailableToAdd
            val isSinglePortfolio: Boolean get() = availableToAddData.isSinglePortfolio
        }
    }

    @Serializable
    data class AnalyticsParams(
        val source: String?,
        val category: String = CategoryDefault,
    ) {
        companion object {
            const val CategoryDefault = "Markets / Chart"
            const val CategoryEarn = "Earn"
        }
    }

    interface Factory {
        fun create(scope: CoroutineScope, settings: Settings, analyticsParams: AnalyticsParams): AddToPortfolioManager
    }

    sealed interface LaunchMode {
        data object DirectAdd : LaunchMode
        data object Preselected : LaunchMode
        data object ViaUserPortfolio : LaunchMode
    }

    /**
     * Immutable settings
     */
    data class Settings(
        val shouldSkipTokenActionsScreen: Boolean = false,
    ) {
        companion object {
            val DefaultMarket = Settings(shouldSkipTokenActionsScreen = false)
            val ChooseToken = Settings(shouldSkipTokenActionsScreen = true)
            val Earn = Settings(shouldSkipTokenActionsScreen = true)
        }
    }

    /**
     * Mutable parameters
     * Updates may trigger reload [State]
     */
    data class Params(
        val networks: List<TokenMarketInfo.Network>,
        val token: RawMarketToken,
        val launchMode: LaunchMode,
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
    val portfolioFetcher: PortfolioFetcher

    suspend fun params(): AddToPortfolioManager.Params = paramsFlow.first()
    suspend fun token(): RawMarketToken = params().token

    fun onDismiss()
    fun onSuccessAdded(result: AddToPortfolioManager.Result)
    fun onAddedTokenClick(result: AddToPortfolioManager.Result)
}