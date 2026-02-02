package com.tangem.features.nft.receive.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.ui.account.toUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.FilterNFTAvailableNetworksUseCase
import com.tangem.domain.nft.GetNFTCurrencyUseCase
import com.tangem.domain.nft.GetNFTNetworkStatusUseCase
import com.tangem.domain.nft.GetNFTNetworksUseCase
import com.tangem.domain.nft.analytics.NFTAnalyticsEvent
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.nft.impl.R
import com.tangem.features.nft.receive.NFTReceiveComponent
import com.tangem.features.nft.receive.entity.NFTReceiveUM
import com.tangem.features.nft.receive.entity.transformer.ToggleSearchBarTransformer
import com.tangem.features.nft.receive.entity.transformer.UpdateDataStateTransformer
import com.tangem.features.nft.receive.entity.transformer.UpdateSearchQueryTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class NFTReceiveModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val searchManager: InputManager,
    private val getNFTNetworksUseCase: GetNFTNetworksUseCase,
    private val filterNFTAvailableNetworksUseCase: FilterNFTAvailableNetworksUseCase,
    private val getNFTNetworkStatusUseCase: GetNFTNetworkStatusUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val messageSender: UiMessageSender,
    private val getNFTCurrencyUseCase: GetNFTCurrencyUseCase,
    private val receiveAddressesFactory: ReceiveAddressesFactory,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val singleAccountSupplier: SingleAccountSupplier,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: NFTReceiveComponent.Params = paramsContainer.require()

    val bottomSheetNavigation: SlotNavigation<TokenReceiveConfig> = SlotNavigation()

    private val _state = MutableStateFlow(
        value = NFTReceiveUM(
            onBackClick = params.onBackClick,
            appBarSubtitle = TextReference.EMPTY,
            search = getInitialSearchBar(),
            networks = NFTReceiveUM.Networks.Content(
                availableItems = persistentListOf(),
                unavailableItems = persistentListOf(),
            ),
            bottomSheetConfig = null,
        ),
    )

    val state: StateFlow<NFTReceiveUM> get() = _state

    init {
        analyticsEventHandler.send(NFTAnalyticsEvent.Receive.ScreenOpened())
        subscribeToNFTAvailableNetworks()
        loadPortfolioName()
    }

    private fun loadPortfolioName() = modelScope.launch(dispatchers.default) {
        val appBarSubtitle = when (val portfolioId = params.portfolioId) {
            is PortfolioId.Wallet -> loadWalletName(portfolioId.userWalletId)
            is PortfolioId.Account -> if (isAccountsModeEnabledUseCase.invokeSync()) {
                loadAccountName(portfolioId.accountId)
            } else {
                loadWalletName(portfolioId.userWalletId)
            }
        }
        _state.update { it.copy(appBarSubtitle = appBarSubtitle) }
    }

    private fun loadWalletName(userWalletId: UserWalletId): TextReference {
        return getUserWalletUseCase(userWalletId)
            .map { it.name }
            .getOrNull()
            ?.let { createAppBarSubtitle(stringReference(it)) }
            ?: TextReference.EMPTY
    }

    private suspend fun loadAccountName(accountId: AccountId): TextReference {
        return singleAccountSupplier
            .getSyncOrNull(SingleAccountProducer.Params(accountId))
            ?.let { createAppBarSubtitle(it.accountName.toUM().value) }
            ?: TextReference.EMPTY
    }

    private fun createAppBarSubtitle(text: TextReference) = resourceReference(
        R.string.hot_crypto_add_token_subtitle,
        formatArgs = wrappedList(text),
    )

    private fun subscribeToNFTAvailableNetworks() {
        combine(
            flow = getNFTNetworksUseCase(params.portfolioId),
            flow2 = searchManager.query.distinctUntilChanged(),
        ) { networks, query ->
            filterNFTAvailableNetworksUseCase(networks, query)
        }
            .onEach { filteredNetworks ->
                _state.update {
                    UpdateDataStateTransformer(
                        networks = filteredNetworks,
                        onNetworkClick = ::onNetworkClick,
                    ).transform(it)
                }
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun onSearchQueryChange(newQuery: String) {
        modelScope.launch {
            _state.update { UpdateSearchQueryTransformer(newQuery).transform(it) }

            searchManager.update(newQuery)
        }
    }

    private fun getInitialSearchBar(): SearchBarUM = SearchBarUM(
        placeholderText = resourceReference(R.string.common_search),
        query = "",
        isActive = false,
        onQueryChange = ::onSearchQueryChange,
        onActiveChange = ::toggleSearchBar,
    )

    private fun toggleSearchBar(isActive: Boolean) {
        _state.update {
            ToggleSearchBarTransformer(isActive).transform(it)
        }
    }

    private fun onNetworkClick(network: Network, enabled: Boolean) {
        if (!enabled) {
            val message = DialogMessage(
                title = resourceReference(R.string.nft_receive_unavailable_asset_warning_title),
                message = resourceReference(R.string.nft_receive_unavailable_asset_warning_message),
            )

            messageSender.send(message)
        } else {
            modelScope.launch {
                analyticsEventHandler.send(NFTAnalyticsEvent.Receive.BlockchainChosen(network.name))

                val networkStatus = getNFTNetworkStatusUseCase.invoke(
                    userWalletId = params.portfolioId.userWalletId,
                    network = network,
                ) ?: return@launch

                when (val value = networkStatus.value) {
                    is NetworkStatus.Verified -> {
                        bottomSheetNavigation.activate(
                            configuration = configureReceiveAddresses(
                                addresses = value.address,
                                network = network,
                            ),
                        )
                    }
                    is NetworkStatus.MissedDerivation,
                    is NetworkStatus.NoAccount,
                    is NetworkStatus.Unreachable,
                    -> Unit
                }
            }
        }
    }

    private suspend fun configureReceiveAddresses(addresses: NetworkAddress, network: Network): TokenReceiveConfig {
        val cryptoCurrency = getNFTCurrencyUseCase.invoke(network)
        return receiveAddressesFactory.createForNft(
            userWalletId = params.portfolioId.userWalletId,
            addresses = addresses,
            network = network,
            nft = cryptoCurrency,
        )
    }
}