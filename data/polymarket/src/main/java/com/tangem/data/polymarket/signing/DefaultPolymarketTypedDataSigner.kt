package com.tangem.data.polymarket.signing

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.polymarket.builder.PolymarketTypedDataBuilder
import com.tangem.data.polymarket.derivation.PolymarketAddressFactory
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
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
            val seedKey = userWallet.secp256k1SeedKey()
                ?: return@catchOn PolymarketSigningError.MissingWallet.left()
            val ownerKey = ownerKey(userWalletId, ByteArrayKey(seedKey))
                ?: return@catchOn PolymarketSigningError.NotDerived.left()

            val publicKey = publicKey(seedKey = seedKey, ownerKey = ownerKey)
            val clobDigest = EthereumUtils.makeTypedDataHash(
                PolymarketTypedDataBuilder.buildClobAuth(
                    address = addressFactory.createAddress(ownerKey),
                    timestamp = clobAuth.timestamp,
                    nonce = clobAuth.nonce,
                ),
            )
            val batchDigest = EthereumUtils.makeTypedDataHash(
                PolymarketTypedDataBuilder.buildApprovalsBatch(
                    depositWallet = approvals.depositWalletAddress,
                    nonce = approvals.nonce,
                    deadline = approvals.deadline,
                    calls = approvals.calls,
                ),
            )

            val hashes = listOf(clobDigest, batchDigest)
            val signatures = when (val result = signer(userWallet).sign(hashes, publicKey)) {
                is CompletionResult.Success -> result.data
                is CompletionResult.Failure -> return@catchOn result.error.toSigningError().left()
            }

            PolymarketOnboardingSignatures(
                l1Signature = formatter.format(signatures[0], clobDigest, publicKey),
                batchSignature = formatter.format(signatures[1], batchDigest, publicKey),
            ).right()
        }
        .getOrElse { it.toSigningError().left() }

    private suspend fun ownerKey(userWalletId: UserWalletId, seedKey: ByteArrayKey): ExtendedPublicKey? =
        derivationsRepository.getExistingDerivedKeys(userWalletId, seedKey)[ownerPath]

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

    private fun UserWallet.secp256k1SeedKey(): ByteArray? = when (this) {
        is UserWallet.Cold -> scanResponse.card.wallets
            .firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?.publicKey
        is UserWallet.Hot -> wallets
            ?.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?.publicKey
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