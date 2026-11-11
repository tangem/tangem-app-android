package com.tangem.domain.collectibles.flow

import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.wallet.UserWalletId

interface CollectiblesActiveTxsProducer : FlowProducer<List<CollectibleActiveTx>> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, CollectiblesActiveTxsProducer>
}