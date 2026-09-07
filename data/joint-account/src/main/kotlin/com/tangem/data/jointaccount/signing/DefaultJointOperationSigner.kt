package com.tangem.data.jointaccount.signing

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Wallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.jointaccount.model.JointOperationSigningError
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.SafeTxTypedDataBuilder
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SignHashesError
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

/**
 * Computes the EIP-712 digest locally via [SafeTxTypedDataBuilder], signs it with the derived owner key
 * in one [SignUseCase] session and formats the result into the contract's 65-byte R‖S‖V form.
 */
internal class DefaultJointOperationSigner @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivationsRepository: DerivationsRepository,
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
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
            val publicKey = ownerPublicKey(userWallet = userWallet, ownerKeyIndex = ownerKeyIndex)
                .getOrElse { return@catchOn it.left() }

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

    private suspend fun ownerPublicKey(
        userWallet: UserWallet,
        ownerKeyIndex: OwnerKeyIndex,
    ): Either<JointOperationSigningError, Wallet.PublicKey> {
        val seedKey = userWallet.secp256k1SeedKey()
            ?: return JointOperationSigningError.Failed(
                cause = IllegalStateException("The wallet has no secp256k1 seed key"),
            ).left()

        val ownerPath = jointAccountOwnerDerivationPath(index = ownerKeyIndex.value)
        val ownerKey = derivationsRepository
            .getExistingDerivedKeys(userWalletId = userWallet.walletId, seedKey = ByteArrayKey(seedKey))[ownerPath]
            ?: return JointOperationSigningError.OwnerKeyNotDerived.left()

        return Wallet.PublicKey(
            seedKey = seedKey,
            derivationType = Wallet.PublicKey.DerivationType.Plain(
                Wallet.HDKey(extendedPublicKey = ownerKey, path = ownerPath),
            ),
        ).right()
    }

    private fun UserWallet.secp256k1SeedKey(): ByteArray? = when (this) {
        is UserWallet.Cold -> scanResponse.card.wallets
            .firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?.publicKey
        is UserWallet.Hot -> wallets
            ?.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?.publicKey
    }
}