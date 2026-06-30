package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.gasless.EthereumGaslessDataProvider
import com.tangem.blockchain.blockchains.ethereum.models.EIP7702AuthorizationData
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.extensions.normalizeByteArray
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.models.GaslessTransactionData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigInteger

class CreateAndSendGaslessTransactionUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val getHotWalletSigner: (UserWallet.Hot) -> TransactionSigner,
    private val isGaslessV2Enabled: Boolean,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        transactionData: TransactionData,
        fee: TransactionFeeExtended,
    ): Either<SendTransactionError, String> = either {
        catch(
            block = {
                val uncompiledTxData = validateTransactionData(transactionData)
                val context = prepareGaslessContext(userWallet, uncompiledTxData, fee)
                val signedData = signGaslessTransactionByUser(userWallet, context, uncompiledTxData)
                signAndSendTransactionOnBackend(context, signedData, uncompiledTxData)
            },
            catch = {
                raise(SendTransactionError.DataError(it.message))
            },
        )
    }

    /**
     * Validates and casts transaction data to Uncompiled type.
     */
    private fun validateTransactionData(transactionData: TransactionData): TransactionData.Uncompiled {
        return transactionData as? TransactionData.Uncompiled
            ?: error("Uncompiled transaction data required")
    }

    /**
     * Prepares all necessary context for gasless transaction.
     * Includes: wallet manager, gasless provider, token status, nonce, transaction data.
     *
     * When the resolved fee plan is [GaslessFeePlan.TokenPayWithYieldWithdraw], the payload is a
     * [GaslessPayload.Batch] with the user's main tx at index 0 and the yield-withdraw tx at index 1.
     * [GaslessFeePlan.TokenPay] and a null plan produce a [GaslessPayload.Single] with the same
     * single-transaction behavior as before. [GaslessFeePlan.NativePay] must never reach this use
     * case — it is guarded in [assembleGaslessPayload].
     */
    private suspend fun prepareGaslessContext(
        userWallet: UserWallet,
        transactionData: TransactionData.Uncompiled,
        fee: TransactionFeeExtended,
    ): GaslessContext {
        val currency = singleAccountListSupplier.getSyncOrNull(userWalletId = userWallet.walletId)
            .getCryptoCurrency(currencyId = fee.feeTokenId, network = null)
            .getOrElse { error("Token for fee not found") }

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWallet.walletId,
            currency.network,
        ) ?: error("WalletManager not found for network ${currency.network.id}")

        val gaslessDataProvider = walletManager as? EthereumGaslessDataProvider ?: error(
            "WalletManager for network ${currency.network.id} " +
                "does not support gasless transactions",
        )

        val gaslessContractNonce = getContractNonce(gaslessDataProvider, transactionData.sourceAddress)

        val mainTxGasLimit = fee.mainTransactionGasLimit
            ?: error("Main transaction gas limit is required for a gasless (token-fee) transaction")
        val mainTx = buildTransaction(transactionData, mainTxGasLimit)
        val feeObj = buildFee(fee, currency)

        val payload = assembleGaslessPayload(
            mainTx = mainTx,
            feeObj = feeObj,
            nonce = gaslessContractNonce,
            plan = fee.gaslessFeePlan,
            withdrawGasLimit = fee.withdrawGasLimit,
        )

        val chainId = gaslessTransactionRepository.getChainIdForNetwork(currency.network)

        return GaslessContext(
            walletManager = walletManager,
            gaslessDataProvider = gaslessDataProvider,
            currency = currency,
            payload = payload,
            chainId = chainId,
        )
    }

    /**
     * Gets contract nonce with fallback to zero on failure.
     */
    private suspend fun getContractNonce(
        gaslessDataProvider: EthereumGaslessDataProvider,
        userAddress: String,
    ): BigInteger {
        return when (val nonceResult = gaslessDataProvider.getGaslessContractNonce(userAddress)) {
            is Result.Failure -> BigInteger.ZERO
            is Result.Success -> nonceResult.data
        }
    }

    /**
     * Signs gasless transaction and EIP-7702 authorization.
     * Returns prepared signatures and authorization data.
     *
     * EIP-712 typed data is constructed from the payload:
     * - [GaslessPayload.Single] → [Eip712TypedDataBuilder.build] (single-transaction schema)
     * - [GaslessPayload.Batch]  → [Eip712TypedDataBuilder.buildBatch] (batch schema)
     */
    private suspend fun signGaslessTransactionByUser(
        userWallet: UserWallet,
        context: GaslessContext,
        transactionData: TransactionData.Uncompiled,
    ): SignedGaslessData {
        val eip712Data = when (val payload = context.payload) {
            is GaslessPayload.Single -> Eip712TypedDataBuilder.build(
                gaslessTransaction = payload.data,
                chainId = context.chainId,
                verifyingContract = transactionData.sourceAddress,
                includeGasLimit = isGaslessV2Enabled,
            )
            is GaslessPayload.Batch -> Eip712TypedDataBuilder.buildBatch(
                gaslessBatch = payload.data,
                chainId = context.chainId,
                verifyingContract = transactionData.sourceAddress,
                includeGasLimit = isGaslessV2Enabled,
            )
        }

        val eip712HashToSign = EthereumUtils.makeTypedDataHash(eip712Data)
        val eip7702Data = getEIP7702DataForGasless(context.gaslessDataProvider)

        val signedHashes = signHashes(
            userWallet = userWallet,
            walletManager = context.walletManager,
            hashesToOperate = HashesToOperate(
                eip712Hash = eip712HashToSign,
                eip7702Hash = eip7702Data.data,
            ),
        )

        val decompressedPublicKey = context.walletManager
            .wallet.publicKey.blockchainKey
            .toDecompressedPublicKey()

        val preparedEip712Hash = UnmarshalHelper.unmarshalSignatureExtended(
            signature = signedHashes.eip712Hash,
            hash = eip712HashToSign,
            publicKey = decompressedPublicKey,
        ).asRSVLegacyEVM().toHexString().formatHex().lowercase()

        val extendedEip7702Data = UnmarshalHelper.unmarshalSignatureExtended(
            signature = signedHashes.eip7702Hash,
            hash = eip7702Data.data,
            publicKey = decompressedPublicKey,
        )

        val eip7702Auth = Eip7702Authorization(
            chainId = context.chainId,
            address = eip7702Data.executorAddress,
            nonce = eip7702Data.nonce,
            yParity = extendedEip7702Data.recId,
            r = extendedEip7702Data.r.toFormattedHex(bytes = 32),
            s = extendedEip7702Data.s.toFormattedHex(bytes = 32),
        )

        return SignedGaslessData(
            eip712Signature = preparedEip712Hash,
            eip7702Auth = eip7702Auth,
        )
    }

    /**
     * Sends gasless transaction to the service.
     *
     * Routes to the appropriate repository call based on payload type:
     * - [GaslessPayload.Single] → [GaslessTransactionRepository.signGaslessTransaction]
     * - [GaslessPayload.Batch]  → [GaslessTransactionRepository.signGaslessBatchTransaction]
     *
     * Pending-transaction tracking is always keyed on the main (user's) transaction only.
     */
    private suspend fun signAndSendTransactionOnBackend(
        context: GaslessContext,
        signedData: SignedGaslessData,
        transactionData: TransactionData.Uncompiled,
    ): String {
        val txHash = when (val payload = context.payload) {
            is GaslessPayload.Single -> gaslessTransactionRepository.signGaslessTransaction(
                network = context.currency.network,
                gaslessTransactionData = payload.data,
                signature = signedData.eip712Signature,
                userAddress = transactionData.sourceAddress,
                eip7702Auth = signedData.eip7702Auth,
            ).txHash
            is GaslessPayload.Batch -> gaslessTransactionRepository.signGaslessBatchTransaction(
                network = context.currency.network,
                gaslessBatchTransactionData = payload.data,
                signature = signedData.eip712Signature,
                userAddress = transactionData.sourceAddress,
                eip7702Auth = signedData.eip7702Auth,
            ).txHash
        }

        (context.walletManager as? PendingTransactionHandler)?.addPendingGaslessTransaction(
            transactionData = transactionData,
            txHash = txHash,
        )

        return txHash
    }

    private suspend fun signHashes(
        userWallet: UserWallet,
        walletManager: WalletManager,
        hashesToOperate: HashesToOperate,
    ): HashesToOperate {
        val signer = getSigner(userWallet)
        val hashesToSign = listOf(hashesToOperate.eip712Hash, hashesToOperate.eip7702Hash)

        return when (val signerResult = signer.sign(hashesToSign, walletManager.wallet.publicKey)) {
            is CompletionResult.Failure -> error("Signing failed: ${signerResult.error.message ?: "sdk error"}")
            is CompletionResult.Success -> {
                val signatures = signerResult.data
                require(signatures.size == 2) { "Expected 2 signatures, got ${signatures.size}" }
                HashesToOperate(
                    eip712Hash = signatures[0],
                    eip7702Hash = signatures[1],
                )
            }
        }
    }

    private fun getSigner(userWallet: UserWallet): TransactionSigner {
        return when (userWallet) {
            is UserWallet.Cold -> {
                val card = userWallet.scanResponse.card
                val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins

                cardSdkConfigRepository.getCommonSigner(
                    cardId = card.cardId.takeIf { isCardNotBackedUp },
                    twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
                    userWalletId = userWallet.walletId,
                )
            }
            is UserWallet.Hot -> getHotWalletSigner(userWallet)
        }
    }

    private fun buildTransaction(
        transactionData: TransactionData.Uncompiled,
        gasLimit: BigInteger,
    ): GaslessTransactionData.Transaction {
        val callData = (transactionData.extras as? EthereumTransactionExtras)?.callData
            ?: error("Ethereum call data is required")

        // Native amount is always zero in gasless transactions for now
        // we don't support gasless transfers of native currency
        val nativeAmount = BigInteger.ZERO

        return GaslessTransactionData.Transaction(
            to = getDestinationAddress(transactionData),
            value = nativeAmount,
            gasLimit = gasLimit,
            data = callData.data,
        )
    }

    private suspend fun buildFee(txFee: TransactionFeeExtended, currency: CryptoCurrency): GaslessTransactionData.Fee {
        val tokenForFee = currency as? CryptoCurrency.Token
            ?: error("Fee currency must be a token")

        val feeInTokenCurrency = txFee.transactionFee.normal as? Fee.Ethereum.TokenCurrency
            ?: error("Fee must be in token currency")

        val maxTokenFeeAmount = feeInTokenCurrency.amount
        val maxTokenFee = maxTokenFeeAmount.value?.movePointRight(maxTokenFeeAmount.decimals)?.toBigInteger()
            ?: error("Max token fee amount is null")
        return GaslessTransactionData.Fee(
            feeToken = tokenForFee.contractAddress,
            maxTokenFee = maxTokenFee,
            coinPriceInToken = feeInTokenCurrency.coinPriceInToken,
            feeTransferGasLimit = feeInTokenCurrency.feeTransferGasLimit,
            baseGas = feeInTokenCurrency.baseGas,
            feeReceiver = gaslessTransactionRepository.getTokenFeeReceiverAddress(),
        )
    }

    private suspend fun getEIP7702DataForGasless(
        gaslessDataProvider: EthereumGaslessDataProvider,
    ): EIP7702AuthorizationData {
        return when (val dataResult = gaslessDataProvider.prepareEIP7702AuthorizationData(isV2 = isGaslessV2Enabled)) {
            is Result.Failure -> throw dataResult.error
            is Result.Success -> dataResult.data
        }
    }

    /**
     * Discriminated union of the gasless transaction payload to sign and send.
     *
     * [Single] carries a single-transaction payload (the pre-existing path).
     * [Batch] carries a batch payload where the yield-withdraw call is appended as the second
     * transaction so that staked tokens are unlocked before the fee is settled.
     */
    internal sealed interface GaslessPayload {
        /** Single-transaction path — behavior is identical to the original implementation. */
        data class Single(val data: GaslessTransactionData) : GaslessPayload

        /**
         * Batch path — used when [GaslessFeePlan.TokenPayWithYieldWithdraw] is resolved.
         * [data.transactions] has the user's main tx at index 0 and the withdraw tx at index 1.
         */
        data class Batch(val data: GaslessBatchTransactionData) : GaslessPayload
    }

    /**
     * Context containing all prepared data for gasless transaction.
     */
    private data class GaslessContext(
        val walletManager: WalletManager,
        val gaslessDataProvider: EthereumGaslessDataProvider,
        val currency: CryptoCurrency,
        val payload: GaslessPayload,
        val chainId: Int,
    )

    /**
     * Signed data ready for submission.
     */
    private data class SignedGaslessData(
        val eip712Signature: String,
        val eip7702Auth: Eip7702Authorization,
    )

    private data class HashesToOperate(
        val eip712Hash: ByteArray,
        val eip7702Hash: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HashesToOperate

            if (!eip712Hash.contentEquals(other.eip712Hash)) return false
            if (!eip7702Hash.contentEquals(other.eip7702Hash)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = eip712Hash.contentHashCode()
            result = 31 * result + eip7702Hash.contentHashCode()
            return result
        }
    }

    internal companion object {

        /**
         * Assembles the [GaslessPayload] from already-built domain objects and the resolved fee plan.
         *
         * Dispatch rules:
         * - [GaslessFeePlan.TokenPayWithYieldWithdraw] → [GaslessPayload.Batch]: the yield-withdraw
         *   call is appended as the second transaction so that the fee token balance is topped up
         *   before the gasless service processes the fee.
         * - [GaslessFeePlan.TokenPay] or `null` → [GaslessPayload.Single]: single-transaction path,
         *   identical to the original implementation. `null` is a legitimate value meaning the plan
         *   was not explicitly resolved.
         * - [GaslessFeePlan.NativePay] → error: native-pay fees must never reach this use case
         *   (they are handled by the standard send path).
         */
        internal fun assembleGaslessPayload(
            mainTx: GaslessTransactionData.Transaction,
            feeObj: GaslessTransactionData.Fee,
            nonce: BigInteger,
            plan: GaslessFeePlan?,
            withdrawGasLimit: BigInteger?,
        ): GaslessPayload = when (plan) {
            is GaslessFeePlan.TokenPayWithYieldWithdraw -> GaslessPayload.Batch(
                GaslessBatchTransactionData(
                    transactions = listOf(
                        mainTx,
                        GaslessTransactionData.Transaction(
                            to = plan.yieldModuleAddress,
                            value = BigInteger.ZERO,
                            gasLimit = withdrawGasLimit
                                ?: error("Withdraw gas limit is required for a yield-withdraw batch"),
                            data = plan.withdrawCallData.data,
                        ),
                    ),
                    fee = feeObj,
                    nonce = nonce,
                ),
            )
            is GaslessFeePlan.TokenPay, null -> GaslessPayload.Single(
                GaslessTransactionData(transaction = mainTx, fee = feeObj, nonce = nonce),
            )
            is GaslessFeePlan.NativePay -> error("NativePay must not reach the gasless send path")
        }

        fun BigInteger.toFormattedHex(bytes: Int): String {
            return toByteArray().normalizeByteArray(bytes).toHexString().formatHex()
        }

        /**
         * Resolves the on-chain `to` for the user's main gasless sub-call.
         *
         * - Yield-supply send (`EthereumYieldSupplySendCallData`, selector 0x0779afe6): `send(token, dest,
         *   amount)` is a method ON the user's yield module — the executor must CALL the module (it holds the
         *   staked funds and routes the transfer); the recipient is already encoded inside the call data.
         *   [TransactionData.Uncompiled.destinationAddress] is patched to the module address in
         *   `DefaultTransactionRepository.createTransaction`, mirroring the non-gasless send path (and the
         *   withdraw sub-call's `to`). Reading `ethereumCallData.destinationAddress` (the recipient) instead
         *   makes the executor call a plain address with the module's calldata, reverting the whole batch with
         *   GAS_ESTIMATION_FAILED / require(false).
         * - Otherwise (e.g. ERC-20 transfer): `to` is the contract the calldata runs against
         *   ([TransactionData.Uncompiled.contractAddress], the token contract).
         */
        internal fun getDestinationAddress(txData: TransactionData.Uncompiled): String {
            val ethereumCallData = (txData.extras as? EthereumTransactionExtras)?.callData
            return if (ethereumCallData is EthereumYieldSupplySendCallData) {
                txData.destinationAddress
            } else {
                txData.contractAddress ?: error("supports only Token transaction with contract address")
            }
        }
    }
}