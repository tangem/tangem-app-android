package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@ModelScoped
internal class MultiWalletTokenListStore @Inject constructor(
    private val getTokenListUseCase: GetTokenListUseCase,
) {

    private val flows: ConcurrentHashMap<UserWalletId, LceFlow<TokenListError, TokenList>> by lazy {
        ConcurrentHashMap()
    }

    fun addIfNot(userWalletId: UserWalletId, coroutineScope: CoroutineScope) {
        if (flows[userWalletId] != null) {
            Timber.d("Flow with token list for $userWalletId already exists")
            return
        }

        coroutineScope.ensureActive()

        flows[userWalletId] = getTokenListUseCase
            .launch(userWalletId)
            .shareIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1,
            )

        Timber.d("Flow with token list for $userWalletId created")
    }

    fun getOrThrow(userWalletId: UserWalletId): LceFlow<TokenListError, TokenList> {
        return requireNotNull(flows[userWalletId]) {
            "Flow with token list for $userWalletId doesn't exist"
        }
    }

    fun remove(userWalletId: UserWalletId) {
        flows.remove(userWalletId)

        Timber.d("Flow with token list for $userWalletId removed")
    }

    fun clear() {
        flows.clear()

        Timber.d("All flows with token list cleared")
    }
}