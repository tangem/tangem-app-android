package com.tangem.domain.managetokens.repository

import com.tangem.domain.managetokens.model.ManageTokensListBatchFlow
import com.tangem.domain.managetokens.model.ManageTokensListBatchingContext

interface ManageTokensRepository {

    fun getTokenListBatchFlow(context: ManageTokensListBatchingContext, batchSize: Int): ManageTokensListBatchFlow
}