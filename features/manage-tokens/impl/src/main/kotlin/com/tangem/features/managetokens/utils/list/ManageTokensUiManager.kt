package com.tangem.features.managetokens.utils.list

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.ContentMessage
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.features.managetokens.ui.dialog.HasLinkedTokensWarning
import com.tangem.features.managetokens.ui.dialog.HideTokenWarning
import com.tangem.features.managetokens.utils.mapper.toUiModel
import com.tangem.features.managetokens.utils.ui.toggleExpanded
import com.tangem.features.managetokens.utils.ui.update
import com.tangem.pagination.Batch
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal abstract class ManageTokensUiManager(
    private val messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    abstract val scope: CoroutineScope
    abstract val state: MutableStateFlow<ManageTokensListState>

    protected fun getUiBatches(
        newCurrencyBatches: List<Batch<Int, List<ManagedCryptoCurrency>>>,
        canEditItems: Boolean,
    ): List<Batch<Int, List<CurrencyItemUM>>> {
        val currentUiBatches = state.value.uiBatches
        val batches = currentUiBatches.toMutableList()

        newCurrencyBatches.forEach { (key, data) ->
            val indexToUpdate = currentUiBatches.indexOfFirst { it.key == key }

            if (indexToUpdate == -1) {
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

                batches.add(newBatch)
            } else {
                val uiBatchToUpdate = currentUiBatches[indexToUpdate]

                if (uiBatchToUpdate.data == data) {
                    return@forEach
                }

                val currentCurrencyBatches = state.value.currencyBatches
                val currencyBatch = currentCurrencyBatches[indexToUpdate]
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
                // TODO: [REDACTED_JIRA]
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
            addCurrency(batchKey, currency.id, source.id)
        } else {
            if (checkNeedToShowRemoveNetworkWarning(currency.id, source.id)) {
                showRemoveNetworkWarning(
                    currency = currency,
                    network = source.network,
                    isCoin = source is ManagedCryptoCurrency.SourceNetwork.Main,
                    onConfirm = {
                        removeCurrency(batchKey, currency.id, source.id)
                    },
                )
            } else {
                removeCurrency(batchKey, currency.id, source.id)
            }
        }
    }

    protected abstract fun addCurrency(batchKey: Int, currencyId: ManagedCryptoCurrency.ID, networkId: Network.ID)

    protected abstract fun removeCurrency(batchKey: Int, currencyId: ManagedCryptoCurrency.ID, networkId: Network.ID)

    protected abstract fun checkNeedToShowRemoveNetworkWarning(
        currencyId: ManagedCryptoCurrency.ID,
        networkId: Network.ID,
    ): Boolean

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
            checkHasLinkedTokens(userWalletId, network)
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

    protected abstract suspend fun checkHasLinkedTokens(userWalletId: UserWalletId, network: Network): Boolean

    private fun Batch<Int, List<ManagedCryptoCurrency>>.currencyIndexById(id: ManagedCryptoCurrency.ID): Int {
        return data
            .indexOfFirst { it.id == id }
            .takeIf { it != -1 }
            ?: error("Currency with currency '$id' not found in batch #$key")
    }
}