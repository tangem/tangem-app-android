package com.tangem.domain.jointaccount.address

import arrow.core.Either
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWalletId

interface JointOwnerAddressResolver {

    suspend fun resolve(userWalletId: UserWalletId, ownerKeyIndex: OwnerKeyIndex): Either<Throwable, String?>
}