package com.tangem.features.managetokens.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.managetokens.GetSupportedNetworksUseCase
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent.Params.DerivationPathSelector
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent.Params.NetworkSelector
import com.tangem.features.managetokens.entity.customtoken.CustomTokenSelectorDialogConfig
import com.tangem.features.managetokens.entity.customtoken.CustomTokenSelectorUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenSelectorUM.HeaderUM
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath
import com.tangem.features.managetokens.entity.customtoken.SelectedNetwork
import com.tangem.features.managetokens.entity.item.DerivationPathUM
import com.tangem.features.managetokens.entity.item.SelectableItemUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.utils.mapper.toCurrencyNetworkModel
import com.tangem.features.managetokens.utils.mapper.toDerivationPathModel
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class CustomTokenSelectorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSupportedNetworksUseCase: GetSupportedNetworksUseCase,
    private val messageSender: UiMessageSender,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: CustomTokenSelectorComponent.Params = paramsContainer.require()

    val dialogNavigation: SlotNavigation<CustomTokenSelectorDialogConfig> = SlotNavigation()

    val state: MutableStateFlow<CustomTokenSelectorUM> = MutableStateFlow(
        value = getInitialState(),
    )

    init {
        loadItems()
    }

    private fun getInitialState(): CustomTokenSelectorUM = when (params) {
        is NetworkSelector -> CustomTokenSelectorUM(
            header = if (params.selectedNetwork == null) {
                HeaderUM.Description
            } else {
                HeaderUM.None
            },
            items = persistentListOf(),
        )
        is DerivationPathSelector -> CustomTokenSelectorUM(
            header = HeaderUM.CustomDerivationButton(
                value = (params.selectedDerivationPath?.value as? Network.DerivationPath.Custom)?.value,
                onClick = ::showCustomDerivationInput,
            ),
            items = persistentListOf(),
        )
    }

    private fun loadItems() = modelScope.launch {
        val items = when (params) {
            is NetworkSelector -> loadNetworks(params).toImmutableList()
            is DerivationPathSelector -> loadDerivationPaths(params).toImmutableList()
        }

        state.update { state ->
            state.copy(items = items)
        }
    }

    private suspend fun loadNetworks(selector: NetworkSelector): List<SelectableItemUM> {
        return getSupportedNetworks(selector.userWalletId).map { network ->
            network.toCurrencyNetworkModel(
                isSelected = network.id == selector.selectedNetwork?.id,
                onSelectedStateChange = {
                    val model = SelectedNetwork(
                        id = network.id,
                        name = network.name,
                        derivationPath = network.derivationPath,
                        canHandleTokens = network.canHandleTokens,
                    )

                    selector.onNetworkSelected(model)
                },
            )
        }
    }

    private suspend fun loadDerivationPaths(selector: DerivationPathSelector): List<SelectableItemUM> {
        val derivationPaths = mutableListOf<DerivationPathUM>()
        val defaultPath = selector.selectedNetwork.let { network ->
            network.toDerivationPathModel(
                isSelected = network.id == selector.selectedDerivationPath?.id,
                onSelectedStateChange = {
                    val model = SelectedDerivationPath(
                        id = network.id,
                        name = network.name,
                        value = network.derivationPath,
                        isDefault = true,
                    )

                    selector.onDerivationPathSelected(model)
                },
            )
        }

        if (defaultPath != null) {
            derivationPaths.add(defaultPath)
        }

        getSupportedNetworks(selector.userWalletId)
            .mapNotNullTo(derivationPaths) { network ->
                if (network.id == selector.selectedNetwork.id) {
                    return@mapNotNullTo null // Skip default path
                }

                network.toDerivationPathModel(
                    isSelected = network.id == selector.selectedDerivationPath?.id,
                    onSelectedStateChange = {
                        val model = SelectedDerivationPath(
                            id = network.id,
                            name = network.name,
                            value = network.derivationPath,
                            isDefault = false,
                        )

                        selector.onDerivationPathSelected(model)
                    },
                )
            }

        return derivationPaths
    }

    private suspend fun getSupportedNetworks(userWalletId: UserWalletId): List<Network> {
        return getSupportedNetworksUseCase(userWalletId).getOrElse { e ->
            val message = SnackbarMessage(message = resourceReference(R.string.common_unknown_error))
            messageSender.send(message)

            emptyList()
        }
    }

    private fun showCustomDerivationInput() {
        val config = when (params) {
            is NetworkSelector -> return
            is DerivationPathSelector -> CustomTokenSelectorDialogConfig.CustomDerivationInput(params.userWalletId)
        }

        dialogNavigation.activate(config)
    }

    fun selectCustomDerivationPath(value: SelectedDerivationPath) {
        when (params) {
            is NetworkSelector -> return
            is DerivationPathSelector -> params.onDerivationPathSelected(value)
        }
    }
}