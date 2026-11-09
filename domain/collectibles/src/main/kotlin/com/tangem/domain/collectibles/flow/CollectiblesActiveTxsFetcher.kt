package com.tangem.domain.collectibles.flow

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId

/** A failed read keeps the last answer in [CollectiblesActiveTxsSupplier]. */
interface CollectiblesActiveTxsFetcher : FlowFetcher<CollectiblesActiveTxsFetcher.Params> {

    data class Params(val userWalletId: UserWalletId)
}