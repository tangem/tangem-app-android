package com.tangem.domain.tokens.store

import com.tangem.domain.models.userwallet.UserWalletId
import java.util.concurrent.ConcurrentHashMap

typealias TokensStoreHolder = ConcurrentHashMap<UserWalletId, TokensStore>

object SingletonTokensStoreHolder : TokensStoreHolder()

class ScopedTokensStoreHolder : TokensStoreHolder() {

    override fun put(key: UserWalletId, value: TokensStore): TokensStore? {
        SingletonTokensStoreHolder[key] = value
        return super.put(key, value)
    }
}
