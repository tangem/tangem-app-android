package com.tangem.domain.managetokens

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.pagination.Batch
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class GetDistinctManagedCurrenciesUseCase(
    private val coroutineDispatchersProvider: CoroutineDispatcherProvider,
) {

    // FIXME: Add interception functionality to BatchFlow state and call this on domain
    //  [REDACTED_JIRA]
    suspend operator fun invoke(
        batches: List<Batch<Int, List<ManagedCryptoCurrency>>>,
    ): List<Batch<Int, List<ManagedCryptoCurrency>>> = withContext(coroutineDispatchersProvider.default) {
        val seenIds = mutableSetOf<ManagedCryptoCurrency.ID>()

        batches.map { batch ->
            val filtered = batch.data.filter { currency ->
                seenIds.add(currency.id)
            }
            batch.copy(data = filtered)
        }
    }
}