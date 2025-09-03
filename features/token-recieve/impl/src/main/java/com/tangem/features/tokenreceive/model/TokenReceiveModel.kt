package com.tangem.features.tokenreceive.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.push
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.models.Asset
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.SaveViewedTokenReceiveWarningUseCase
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.domain.tokens.model.analytics.TokenReceiveNewAnalyticsEvent
import com.tangem.domain.transaction.usecase.GetReverseResolvedEnsAddressUseCase
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.tokenreceive.component.TokenReceiveModelCallback
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.entity.ReceiveAddress.Type.Ens
import com.tangem.features.tokenreceive.entity.ReceiveAddress.Type.Primary
import com.tangem.features.tokenreceive.entity.TokenReceiveStateFactory
import com.tangem.features.tokenreceive.route.TokenReceiveRoutes
import com.tangem.features.tokenreceive.ui.state.TokenReceiveUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class TokenReceiveModel @Inject constructor(
    private val clipboardManager: ClipboardManager,
    private val shareManager: ShareManager,
    private val getReverseResolvedEnsAddressUseCase: GetReverseResolvedEnsAddressUseCase,
    private val saveViewedTokenReceiveWarningUseCase: SaveViewedTokenReceiveWarningUseCase,
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model(), TokenReceiveModelCallback {

    private val tokenReceiveStateFactory: TokenReceiveStateFactory by lazy {
        TokenReceiveStateFactory(
            cryptoCurrency = params.config.cryptoCurrency,
            addresses = params.config.receiveAddress,
            tokenReceiveNotification = params.config.tokenReceiveNotification,
            currentStateProvider = Provider { state.value },
        )
    }

    val params = paramsContainer.require<TokenReceiveComponent.Params>()

    val stackNavigation = StackNavigation<TokenReceiveRoutes>()

    internal val state: StateFlow<TokenReceiveUM>
    field = MutableStateFlow<TokenReceiveUM>(tokenReceiveStateFactory.getInitialState(getTokenName()))

    init {
        modelScope.launch {
            load()
        }
    }

    override fun onQrCodeClick(address: String) {
        analyticsEventHandler.send(
            TokenReceiveNewAnalyticsEvent.QrScreenOpened(
                token = getTokenName(),
                blockchainName = params.config.cryptoCurrency.network.name,
            ),
        )
        stackNavigation.push(configuration = TokenReceiveRoutes.QrCode(address = address))
    }

    override fun onCopyClick(address: ReceiveAddress, source: TokenReceiveCopyActionSource) {
        sendCopyActionAnalytic(address, source)
        clipboardManager.setText(text = address.value, isSensitive = true)
    }

    override fun onShareClick(address: String) {
        shareManager.shareText(text = address)
    }

    override fun onWarningAcknowledged() {
        modelScope.launch {
            saveViewedTokenReceiveWarningUseCase.invoke(
                when (val asset = params.config.asset) {
                    Asset.Currency -> params.config.cryptoCurrency.name
                    Asset.NFT -> asset.name
                },
            )
            stackNavigation.push(configuration = TokenReceiveRoutes.ReceiveAssets)
        }
    }

    internal fun getTokenName(): String {
        return when (val asset = params.config.asset) {
            Asset.Currency -> params.config.cryptoCurrency.symbol
            Asset.NFT -> asset.name
        }
    }

    private suspend fun load() = withContext(dispatchers.default) {
        state.value = tokenReceiveStateFactory.getLoadingState()
        val reverseResolveResult =
            if (params.config.cryptoCurrency.network.nameResolvingType == Network.NameResolvingType.ENS) {
                getReverseResolvedEnsAddressUseCase(
                    userWalletId = params.config.userWalletId,
                    network = params.config.cryptoCurrency.network,
                    addresses = params.config.receiveAddress.map { it.value },
                )
            } else {
                emptyList()
            }
        state.value = tokenReceiveStateFactory.getLoadedState(reverseResolveResult)
    }

    private fun sendCopyActionAnalytic(receiveAddress: ReceiveAddress, source: TokenReceiveCopyActionSource) {
        val event = when (receiveAddress.type) {
            is Primary -> {
                TokenReceiveNewAnalyticsEvent.ButtonCopyAddress(
                    token = getTokenName(),
                    blockchainName = params.config.cryptoCurrency.network.name,
                    tokenReceiveSource = source,
                )
            }
            Ens -> {
                TokenReceiveNewAnalyticsEvent.ButtonCopyEns(
                    token = getTokenName(),
                    blockchainName = params.config.cryptoCurrency.network.name,
                )
            }
        }
        analyticsEventHandler.send(event)
    }
}