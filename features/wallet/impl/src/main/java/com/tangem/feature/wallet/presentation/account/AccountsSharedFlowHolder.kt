package com.tangem.feature.wallet.presentation.account

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class AccountsSharedFlowHolder @Inject constructor(
    val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) {

    private val accountStatusList = mutableMapOf<UserWalletId, Flow<AccountStatusList>>()
    private var modelScope: CoroutineScope? = null

    fun init(modelScope: CoroutineScope) {
        this.modelScope = modelScope
    }

    fun getAccountStatusListFlow(userWalletId: UserWalletId): Flow<AccountStatusList> {
        return accountStatusList.getOrPut(
            key = userWalletId,
            defaultValue = {
                singleAccountStatusListSupplier(userWalletId)
                    .conflate()
                    .distinctUntilChanged()
                    .shareIn(
                        scope = requireNotNull(modelScope),
                        replay = 1,
                        started = SharingStarted.WhileSubscribed(
                            stopTimeoutMillis = 5_000,
                            replayExpirationMillis = 30_000,
                        ),
                    )
            },
        )
    }

    @Suppress("IgnoredReturnValue")
    fun remove(userWalletId: UserWalletId) {
        accountStatusList.remove(userWalletId)
    }

    fun clear() {
        accountStatusList.clear()
    }
}