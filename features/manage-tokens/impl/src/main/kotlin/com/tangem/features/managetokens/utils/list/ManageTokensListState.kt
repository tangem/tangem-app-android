package com.tangem.features.managetokens.utils.list

import com.tangem.domain.managetokens.model.ManageTokensListConfig
import com.tangem.domain.managetokens.model.ManageTokensUpdateAction
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction

internal typealias ManageTokensBatchAction = BatchAction<Int, ManageTokensListConfig, ManageTokensUpdateAction>

internal data class ManageTokensListState(
    val userWalletId: UserWalletId? = null,
    val uiBatches: List<Batch<Int, List<CurrencyItemUM>>> = mutableListOf(),
    val currencyBatches: List<Batch<Int, List<ManagedCryptoCurrency>>> = mutableListOf(),
    val canEditItems: Boolean = true,
) {

    fun batchIndexByCurrencyId(currencyId: ManagedCryptoCurrency.ID): Int {
        return currencyBatches
            .indexOfFirst { batch -> batch.data.any { it.id == currencyId } }
            .takeIf { it != -1 }
            ?: error("Batch with currency '$currencyId' not found")
    }

    fun updateUiBatchesItem(
        indexToBatch: Pair<Int, Batch<Int, List<CurrencyItemUM>>>,
        indexToItem: Pair<Int, CurrencyItemUM>,
    ): ManageTokensListState {
        val updatedUiBatch = indexToBatch.second.copy(
            data = indexToBatch.second.data.toMutableList().apply {
                set(indexToItem.first, indexToItem.second)
            },
        )

        return copy(
            uiBatches = uiBatches.toMutableList().apply {
                set(indexToBatch.first, updatedUiBatch)
            },
        )
    }
}
