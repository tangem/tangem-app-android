package com.tangem.data.managetokens.utils

import com.tangem.domain.managetokens.model.ManageTokensUpdateAction
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchUpdateFetcher
import com.tangem.pagination.BatchUpdateResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ManageTokensUpdateFetcher @Inject constructor() :
    BatchUpdateFetcher<Int, List<ManagedCryptoCurrency>, ManageTokensUpdateAction> {

    override suspend fun BatchUpdateFetcher.UpdateContext<Int, List<ManagedCryptoCurrency>>.fetchUpdateAsync(
        toUpdate: List<Batch<Int, List<ManagedCryptoCurrency>>>,
        updateRequest: ManageTokensUpdateAction,
    ) {
        when (updateRequest) {
            is ManageTokensUpdateAction.AddCurrency -> coroutineScope {
                val tasks = toUpdate.map { (key, data) ->
                    async {
                        val currencyIndex = data.indexOfFirst { it.id == updateRequest.currencyId }
                            .takeIf { it != -1 }
                            ?: error("Currency '${updateRequest.currencyId}' not found in batch #$key")
                        val updatedCurrency = when (val currency = data[currencyIndex]) {
                            is ManagedCryptoCurrency.Custom -> error("Can't add custom currency '${currency.id}'")
                            is ManagedCryptoCurrency.Token -> {
                                currency.copy(
                                    addedIn = if (updateRequest.isSelected) {
                                        currency.addedIn + updateRequest.networkId
                                    } else {
                                        currency.addedIn - updateRequest.networkId
                                    },
                                )
                            }
                        }

                        data.toMutableList().apply {
                            set(currencyIndex, updatedCurrency)
                        }
                    }
                }

                tasks.forEachIndexed { index, task ->
                    launch {
                        val updatedItems = task.await()

                        update {
                            BatchUpdateResult.Success(
                                data = mapNotNull {
                                    if (it.key == toUpdate[index].key) {
                                        Batch(it.key, updatedItems)
                                    } else {
                                        null
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
