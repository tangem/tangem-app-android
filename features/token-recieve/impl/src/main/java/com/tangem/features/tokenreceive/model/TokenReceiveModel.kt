package com.tangem.features.tokenreceive.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.push
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.R
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.Asset
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.ens.EnsAddress
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.SaveViewedTokenReceiveWarningUseCase
import com.tangem.domain.tokens.model.analytics.TokenReceiveNewAnalyticsEvent
import com.tangem.domain.transaction.usecase.GetReverseResolvedEnsAddressUseCase
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.tokenreceive.component.TokenReceiveModelCallback
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.route.TokenReceiveRoutes
import com.tangem.features.tokenreceive.ui.state.TokenReceiveUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
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

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    val params = paramsContainer.require<TokenReceiveComponent.Params>()

    val stackNavigation = StackNavigation<TokenReceiveRoutes>()

    internal val state: StateFlow<TokenReceiveUM>
    field = MutableStateFlow<TokenReceiveUM>(getInitState())

    init {
        modelScope.launch {
            load()
        }
    }

    override fun onQrCodeClick(id: Int) {
        analyticsEventHandler.send(
            TokenReceiveNewAnalyticsEvent.QrScreenOpened(
                token = getTokenName(),
                blockchainName = params.config.cryptoCurrency.network.name,
            ),
        )
        stackNavigation.push(configuration = TokenReceiveRoutes.QrCode(addressId = id))
    }

    override fun onCopyClick(id: Int) {
        val addressToCopy = state.value.addresses[id] ?: return
        sendCopyActionAnalytic(addressToCopy)
        clipboardManager.setText(text = addressToCopy.value, isSensitive = true)
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

    private fun mapAddresses(addresses: List<ReceiveAddressModel>): ImmutableMap<Int, ReceiveAddress> {
        return buildMap {
            addresses.mapIndexed { index, model ->
                val type = when (model.nameService) {
                    ReceiveAddressModel.NameService.Default -> {
                        ReceiveAddress.Type.Default(
                            displayName = stringReference(model.displayName),
                        )
                    }
                    ReceiveAddressModel.NameService.Ens -> ReceiveAddress.Type.Ens
                }
                put(
                    key = index,
                    value = ReceiveAddress(
                        value = model.value,
                        type = type,
                    ),
                )
            }
        }.toPersistentMap()
    }

    private suspend fun load() = withContext(dispatchers.default) {
        state.value = state.value.copy(
            isEnsResultLoading = true,
        )

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

        val currentAddressValues = state.value.addresses.values.map { it.value }.toSet()

        val newEnsAddresses = reverseResolveResult
            .filterIsInstance<EnsAddress.Address>()
            .filterNot { it.name in currentAddressValues }
            .map { ensAddress -> ReceiveAddress(value = ensAddress.name, type = ReceiveAddress.Type.Ens) }

        val combinedAddresses = (state.value.addresses.values + newEnsAddresses)
            .sortedWith(compareByDescending { it.type is ReceiveAddress.Type.Ens })

        val updatedAddresses = combinedAddresses
            .mapIndexed { index, address -> index to address }
            .toMap()
            .toPersistentMap()

        state.value = state.value.copy(
            isEnsResultLoading = false,
            addresses = updatedAddresses,
        )
    }

    private fun getNotifications(): List<NotificationUM> {
        return buildList {
            add(
                NotificationUM.Info(
                    title = resourceReference(
                        R.string.receive_bottom_sheet_warning_title,
                        wrappedList(
                            getTokenName(),
                            params.config.cryptoCurrency.network.name,
                        ),
                    ),
                    subtitle = resourceReference(R.string.receive_bottom_sheet_warning_message_description),
                    iconTint = NotificationConfig.IconTint.Accent,
                ),
            )

            params.config.tokenReceiveNotification.map { notification ->
                add(
                    NotificationUM.Warning(
                        title = resourceReference(notification.title),
                        subtitle = resourceReference(notification.subtitle),
                    ),
                )
            }
        }
    }

    private fun getInitState(): TokenReceiveUM {
        return TokenReceiveUM(
            addresses = mapAddresses(params.config.receiveAddress),
            iconState = iconStateConverter.convert(params.config.cryptoCurrency),
            network = params.config.cryptoCurrency.network.name,
            isEnsResultLoading = false,
            notificationConfigs = getNotifications().toImmutableList(),
        )
    }

    private fun sendCopyActionAnalytic(receiveAddress: ReceiveAddress) {
        val event = when (receiveAddress.type) {
            is ReceiveAddress.Type.Default -> {
                TokenReceiveNewAnalyticsEvent.ButtonCopyAddress(
                    token = getTokenName(),
                    blockchainName = params.config.cryptoCurrency.network.name,
                )
            }
            ReceiveAddress.Type.Ens -> {
                TokenReceiveNewAnalyticsEvent.ButtonCopyEns(
                    token = getTokenName(),
                    blockchainName = params.config.cryptoCurrency.network.name,
                )
            }
        }
        analyticsEventHandler.send(event)
    }
}