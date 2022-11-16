package com.tangem.tap.proxy

import com.tangem.crypto.UserWalletManager
import com.tangem.crypto.models.Token

class UserWalletManagerImpl : UserWalletManager {

    override fun getUserTokens(): List<Token> {
        TODO("Not yet implemented")
    }

    override fun getWalletId(): String {
        TODO("Not yet implemented")
    }

    override fun isTokenAdded(token: Token): Boolean {
        TODO("Not yet implemented")
    }

    override fun addToken(token: Token) {
        TODO("Not yet implemented")
    }

    override fun getWalletAddress(token: Token): String {
        TODO("Not yet implemented")
    }
}