package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
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
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.domain.transaction.models.GaslessTransactionData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigInteger

class CreateAndSendGaslessTransactionUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val getHotWalletSigner: (UserWallet.Hot) -> TransactionSigner,
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
     */
    private suspend fun prepareGaslessContext(
        userWallet: UserWallet,
        transactionData: TransactionData.Uncompiled,
        fee: TransactionFeeExtended,
    ): GaslessContext {
        val tokenForFeeStatus = getSingleCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
            userWallet.walletId,
            fee.feeTokenId,
        ).getOrNull() ?: error("Token for fee not found")

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWallet.walletId,
            tokenForFeeStatus.currency.network,
        ) ?: error("WalletManager not found for network ${tokenForFeeStatus.currency.network.id}")

        val gaslessDataProvider = walletManager as? EthereumGaslessDataProvider ?: error(
            "WalletManager for network ${tokenForFeeStatus.currency.network.id} " +
                "does not support gasless transactions",
        )

        val gaslessContractNonce = getContractNonce(gaslessDataProvider, transactionData.sourceAddress)

        val gaslessTransactionData = createGaslessTransactionData(
            transactionData = transactionData,
            txFee = fee,
            tokenFeeStatus = tokenForFeeStatus,
            nonce = gaslessContractNonce,
        )

        val chainId = gaslessTransactionRepository.getChainIdForNetwork(tokenForFeeStatus.currency.network)

        return GaslessContext(
            walletManager = walletManager,
            gaslessDataProvider = gaslessDataProvider,
            tokenForFeeStatus = tokenForFeeStatus,
            gaslessTransactionData = gaslessTransactionData,
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
     */
    private suspend fun signGaslessTransactionByUser(
        userWallet: UserWallet,
        context: GaslessContext,
        transactionData: TransactionData.Uncompiled,
    ): SignedGaslessData {
        val eip712Data = Eip712TypedDataBuilder.build(
            gaslessTransaction = context.gaslessTransactionData,
            chainId = context.chainId,
            verifyingContract = transactionData.sourceAddress,
        )

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
     */
    private suspend fun signAndSendTransactionOnBackend(
        context: GaslessContext,
        signedData: SignedGaslessData,
        transactionData: TransactionData.Uncompiled,
    ): String {
        val txHash = gaslessTransactionRepository.signGaslessTransaction(
            network = context.tokenForFeeStatus.currency.network,
            gaslessTransactionData = context.gaslessTransactionData,
            signature = signedData.eip712Signature,
            userAddress = transactionData.sourceAddress,
            eip7702Auth = signedData.eip7702Auth,
        ).txHash

        (context.walletManager as? PendingTransactionHandler)?.addPendingGaslessTransaction(
            transactionData = transactionData,
            txHash = txHash,
            contractAddress = transactionData.contractAddress,
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
                )
            }
            is UserWallet.Hot -> getHotWalletSigner(userWallet)
        }
    }

    private suspend fun createGaslessTransactionData(
        transactionData: TransactionData.Uncompiled,
        txFee: TransactionFeeExtended,
        tokenFeeStatus: CryptoCurrencyStatus,
        nonce: BigInteger,
    ): GaslessTransactionData {
        val transaction = buildTransaction(transactionData)
        val fee = buildFee(txFee, tokenFeeStatus)

        return GaslessTransactionData(
            transaction = transaction,
            fee = fee,
            nonce = nonce,
        )
    }

    private fun buildTransaction(transactionData: TransactionData.Uncompiled): GaslessTransactionData.Transaction {
        val callData = (transactionData.extras as? EthereumTransactionExtras)?.callData
            ?: error("Ethereum call data is required")

        // Native amount is always zero in gasless transactions for now
        // we don't support gasless transfers of native currency
        val nativeAmount = BigInteger.ZERO

        return GaslessTransactionData.Transaction(
            to = getDestinationAddress(transactionData),
            value = nativeAmount,
            data = callData.data,
        )
    }

    private suspend fun buildFee(
        txFee: TransactionFeeExtended,
        tokenFeeStatus: CryptoCurrencyStatus,
    ): GaslessTransactionData.Fee {
        val tokenForFee = tokenFeeStatus.currency as? CryptoCurrency.Token
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
        return when (val dataResult = gaslessDataProvider.prepareEIP7702AuthorizationData()) {
            is Result.Failure -> throw dataResult.error
            is Result.Success -> dataResult.data
        }
    }

    private fun getDestinationAddress(txData: TransactionData.Uncompiled): String {
        val ethereumCallData = (txData.extras as? EthereumTransactionExtras)?.callData
        val contractAddress = txData.contractAddress
        return if (ethereumCallData is EthereumYieldSupplySendCallData) {
            ethereumCallData.destinationAddress
        } else {
            contractAddress ?: error("supports only Token transaction with contract address")
        }
    }

    /**
     * Context containing all prepared data for gasless transaction.
     */
    private data class GaslessContext(
        val walletManager: WalletManager,
        val gaslessDataProvider: EthereumGaslessDataProvider,
        val tokenForFeeStatus: CryptoCurrencyStatus,
        val gaslessTransactionData: GaslessTransactionData,
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

    private companion object {
        fun BigInteger.toFormattedHex(bytes: Int): String {
            return toByteArray().normalizeByteArray(bytes).toHexString().formatHex()
        }
    }
}