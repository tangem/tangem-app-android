package com.tangem.domain.wallets.delegate

import arrow.core.Either
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.models.UserWalletRemoteInfo

interface UserWalletsSyncDelegate {

    suspend fun syncWallet(userWalletId: UserWalletId, name: String): Either<UpdateWalletError, UserWallet>

    suspend fun syncWallets(list: List<UserWalletRemoteInfo>): Either<UpdateWalletError, Unit>
}
