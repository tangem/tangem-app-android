package com.tangem.data.polymarket.signing

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.polymarket.builder.PolymarketErc7739SignatureBuilder
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
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyEnvelope
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import com.tangem.domain.polymarket.signing.PredictionOrderPayload
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.extensions.toHexString
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import java.math.BigInteger
import javax.inject.Inject

@Suppress("LongParameterList")
internal class DefaultPolymarketTypedDataSigner @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivationsRepository: DerivationsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val hotSignerFactory: TangemHotWalletSigner.Factory,
    private val digestFactory: PolymarketDigestFactory,
    private val formatter: PolymarketSignatureFormatter,
    private val sessionKeyStore: PolymarketSessionKeyStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketTypedDataSigner {

    override suspend fun signOrder(
        userWalletId: UserWalletId,
        payload: PredictionOrderPayload,
    ): Either<PolymarketSigningError, PredictionSignedOrder> = Either
        .catchOn(dispatchers.io) {
            val context = signingContext(userWalletId).getOrElse { return@catchOn it.left() }
            val order = payload.toOrder()
            val digest = PolymarketErc7739SignatureBuilder.digest(
                order = order,
                domain = payload.draft.domain,
                depositWalletAddress = order.maker,
            )

            val signatures = sign(context, listOf(digest)).getOrElse { return@catchOn it.left() }
            val inner = formatter.format(signatures[0], digest, context.publicKey)

            PredictionSignedOrder(
                order = order,
                signature = PolymarketErc7739SignatureBuilder.wrap(
                    innerSignature = inner,
                    order = order,
                    domain = payload.draft.domain,
                ),
                domain = payload.draft.domain,
            ).right()
        }
        .getOrElse { it.toPolymarketSigningError().left() }

    override suspend fun signOrderWithSessionKey(
        userWalletId: UserWalletId,
        sessionKeyAddress: String,
        payload: PredictionOrderPayload,
    ): Either<PolymarketSigningError, PredictionSignedOrder> = Either
        .catchOn(dispatchers.io) {
            val order = payload.toOrder()
            val digest = PolymarketErc7739SignatureBuilder.digest(
                order = order,
                domain = payload.draft.domain,
                depositWalletAddress = order.maker,
            )
            val inner = sessionKeyStore.sign(userWalletId = userWalletId, address = sessionKeyAddress, digest = digest)
                .getOrElse { return@catchOn PolymarketSigningError.SessionKey(it).left() }
            val wrapped = PolymarketErc7739SignatureBuilder.wrap(
                innerSignature = inner.toHexString(),
                order = order,
                domain = payload.draft.domain,
            )

            PolymarketSessionKeyEnvelope.wrap(depositWalletSignature = wrapped, sessionKeyAddress = sessionKeyAddress)
                .map { envelope ->
                    PredictionSignedOrder(order = order, signature = envelope, domain = payload.draft.domain)
                }
                .mapLeft { error ->
                    when (error) {
                        is PolymarketSessionKeyEnvelope.Error.InvalidAddress -> PolymarketSigningError.SessionKey(
                            PolymarketSessionKeyError.InvalidAddress(error.address),
                        )
                        PolymarketSessionKeyEnvelope.Error.InvalidSignature -> PolymarketSigningError.Unknown
                    }
                }
        }
        .getOrElse { it.toPolymarketSigningError().left() }

    private fun PredictionOrderPayload.toOrder(): PredictionOrder = PredictionOrder(
        salt = salt,
        maker = depositWalletAddress,
        signer = depositWalletAddress,
        tokenId = BigInteger(draft.tokenId),
        makerAmount = draft.makerAmount,
        takerAmount = draft.takerAmount,
        side = draft.side,
        signatureType = PolymarketContracts.SIGNATURE_TYPE_DEPOSIT_WALLET,
        timestampMillis = timestampMillis,
        metadata = ZERO_WORD,
        builder = draft.builderCode,
    )

    override suspend fun signRelayedBatch(
        userWalletId: UserWalletId,
        payload: PolymarketApprovalsPayload,
    ): Either<PolymarketSigningError, String> = Either
        .catchOn(dispatchers.io) {
            val context = signingContext(userWalletId).getOrElse { return@catchOn it.left() }
            val digest = digestFactory.relayedBatch(payload)

            val signatures = sign(context, listOf(digest)).getOrElse { return@catchOn it.left() }

            formatter.format(signatures[0], digest, context.publicKey).right()
        }
        .getOrElse { it.toPolymarketSigningError().left() }

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

    private suspend fun sign(
        context: SigningContext,
        hashes: List<ByteArray>,
    ): Either<PolymarketSigningError, List<ByteArray>> =
        when (val result = signer(context.userWallet).sign(hashes, context.publicKey)) {
            is CompletionResult.Success -> result.data
                .takeIf { it.size == hashes.size }
                ?.right()
                ?: PolymarketSigningError.Unknown.left()
            is CompletionResult.Failure -> result.error.toPolymarketSigningError().left()
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

    private companion object {
        val ownerPath = DerivationPath(POLYMARKET_OWNER_DERIVATION_PATH)

        const val ZERO_WORD = "0x0000000000000000000000000000000000000000000000000000000000000000"
    }
}