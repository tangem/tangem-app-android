package com.tangem.features.earn.lending.impl.supplier

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/** One Lending snapshot per wallet, shared by every reader instead of a subscription per screen */
internal class LendingSupplier(
    override val factory: LendingProducer.Factory,
    override val keyCreator: (LendingProducer.Params) -> String,
) : FlowCachingSupplier<LendingProducer, LendingProducer.Params, LendingSnapshot>() {

    operator fun invoke(userWalletId: UserWalletId): Flow<LendingSnapshot> =
        invoke(LendingProducer.Params(userWalletId))
}