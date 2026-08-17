package com.tangem.domain.jointaccount.supplier

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.producer.SingleJointAccountListProducer
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

abstract class SingleJointAccountListSupplier(
    override val factory: SingleJointAccountListProducer.Factory,
    override val keyCreator: (SingleJointAccountListProducer.Params) -> String,
) : FlowCachingSupplier<SingleJointAccountListProducer, SingleJointAccountListProducer.Params, List<JointAccount>>() {

    operator fun invoke(userWalletId: UserWalletId): Flow<List<JointAccount>> {
        return invoke(params = SingleJointAccountListProducer.Params(userWalletId))
    }
}