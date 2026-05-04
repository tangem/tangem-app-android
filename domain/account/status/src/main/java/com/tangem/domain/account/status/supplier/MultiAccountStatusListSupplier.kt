package com.tangem.domain.account.status.supplier

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.MultiAccountStatusListProducer
import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Supplier that provides a list of [AccountStatusList]s for all user wallets.
 *
[REDACTED_AUTHOR]
 */
abstract class MultiAccountStatusListSupplier(
    override val factory: MultiAccountStatusListProducer.Factory,
    override val keyCreator: (Unit) -> String,
) : FlowCachingSupplier<MultiAccountStatusListProducer, Unit, List<AccountStatusList>>() {

    operator fun invoke(): Flow<List<AccountStatusList>> {
        return super.invoke(params = Unit)
    }

    fun invokeAsMap(): Flow<LinkedHashMap<UserWalletId, AccountStatusList>> = invoke()
        .map { accountLists ->
            accountLists.associateByTo(
                destination = linkedMapOf(),
                keySelector = { accountList -> accountList.userWalletId },
            )
        }
}