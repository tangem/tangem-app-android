package com.tangem.data.jointaccount.signing

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.Wallet
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.jointaccount.model.JointOperationSigningError
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.data.jointaccount.safe.SafeTxTypedDataBuilder
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SignHashesError
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

/**
 * Computes the EIP-712 digest locally via [SafeTxTypedDataBuilder], signs it with the derived owner key
 * in one [SignUseCase] session and formats the result into the contract's 65-byte R‖S‖V form.
 */
internal class DefaultJointOperationSigner @Inject constructor(
    private val ownerKeyReader: JointOwnerKeyReader,
    private val signUseCase: SignUseCase,
    private val formatter: JointSignatureFormatter,
    private val dispatchers: CoroutineDispatcherProvider,
) : JointOperationSigner {

    override suspend fun signOperation(
        userWalletId: UserWalletId,
        ownerKeyIndex: OwnerKeyIndex,
        safeTx: SafeTx,
        chainId: Long,
        safeAddress: String,
    ): Either<JointOperationSigningError, String> = Either
        .catchOn(dispatchers.io) {
            val ownerKey = ownerKeyReader.read(userWalletId = userWalletId, ownerKeyIndex = ownerKeyIndex)
                ?: return@catchOn JointOperationSigningError.OwnerKeyNotDerived.left()
            val userWallet = ownerKey.userWallet

            val publicKey = Wallet.PublicKey(
                seedKey = ownerKey.seedKey,
                derivationType = Wallet.PublicKey.DerivationType.Plain(
                    Wallet.HDKey(extendedPublicKey = ownerKey.extendedPublicKey, path = ownerKey.path),
                ),
            )

            val digest = SafeTxTypedDataBuilder.digest(
                safeTx = safeTx,
                chainId = chainId,
                verifyingContract = safeAddress,
            )

            signUseCase(hashes = listOf(digest), publicKey = publicKey, userWallet = userWallet)
                .map { signatures -> formatter.format(signatures.single(), digest, publicKey) }
                .mapLeft { error ->
                    when (error) {
                        SignHashesError.Cancelled -> JointOperationSigningError.UserCancelled
                        SignHashesError.NoSigningKey,
                        is SignHashesError.SigningFailed,
                        -> JointOperationSigningError.Failed(
                            cause = IllegalStateException("Signing a joint operation failed: $error"),
                        )
                    }
                }
        }
        .getOrElse { JointOperationSigningError.Failed(cause = it).left() }
}