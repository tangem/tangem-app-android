package com.tangem.domain.managetokens.repository

import com.tangem.domain.managetokens.model.ManageTokensListBatchFlow
import com.tangem.domain.managetokens.model.ManageTokensListBatchingContext

interface ManageTokensRepository {

    fun tokensFlow(context: ManageTokensListBatchingContext, batchSize: Int): ManageTokensListBatchFlow
}