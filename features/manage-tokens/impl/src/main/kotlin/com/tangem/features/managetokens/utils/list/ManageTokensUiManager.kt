package com.tangem.features.managetokens.utils.list

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.utils.mapper.toUiModel
import com.tangem.features.managetokens.utils.ui.update
import com.tangem.pagination.Batch
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class ManageTokensUiManager(
    private val state: MutableStateFlow<ManageTokensListState>,
    private val dispatchers: CoroutineDispatcherProvider,
    private val scope: CoroutineScope,
    private val actions: ManageTokensUiActions,
    private val manageTokensWarningDelegate: ManageTokensWarningDelegate,
) {

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
                            onTokenClick = actions::onTokenClick,
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
                                onTokenClick = actions::onTokenClick,
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
        manageTokensWarningDelegate.showRemoveNetworkWarning(
            currency = currency,
            network = currency.network,
            isCoin = currency is ManagedCryptoCurrency.Custom.Coin,
            onConfirm = { actions.removeCustomCurrency(currency = currency) },
        )
    }
}