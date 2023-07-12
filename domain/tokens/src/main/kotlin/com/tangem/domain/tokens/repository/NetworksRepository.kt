package com.tangem.domain.tokens.repository

import arrow.core.Either
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

// FIXME: Use Raise as context instead of Effect when context receivers become stable
// https://tangem.atlassian.net/browse/AND-3947

/**
 * Repository for everything related to the blockchain networks
 * */
interface NetworksRepository {

    fun getNetworks(networksIds: Set<Network.ID>): Either<TokensError, Set<Network>>

    fun getNetworkStatuses(
        userWalletId: UserWalletId,
        networks: Map<Network.ID, Set<Token.ID>>,
        refresh: Boolean,
    ): Flow<Either<TokensError, Set<NetworkStatus>>>
}
