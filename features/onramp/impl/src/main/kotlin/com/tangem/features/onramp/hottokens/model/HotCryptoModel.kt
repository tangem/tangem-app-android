package com.tangem.features.onramp.hottokens.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.popToFirst
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.onramp.GetHotCryptoUseCase
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.features.onramp.hottokens.HotCryptoComponent
import com.tangem.features.onramp.hottokens.converter.HotTokenItemStateConverter
import com.tangem.features.onramp.hottokens.entity.HotCryptoUM
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddTokenComponent
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddTokenComponent.AddHotCryptoData
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddToPortfolioBSConfig
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddTokenRoute
import com.tangem.features.onramp.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Hot crypto model
 *
 * @param paramsContainer                       params container
 * @param getHotCryptoUseCase                   use case for getting hot crypto
 * @param getSelectedAppCurrencyUseCase         use case for getting selected app currency
 * @property getCryptoCurrencyStatusSyncUseCase use case for getting crypto currency status by id
 * @property dispatchers                        dispatchers
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
@ModelScoped
internal class HotCryptoModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val getHotCryptoUseCase: GetHotCryptoUseCase,
    private val callbackDelegate: HotCryptoModelCallbackDelegate,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val hotCryptoPortfolioDataLoader: HotCryptoPortfolioDataLoader,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    val portfolioSelectorController: PortfolioSelectorController,
    private val portfolioFetcherFactory: PortfolioFetcher.Factory,
) : Model(), OnrampAddTokenComponent.Callbacks by callbackDelegate {

    val bottomSheetNavigation: SlotNavigation<OnrampAddToPortfolioBSConfig> = SlotNavigation()

    val portfolioFetcher: PortfolioFetcher?
    val bottomSheetNavigationV2 = StackNavigation<OnrampAddTokenRoute>()
    private val addHotCryptoJob = JobHolder()
    val hotCryptoToAddDataFlow: MutableSharedFlow<AddHotCryptoData> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val state: StateFlow<HotCryptoUM>
        field = MutableStateFlow(value = HotCryptoUM(items = persistentListOf()))

    private val params: HotCryptoComponent.Params = paramsContainer.require()

    init {
        if (accountsFeatureToggles.isFeatureEnabled) {
            portfolioFetcher = portfolioFetcherFactory.create(
                mode = PortfolioFetcher.Mode.Wallet(params.userWalletId),
                scope = modelScope,
            )
            combineData()
        } else {
            portfolioFetcher = null
            combineDataOld()
        }
    }

    private fun combineData() {
        combine(
            flow = hotCryptoPortfolioDataLoader.loadPortfolioData(params.userWalletId),
            flow2 = isAccountsModeEnabledUseCase.invoke(),
            transform = { data, isAccountMode ->
                HotTokenItemStateConverter(
                    appCurrency = data.appCurrency,
                    onItemClick = { tokenItemState, hotCryptoCurrency ->
                        startAddTokenFlow(currency = hotCryptoCurrency, hotCryptoPortfolioData = data)
                    },
                )
                    .convertList(data.allHotCrypto)
                    .map(TokensListItemUM::Token)
            },
        )
            .onEach { items -> state.update { HotCryptoUM(items = it.buildItems(items)) } }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun combineDataOld() {
        combine(
            flow = getSelectedAppCurrencyUseCase().map { it.getOrElse { AppCurrency.Default } },
            flow2 = getHotCryptoUseCase(params.userWalletId),
        ) { appCurrency, currencies ->
            HotTokenItemStateConverter(appCurrency = appCurrency, onItemClick = ::onTokenClick)
                .convertList(currencies)
                .map(TokensListItemUM::Token)
        }
            .onEach { items ->
                state.update { HotCryptoUM(items = it.buildItems(items)) }
            }
            .launchIn(modelScope)
    }

    private fun HotCryptoUM.buildItems(items: List<TokensListItemUM.Token>): ImmutableList<TokensListItemUM> =
        buildList {
            if (items.isNotEmpty()) {
                add(getOrCreateGroupTitle())
            }
            addAll(items)
        }.toImmutableList()

    private fun HotCryptoUM.getOrCreateGroupTitle(): TokensListItemUM {
        return items.firstOrNull()
            .takeIf { it is TokensListItemUM.GroupTitle }
            ?: TokensListItemUM.GroupTitle(
                id = R.string.tokens_list_hot_crypto_header,
                text = resourceReference(id = R.string.tokens_list_hot_crypto_header),
            )
    }

    private fun onTokenClick(tokenItemState: TokenItemState, currency: HotCryptoCurrency) {
        bottomSheetNavigation.activate(
            configuration = OnrampAddToPortfolioBSConfig.AddToPortfolio(
                cryptoCurrency = currency.cryptoCurrency,
                currencyIconState = tokenItemState.iconState,
                onSuccessAdding = ::onSuccessAdding,
            ),
        )
    }

    private fun startAddTokenFlow(currency: HotCryptoCurrency, hotCryptoPortfolioData: HotCryptoPortfolioData) {
        hotCryptoToAddDataFlow.resetReplayCache()
        fun closeNavigationFlow() = bottomSheetNavigationV2.replaceAll(OnrampAddTokenRoute.Empty)
        channelFlow<Unit> {
            val userWallet = hotCryptoPortfolioData.wallet.userWallet
            val isSingleAccount = hotCryptoPortfolioData.wallet.accounts.size == 1

            if (isSingleAccount) {
                val account = hotCryptoPortfolioData.wallet.accounts.first().account
                val tokenToAdd = AddHotCryptoData(
                    cryptoCurrency = currency.cryptoCurrency,
                    userWallet = userWallet,
                    account = account,
                    isMorePortfolioAvailable = false,
                )
                hotCryptoToAddDataFlow.emit(tokenToAdd)
                bottomSheetNavigationV2.replaceAll(OnrampAddTokenRoute.AddToken)
            } else {
                setupPortfolioSelector(currency, hotCryptoPortfolioData)
                bottomSheetNavigationV2.replaceAll(OnrampAddTokenRoute.PortfolioSelector)

                val tokenToAddStateFlow = portfolioSelectorController
                    .selectedAccountWithData(requireNotNull(portfolioFetcher))
                    .filterNotNull()
                    .map { (_, selectedAccount) ->
                        AddHotCryptoData(
                            cryptoCurrency = currency.cryptoCurrency,
                            userWallet = userWallet,
                            account = selectedAccount,
                            isMorePortfolioAvailable = true,
                        )
                    }
                    .shareIn(this, started = SharingStarted.Eagerly)

                val firstTokenToAdd = tokenToAddStateFlow.first()
                hotCryptoToAddDataFlow.emit(firstTokenToAdd)
                bottomSheetNavigationV2.replaceAll(OnrampAddTokenRoute.AddToken)

                callbackDelegate.onChangePortfolioClick.receiveAsFlow()
                    .onEach { bottomSheetNavigationV2.pushNew(OnrampAddTokenRoute.PortfolioSelector) }
                    .launchIn(this)

                tokenToAddStateFlow
                    .onEach { newTokenToAdd -> hotCryptoToAddDataFlow.emit(newTokenToAdd) }
                    .onEach { bottomSheetNavigationV2.popToFirst() }
                    .launchIn(this)
            }

            val addedToken = callbackDelegate.onTokenAdded
                .receiveAsFlow()
                .first()
            params.onTokenClick(addedToken)
            closeNavigationFlow()
            channel.close()
        }
            .catch { throwable ->
                Timber.e(throwable)
                closeNavigationFlow()
            }
            .launchIn(modelScope)
            .saveIn(addHotCryptoJob)
    }

    private fun onSuccessAdding(id: CryptoCurrency.ID) {
        modelScope.launch {
            getSingleCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
                userWalletId = params.userWalletId,
                cryptoCurrencyId = id,
            )
                .onRight { status ->
                    bottomSheetNavigation.dismiss()
                    params.onTokenClick(status)
                }
                .onLeft { Timber.d("Unable to get CryptoCurrencyStatus[$id]: $it") }
        }
    }

    private fun setupPortfolioSelector(hotCrypto: HotCryptoCurrency, hotCryptoPortfolioData: HotCryptoPortfolioData) {
        portfolioSelectorController.selectAccount(null)
        portfolioSelectorController.isEnabled.value = isEnabled@{ _, accountStatus ->
            val isNotAddedHotCrypto = hotCryptoPortfolioData.wallet.accounts
                .find { it.account.accountId == accountStatus.accountId }
                ?.addedHotCrypto
                ?.none { it.currency.id.rawCurrencyId == hotCrypto.cryptoCurrency.id.rawCurrencyId } == true

            return@isEnabled isNotAddedHotCrypto
        }
    }
}

@ModelScoped
internal class HotCryptoModelCallbackDelegate @Inject constructor() : OnrampAddTokenComponent.Callbacks {

    val onChangePortfolioClick = Channel<Unit>()
    val onTokenAdded = Channel<CryptoCurrencyStatus>()

    override fun onChangePortfolioClick() {
        onChangePortfolioClick.trySend(Unit)
    }

    override fun onTokenAdded(status: CryptoCurrencyStatus) {
        onTokenAdded.trySend(status)
    }
}