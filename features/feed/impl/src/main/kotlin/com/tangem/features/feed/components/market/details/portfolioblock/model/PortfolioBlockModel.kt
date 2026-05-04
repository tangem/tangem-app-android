package com.tangem.features.feed.components.market.details.portfolioblock.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.SpanStyle
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.common.getTotalFiatAmount
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.feed.components.market.details.portfolioblock.PortfolioBlockComponent
import com.tangem.features.feed.components.market.details.portfolioblock.ui.state.PortfolioBlockUM
import com.tangem.features.feed.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@Stable
@ModelScoped
internal class PortfolioBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val allAccountSupplier: MultiAccountStatusListSupplier,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : Model() {

    val state: StateFlow<PortfolioBlockUM>
        field = MutableStateFlow<PortfolioBlockUM>(PortfolioBlockUM.Loading)

    val bottomSheetNavigation: SlotNavigation<PortfolioBlockRoute> = SlotNavigation()

    val cryptoCurrencyIdState: StateFlow<CryptoCurrency.ID?>
        field = MutableStateFlow(null)

    private val params = paramsContainer.require<PortfolioBlockComponent.Params>()
    private val currencyRawId: CryptoCurrency.RawID = params.token.id

    private val tokenIcon: CurrencyIconState = CurrencyIconState.CoinIcon(
        url = params.token.imageUrl,
        fallbackResId = R.drawable.ic_custom_token_44,
        isGrayscale = false,
        shouldShowCustomBadge = false,
    )

    private val availableNetworks = MutableSharedFlow<List<TokenMarketInfo.Network>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        combineData()
            .onEach { state.value = it }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        availableNetworks.tryEmit(networks)
    }

    fun setNoNetworksAvailable() {
        availableNetworks.tryEmit(emptyList())
    }

    private fun combineData(): Flow<PortfolioBlockUM> {
        return availableNetworks.transformLatest { networks ->
            if (networks.isEmpty()) {
                emit(PortfolioBlockUM.Hidden)
            } else {
                emitAll(portfolioFlow().distinctUntilChanged())
            }
        }.distinctUntilChanged()
    }

    private fun portfolioFlow(): Flow<PortfolioBlockUM> {
        val portfolioDataFlow = allAccountSupplier().flatMapLatest { accountStatusLists ->
            val walletFlows = accountStatusLists.map { accountStatusList ->
                getUserWalletUseCase.invokeFlow(accountStatusList.userWalletId)
                    .mapNotNull { it.getOrNull() }
                    .map { wallet ->
                        WalletPortfolio(
                            userWallet = wallet,
                            currencies = accountStatusList.filterByRawId(),
                        )
                    }
            }
            if (walletFlows.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(walletFlows) { it.toList() }
            }
        }.distinctUntilChanged()

        val settingsFlow = combine(
            getSelectedAppCurrencyUseCase.invokeOrDefault(),
            getBalanceHidingSettingsUseCase.isBalanceHidden(),
        ) { appCurrency, isBalanceHidden ->
            SettingsBox(appCurrency, isBalanceHidden)
        }.distinctUntilChanged()

        return combine(portfolioDataFlow, settingsFlow) { portfolios, settings ->
            buildState(portfolios, settings)
        }.distinctUntilChanged()
    }

    private fun AccountStatusList.filterByRawId(): List<CryptoCurrencyStatus> {
        return accountStatuses.filterCryptoPortfolio().flatMap { accountStatus ->
            accountStatus.tokenList.flattenCurrencies().filter { status ->
                val statusRawId = status.currency.id.rawCurrencyId ?: return@filter false
                getTokenIdIfL2Network(statusRawId.value) == currencyRawId.value
            }
        }
    }

    private fun buildState(portfolios: List<WalletPortfolio>, settings: SettingsBox): PortfolioBlockUM {
        val allCurrencies = portfolios.flatMap { it.currencies }
        val hasMultiCurrencyWallet = portfolios.any { it.userWallet.isMultiCurrency }

        if (allCurrencies.isEmpty()) {
            return if (hasMultiCurrencyWallet) {
                PortfolioBlockUM.AddToken(
                    tokenIcon = tokenIcon,
                    onClick = { bottomSheetNavigation.activate(PortfolioBlockRoute) },
                )
            } else {
                PortfolioBlockUM.Hidden
            }
        }

        cryptoCurrencyIdState.update {
            it ?: allCurrencies.first().currency.id
        }

        val totalFiat = allCurrencies.mapNotNull { it.getTotalFiatAmount() }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        val formattedBalance = totalFiat.formatStyled {
            fiat(
                fiatCurrencyCode = settings.appCurrency.code,
                fiatCurrencySymbol = settings.appCurrency.symbol,
                spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
            )
        }

        return PortfolioBlockUM.Content(
            totalBalance = formattedBalance,
            tokensInPortfolioCount = allCurrencies.size,
            tokenIcon = tokenIcon,
            tokenName = allCurrencies.first().currency.name,
            tokenSymbol = allCurrencies.first().currency.symbol,
            isBalanceHidden = settings.isBalanceHidden,
            onClick = { bottomSheetNavigation.activate(PortfolioBlockRoute) },
        )
    }
}

private data class WalletPortfolio(
    val userWallet: UserWallet,
    val currencies: List<CryptoCurrencyStatus>,
)

private data class SettingsBox(
    val appCurrency: AppCurrency,
    val isBalanceHidden: Boolean,
)