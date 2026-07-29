package com.tangem.data.polymarket.signing

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.polymarket.builder.PolymarketTypedDataBuilder
import com.tangem.data.polymarket.derivation.PolymarketAddressFactory
import com.tangem.data.polymarket.secp256k1SeedKey
import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.derivation.POLYMARKET_OWNER_DERIVATION_PATH
import com.tangem.domain.polymarket.model.PolymarketOnboardingSignatures
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketClobAuthData
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Suppress("LongParameterList")
internal class DefaultPolymarketTypedDataSigner @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivationsRepository: DerivationsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val hotSignerFactory: TangemHotWalletSigner.Factory,
    private val addressFactory: PolymarketAddressFactory,
    private val formatter: PolymarketSignatureFormatter,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketTypedDataSigner {

    override suspend fun signOnboarding(
        userWalletId: UserWalletId,
        clobAuth: PolymarketClobAuthData,
        approvals: PolymarketApprovalsPayload,
    ): Either<PolymarketSigningError, PolymarketOnboardingSignatures> = Either
        .catchOn(dispatchers.io) {
            val context = signingContext(userWalletId).getOrElse { return@catchOn it.left() }
            val clobDigest = clobDigest(context, clobAuth)
            val batchDigest = EthereumUtils.makeTypedDataHash(
                PolymarketTypedDataBuilder.buildApprovalsBatch(
                    depositWallet = approvals.depositWalletAddress,
                    nonce = approvals.nonce,
                    deadline = approvals.deadline,
                    calls = approvals.calls,
                ),
            )

            val signatures = sign(context, listOf(clobDigest, batchDigest))
                .getOrElse { return@catchOn it.left() }

            PolymarketOnboardingSignatures(
                l1Signature = formatter.format(signatures[0], clobDigest, context.publicKey),
                batchSignature = formatter.format(signatures[1], batchDigest, context.publicKey),
            ).right()
        }
        .getOrElse { it.toSigningError().left() }

    override suspend fun signClobAuth(
        userWalletId: UserWalletId,
        clobAuth: PolymarketClobAuthData,
    ): Either<PolymarketSigningError, String> = Either
        .catchOn(dispatchers.io) {
            val context = signingContext(userWalletId).getOrElse { return@catchOn it.left() }
            val digest = clobDigest(context, clobAuth)
            val signatures = sign(context, listOf(digest)).getOrElse { return@catchOn it.left() }

            formatter.format(signatures[0], digest, context.publicKey).right()
        }
        .getOrElse { it.toSigningError().left() }

    private suspend fun signingContext(userWalletId: UserWalletId): Either<PolymarketSigningError, SigningContext> {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val seedKey = userWallet.secp256k1SeedKey() ?: return PolymarketSigningError.MissingWallet.left()
        val ownerKey = derivationsRepository.getExistingDerivedKeys(userWalletId, ByteArrayKey(seedKey))[ownerPath]
            ?: return PolymarketSigningError.NotDerived.left()

        return SigningContext(
            userWallet = userWallet,
            ownerKey = ownerKey,
            publicKey = publicKey(seedKey = seedKey, ownerKey = ownerKey),
        ).right()
    }

    private fun clobDigest(context: SigningContext, clobAuth: PolymarketClobAuthData): ByteArray =
        EthereumUtils.makeTypedDataHash(
            PolymarketTypedDataBuilder.buildClobAuth(
                address = addressFactory.createAddress(context.ownerKey),
                timestamp = clobAuth.timestamp,
                nonce = clobAuth.nonce,
            ),
        )

    private suspend fun sign(
        context: SigningContext,
        hashes: List<ByteArray>,
    ): Either<PolymarketSigningError, List<ByteArray>> =
        when (val result = signer(context.userWallet).sign(hashes, context.publicKey)) {
            is CompletionResult.Success -> result.data
                .takeIf { it.size == hashes.size }
                ?.right()
                ?: PolymarketSigningError.Unknown.left()
            is CompletionResult.Failure -> result.error.toSigningError().left()
        }

    private data class SigningContext(
        val userWallet: UserWallet,
        val ownerKey: ExtendedPublicKey,
        val publicKey: Wallet.PublicKey,
    )

    private fun publicKey(seedKey: ByteArray, ownerKey: ExtendedPublicKey): Wallet.PublicKey = Wallet.PublicKey(
        seedKey = seedKey,
        derivationType = Wallet.PublicKey.DerivationType.Plain(
            Wallet.HDKey(extendedPublicKey = ownerKey, path = ownerPath),
        ),
    )

    private fun signer(userWallet: UserWallet): TransactionSigner = when (userWallet) {
        is UserWallet.Hot -> hotSignerFactory.create(userWallet)
        is UserWallet.Cold -> {
            val card = userWallet.scanResponse.card
            val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins
            cardSdkConfigRepository.getCommonSigner(
                cardId = card.cardId.takeIf { isCardNotBackedUp },
                twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
                userWalletId = userWallet.walletId,
            )
        }
    }

    private fun Throwable.toSigningError(): PolymarketSigningError = when (this) {
        is TangemSdkError.UserCancelled -> PolymarketSigningError.UserCancelled
        is TangemSdkError.WalletNotFound -> PolymarketSigningError.MissingWallet
        is TangemSdkError -> PolymarketSigningError.CardError
        else -> PolymarketSigningError.Unknown
    }

    private companion object {
        val ownerPath = DerivationPath(POLYMARKET_OWNER_DERIVATION_PATH)
    }
}