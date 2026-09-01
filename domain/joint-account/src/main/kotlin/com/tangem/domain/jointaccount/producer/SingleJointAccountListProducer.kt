package com.tangem.domain.jointaccount.producer

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.wallet.UserWalletId

interface SingleJointAccountListProducer : FlowProducer<List<JointAccount>> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, SingleJointAccountListProducer>
}