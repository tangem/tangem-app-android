package com.tangem.domain.managetokens

import com.tangem.domain.managetokens.model.ManageTokensListBatchFlow
import com.tangem.domain.managetokens.model.ManageTokensListBatchingContext
import com.tangem.domain.managetokens.repository.ManageTokensRepository

class GetManagedTokensUseCase(
    private val repository: ManageTokensRepository,
) {

    operator fun invoke(
        context: ManageTokensListBatchingContext,
        // only for onboarding case, change carefully and check repository implementation
        loadUserTokensFromRemote: Boolean,
        batchSize: Int = 40,
    ): ManageTokensListBatchFlow {
        return repository.getTokenListBatchFlow(context, loadUserTokensFromRemote, batchSize)
    }
}