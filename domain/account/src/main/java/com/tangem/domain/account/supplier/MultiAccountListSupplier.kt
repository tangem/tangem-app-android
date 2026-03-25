package com.tangem.domain.account.supplier

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.MultiAccountListProducer
import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Supplier that provides a list of [AccountList]s for all user wallets.
 *
[REDACTED_AUTHOR]
 */
abstract class MultiAccountListSupplier(
    override val factory: MultiAccountListProducer.Factory,
    override val keyCreator: (Unit) -> String,
) : FlowCachingSupplier<MultiAccountListProducer, Unit, List<AccountList>>() {

    operator fun invoke(): Flow<List<AccountList>> {
        return super.invoke(params = Unit)
    }

    fun invokeMap(): Flow<LinkedHashMap<UserWalletId, AccountList>> = invoke()
        .map { accountLists ->
            accountLists.associateByTo(
                destination = linkedMapOf(),
                keySelector = { accountList -> accountList.userWalletId },
            )
        }
}