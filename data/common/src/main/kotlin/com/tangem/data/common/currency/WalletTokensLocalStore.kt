package com.tangem.data.common.currency

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.wallet.UserWalletId

interface WalletTokensLocalStore {

    suspend fun addRemoteTokens(userWalletId: UserWalletId, tokens: List<UserTokensResponse.Token>)

    /** Makes [tokens] the whole token list of the wallet's accounts */
    suspend fun setTokens(userWalletId: UserWalletId, tokens: List<UserTokensResponse.Token>)
}