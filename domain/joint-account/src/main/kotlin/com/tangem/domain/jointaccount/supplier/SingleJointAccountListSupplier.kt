package com.tangem.domain.jointaccount.supplier

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.producer.SingleJointAccountListProducer
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

open class SingleJointAccountListSupplier(
    override val factory: SingleJointAccountListProducer.Factory,
    override val keyCreator: (SingleJointAccountListProducer.Params) -> String,
) : FlowCachingSupplier<SingleJointAccountListProducer, SingleJointAccountListProducer.Params, List<JointAccount>>() {

    operator fun invoke(userWalletId: UserWalletId): Flow<List<JointAccount>> {
        return invoke(params = SingleJointAccountListProducer.Params(userWalletId))
    }
}

suspend fun SingleJointAccountListSupplier.find(accountId: AccountId): JointAccount? {
    return invoke(userWalletId = accountId.userWalletId).firstOrNull().orEmpty()
        .firstOrNull { it.cryptoAccountId.equals(accountId.value, ignoreCase = true) }
}

suspend fun SingleJointAccountListSupplier.findByMember(
    userWalletId: UserWalletId,
    ownerAddress: String,
): JointAccount? {
    return invoke(userWalletId = userWalletId).firstOrNull().orEmpty()
        .firstOrNull { account -> account.members.any { it.address.equals(ownerAddress, ignoreCase = true) } }
}