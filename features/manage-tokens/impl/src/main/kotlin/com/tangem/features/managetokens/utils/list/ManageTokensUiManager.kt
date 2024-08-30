package com.tangem.features.managetokens.utils.list

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.ContentMessage
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.ui.dialog.HasLinkedTokensWarning
import com.tangem.features.managetokens.ui.dialog.HideTokenWarning
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

internal class ManageTokensUiManager(
    private val state: MutableStateFlow<ManageTokensListState>,
    private val messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
    private val scopeProvider: Provider<CoroutineScope>,
    private val actions: ManageTokensUiActions,
) {

    private val scope: CoroutineScope
        get() = scopeProvider()

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
            )

            batches.updateUiBatchesItem(
                indexToBatch = batchIndex to uiBatch,
                indexToItem = currencyIndex to updatedUiItem,
            )
        }
    }

    private fun selectNetwork(
        batchKey: Int,
        currency: ManagedCryptoCurrency,
        source: ManagedCryptoCurrency.SourceNetwork,
        isSelected: Boolean,
    ) = scope.launch(dispatchers.default) {
        if (currency !is ManagedCryptoCurrency.Token) return@launch

        if (isSelected) {
            actions.addCurrency(batchKey, currency, source.network)
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

        val message = ContentMessage { onDismiss ->
            if (hasLinkedTokens) {
                HasLinkedTokensWarning(
                    currency = currency,
                    network = network,
                    onDismiss = onDismiss,
                )
            } else {
                HideTokenWarning(
                    currency = currency,
                    onConfirm = {
                        onConfirm()
                        onDismiss()
                    },
                    onDismiss = onDismiss,
                )
            }
        }

        messageSender.send(message)
    }

    private fun Batch<Int, List<ManagedCryptoCurrency>>.currencyIndexById(id: ManagedCryptoCurrency.ID): Int {
        return data
            .indexOfFirst { it.id == id }
            .takeIf { it != -1 }
            ?: error("Currency with currency '$id' not found in batch #$key")
    }
}