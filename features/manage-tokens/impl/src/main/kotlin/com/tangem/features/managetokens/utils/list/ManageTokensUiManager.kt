package com.tangem.features.managetokens.utils.list

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.utils.mapper.toUiModel
import com.tangem.features.managetokens.utils.ui.toggleExpanded
import com.tangem.features.managetokens.utils.ui.update
import com.tangem.pagination.Batch
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class ManageTokensUiManager(
    private val state: MutableStateFlow<ManageTokensListState>,
    private val messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
    private val scopeProvider: Provider<CoroutineScope>,
    private val sourceProvider: Provider<ManageTokensSource>,
    private val actions: ManageTokensUiActions,
    private val clipboardManager: ClipboardManager,
) {

    private val scope: CoroutineScope
        get() = scopeProvider()

    private val source: ManageTokensSource
        get() = sourceProvider()

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: Flow<ImmutableList<CurrencyItemUM>> = state
        .mapLatest { state ->
            state.uiBatches.asSequence()
                .flatMap { it.data }
                .toImmutableList()
        }
        .distinctUntilChanged()

    fun createOrUpdateUiBatches(
        newCurrencyBatches: List<Batch<Int, List<ManagedCryptoCurrency>>>,
        canEditItems: Boolean,
    ): List<Batch<Int, List<CurrencyItemUM>>> {
        val currentUiBatches = state.value.uiBatches
        val batches = currentUiBatches.toMutableList()

        newCurrencyBatches.forEach { (key, data) ->
            val indexToUpdate = currentUiBatches.indexOfFirst { it.key == key }
            val currencyBatch = state.value.currencyBatches.getOrNull(indexToUpdate)

            if (indexToUpdate == -1 || currencyBatch == null) {
                val newBatch = Batch(
                    key = key,
                    data = data.map { item ->
                        item.toUiModel(
                            isEditable = canEditItems,
                            onRemoveCustomCurrencyClick = ::removeCustomCurrency,
                            onExpandNetworksClick = ::toggleCurrencyNetworksVisibility,
                        )
                    },
                )

                batches.addOrReplace(newBatch) { it.key == key }
            } else {
                val uiBatchToUpdate = currentUiBatches[indexToUpdate]

                if (uiBatchToUpdate.data == data) {
                    return@forEach
                }

                val updatedBatch = uiBatchToUpdate.copy(
                    data = data.mapIndexed { index, item ->
                        if (item == currencyBatch.data[index]) {
                            return@mapIndexed uiBatchToUpdate.data[index]
                        }

                        val previousUiItem = uiBatchToUpdate.data.getOrNull(index)
                        if (previousUiItem == null || previousUiItem.id != item.id) {
                            item.toUiModel(
                                isEditable = canEditItems,
                                onRemoveCustomCurrencyClick = ::removeCustomCurrency,
                                onExpandNetworksClick = ::toggleCurrencyNetworksVisibility,
                            )
                        } else {
                            previousUiItem.update(item)
                        }
                    },
                )

                batches[indexToUpdate] = updatedBatch
            }
        }

        return batches
    }

    private fun removeCustomCurrency(currency: ManagedCryptoCurrency.Custom) = scope.launch(dispatchers.default) {
        showRemoveNetworkWarning(
            currency = currency,
            network = currency.network,
            isCoin = currency is ManagedCryptoCurrency.Custom.Coin,
            onConfirm = {
                val userWalletId = requireNotNull(state.value.userWalletId) { "UserWalletId is null. Can not remove" }
                actions.removeCustomCurrency(userWalletId = userWalletId, currency = currency)
            },
        )
    }

    private fun toggleCurrencyNetworksVisibility(currency: ManagedCryptoCurrency.Token) = scope.launch(
        dispatchers.default,
    ) {
        state.update { batches ->
            val batchIndex = batches.batchIndexByCurrencyId(currency.id)
            val currencyBatch = batches.currencyBatches[batchIndex]
            val currencyIndex = currencyBatch.currencyIndexById(currency.id)

            val uiBatch = batches.uiBatches[batchIndex]
            val updatedUiItem = uiBatch.data[currencyIndex].toggleExpanded(
                currency = currencyBatch.data[currencyIndex],
                isEditable = batches.canEditItems,
                onSelectCurrencyNetwork = { networkId, isSelected ->
                    selectNetwork(currencyBatch.key, currency, networkId, isSelected)
                },
                onLongTap = ::copyContractAddress,
            )

            batches.updateUiBatchesItem(
                indexToBatch = batchIndex to uiBatch,
                indexToItem = currencyIndex to updatedUiItem,
            )
        }
    }

    private fun copyContractAddress(source: ManagedCryptoCurrency.SourceNetwork) {
        if (source is ManagedCryptoCurrency.SourceNetwork.Default) {
            clipboardManager.setText(text = source.contractAddress, isSensitive = false)
            showSnackbarMessage(resourceReference(R.string.contract_address_copied_message))
        }
    }

    private fun showSnackbarMessage(messageText: TextReference) {
        val message = SnackbarMessage(message = messageText)
        messageSender.send(message)
    }

    private fun selectNetwork(
        batchKey: Int,
        currency: ManagedCryptoCurrency,
        source: ManagedCryptoCurrency.SourceNetwork,
        isSelected: Boolean,
    ) = scope.launch(dispatchers.default) {
        if (currency !is ManagedCryptoCurrency.Token) return@launch

        if (isSelected) {
            val userWalletId = state.value.userWalletId
            val unsupportedState = userWalletId?.let { actions.checkCurrencyUnsupportedState(it, source) }
            if (unsupportedState != null) {
                showUnsupportedWarning(unsupportedState)
            } else {
                actions.addCurrency(batchKey, currency, source.network)
            }
        } else {
            if (actions.checkNeedToShowRemoveNetworkWarning(currency, source.network)) {
                showRemoveNetworkWarning(
                    currency = currency,
                    network = source.network,
                    isCoin = source is ManagedCryptoCurrency.SourceNetwork.Main,
                    onConfirm = {
                        actions.removeCurrency(batchKey, currency, source.network)
                    },
                )
            } else {
                actions.removeCurrency(batchKey, currency, source.network)
            }
        }
    }

    private fun showUnsupportedWarning(unsupportedState: CurrencyUnsupportedState) {
        val message = DialogMessage(
            title = resourceReference(R.string.common_warning),
            message = when (unsupportedState) {
                is CurrencyUnsupportedState.Token.NetworkTokensUnsupported -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
                is CurrencyUnsupportedState.Token.UnsupportedCurve -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_curve_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
                is CurrencyUnsupportedState.UnsupportedNetwork -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_curve_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
            },
        )

        messageSender.send(message)
    }

    private suspend fun showRemoveNetworkWarning(
        currency: ManagedCryptoCurrency,
        network: Network,
        isCoin: Boolean,
        onConfirm: () -> Unit,
    ) {
        val userWalletId = state.value.userWalletId
        val hasLinkedTokens = if (userWalletId == null || !isCoin) {
            false
        } else {
            actions.checkHasLinkedTokens(userWalletId, network)
        }
        val canHideWithoutConfirming = source == ManageTokensSource.ONBOARDING

        if (hasLinkedTokens) {
            showLinkedTokensWarning(currency, network)
        } else if (canHideWithoutConfirming) {
            onConfirm()
        } else {
            showHideTokenWarning(currency, onConfirm)
        }
    }

    private fun showLinkedTokensWarning(currency: ManagedCryptoCurrency, network: Network) {
        val message = DialogMessage(
            title = resourceReference(
                id = R.string.token_details_unable_hide_alert_title,
                formatArgs = wrappedList(currency.name),
            ),
            message = resourceReference(
                id = R.string.token_details_unable_hide_alert_message,
                formatArgs = wrappedList(
                    currency.name,
                    currency.symbol,
                    network.name,
                ),
            ),
        )
        messageSender.send(message)
    }

    private fun showHideTokenWarning(currency: ManagedCryptoCurrency, onConfirm: () -> Unit) {
        val message = DialogMessage(
            title = resourceReference(
                id = R.string.token_details_hide_alert_title,
                formatArgs = wrappedList(currency.name),
            ),
            message = resourceReference(R.string.token_details_hide_alert_message),
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.token_details_hide_alert_hide),
                    warning = true,
                    onClick = onConfirm,
                )
            },
            secondActionBuilder = { cancelAction() },
        )

        messageSender.send(message)
    }

    private fun Batch<Int, List<ManagedCryptoCurrency>>.currencyIndexById(id: ManagedCryptoCurrency.ID): Int {
        return data
            .indexOfFirst { it.id == id }
            .takeIf { it != -1 }
            ?: error("Currency with currency '$id' not found in batch #$key")
    }
}