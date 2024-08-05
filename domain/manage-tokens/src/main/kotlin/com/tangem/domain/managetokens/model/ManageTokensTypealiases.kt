package com.tangem.domain.managetokens.model

import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

typealias ManageTokensListBatchingContext = BatchingContext<Int, ManageTokensListConfig, ManageTokensUpdateAction>

typealias ManageTokensListBatchFlow = BatchFlow<Int, List<ManagedCryptoCurrency>, ManageTokensUpdateAction>