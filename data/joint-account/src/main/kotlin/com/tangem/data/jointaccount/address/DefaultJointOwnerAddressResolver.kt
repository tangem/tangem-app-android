package com.tangem.data.jointaccount.address

import arrow.core.Either
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.jointaccount.signing.JointOwnerKeyReader
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

internal class DefaultJointOwnerAddressResolver @Inject constructor(
    private val ownerKeyReader: JointOwnerKeyReader,
    private val dispatchers: CoroutineDispatcherProvider,
) : JointOwnerAddressResolver {

    override suspend fun resolve(
        userWalletId: UserWalletId,
        ownerKeyIndex: OwnerKeyIndex,
    ): Either<Throwable, String?> = Either.catchOn(dispatchers.io) {
        val ownerKey = ownerKeyReader.read(userWalletId = userWalletId, ownerKeyIndex = ownerKeyIndex)
            ?: return@catchOn null

        Blockchain.Ethereum.makeAddressesFromExtendedPublicKey(
            extendedPublicKey = ownerKey.extendedPublicKey,
            rawPath = null,
            cachedIndex = null,
        ).address
    }
}