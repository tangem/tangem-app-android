package com.tangem.domain.jointaccount.signing

import arrow.core.Either
import com.tangem.domain.jointaccount.model.JointOperationSigningError
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Signs a [SafeTx] with the wallet's derived owner key: EIP-712 over the full structure in the domain
 * {chainId, verifyingContract = safeAddress}.
 */
interface JointOperationSigner {

    /** Returns the `0x`-prefixed 65-byte R‖S‖V signature, `v ∈ {27, 28}`, lowercase */
    suspend fun signOperation(
        userWalletId: UserWalletId,
        ownerKeyIndex: OwnerKeyIndex,
        safeTx: SafeTx,
        chainId: Long,
        safeAddress: String,
    ): Either<JointOperationSigningError, String>
}