package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.gasless.EthereumGaslessDataProvider
import com.tangem.blockchain.blockchains.ethereum.models.EIP7702AuthorizationData
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.extensions.normalizeByteArray
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyDeployCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyReactivateTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySwapCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyWithdrawCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.MerklClaimCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.unwrapYieldModuleUpgrade
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.isBurnAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.mapToGaslessSendError
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.domain.transaction.models.GaslessApproval
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.models.GaslessTransactionData
import com.tangem.domain.transaction.models.GaslessYieldWithdraw
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigInteger

class CreateAndSendEvmGaslessTransactionUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider,
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val gaslessSignerProvider: GaslessSignerProvider,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        transactionData: TransactionData,
        fee: TransactionFeeExtended,
    ): Either<SendTransactionError, String> = invoke(userWallet, listOf(transactionData), fee)

    /**
     * Sends [transactionDataList] as the user's calls of one gasless meta-transaction, executed in list order.
     * [TransactionFeeExtended.callGasLimits] must hold one gas limit per call.
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        transactionDataList: List<TransactionData>,
        fee: TransactionFeeExtended,
    ): Either<SendTransactionError, String> = either {
        catch(
            block = {
                val uncompiledTxDataList = validateTransactionData(transactionDataList)
                val context = prepareGaslessContext(userWallet, uncompiledTxDataList, fee)
                val signedData = signGaslessTransactionByUser(userWallet, context)
                signAndSendTransactionOnBackend(context, signedData).also {
                    if (uncompiledTxDataList.upgradesYieldModule() || fee.gaslessFeePlan.upgradesYieldModule()) {
                        walletManagersFacade.resetYieldSendUpgradeDecision(
                            userWalletId = userWallet.walletId,
                            network = context.currency.network,
                        )
                    }
                }
            },
            catch = {
                raise(it.mapToGaslessSendError())
            },
        )
    }

    private fun validateTransactionData(transactionDataList: List<TransactionData>): List<TransactionData.Uncompiled> {
        require(transactionDataList.isNotEmpty()) { "At least one transaction is required" }
        val uncompiled = transactionDataList.map {
            it as? TransactionData.Uncompiled ?: error("Uncompiled transaction data required")
        }
        require(uncompiled.map(TransactionData.Uncompiled::sourceAddress).distinct().size == 1) {
            "All gasless calls must be sent from the same address"
        }
        return uncompiled
    }

    /**
     * Prepares all necessary context for gasless transaction.
     * Includes: wallet manager, gasless provider, token status, nonce, transaction data.
     *
     * The payload shape is decided by [assembleGaslessPayload]. [GaslessFeePlan.NativePay] must never reach
     * this use case — it is guarded there.
     */
    private suspend fun prepareGaslessContext(
        userWallet: UserWallet,
        transactionDataList: List<TransactionData.Uncompiled>,
        fee: TransactionFeeExtended,
    ): GaslessContext {
        val currency = singleAccountListSupplier.getSyncOrNull(userWalletId = userWallet.walletId)
            .getCryptoCurrency(currencyId = fee.feeTokenId, network = null)
            .getOrNull()
            ?: nonPortfolioCurrenciesProvider.findCurrency(userWallet.walletId, fee.feeTokenId)
            ?: error("Token for fee not found")

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWallet.walletId,
            currency.network,
        ) ?: error("WalletManager not found for network ${currency.network.id}")

        val gaslessDataProvider = walletManager as? EthereumGaslessDataProvider ?: error(
            "WalletManager for network ${currency.network.id} " +
                "does not support gasless transactions",
        )

        transactionDataList.forEach { transactionData ->
            validateTransactionRecipients(
                blockchain = walletManager.wallet.blockchain,
                transactionData = transactionData,
            )
        }

        val sourceAddress = transactionDataList.first().sourceAddress
        val gaslessContractNonce = getContractNonce(gaslessDataProvider, sourceAddress)

        val callGasLimits = requireNotNull(fee.callGasLimits) {
            "Call gas limits are required for a gasless (token-fee) transaction"
        }
        require(callGasLimits.size == transactionDataList.size) {
            "Expected ${transactionDataList.size} call gas limits, got ${callGasLimits.size}"
        }
        val calls = transactionDataList.zip(callGasLimits, ::buildTransaction)
        val feeObj = buildFee(fee, currency)

        val payload = assembleGaslessPayload(
            calls = calls,
            feeObj = feeObj,
            nonce = gaslessContractNonce,
            plan = unwrapFeePlanIfNeeded(plan = fee.gaslessFeePlan, transactionDataList = transactionDataList),
            withdrawGasLimit = fee.withdrawGasLimit,
            approveGasLimit = fee.approveGasLimit,
        )

        val chainId = gaslessTransactionRepository.getChainIdForNetwork(currency.network)

        return GaslessContext(
            walletManager = walletManager,
            gaslessDataProvider = gaslessDataProvider,
            currency = currency,
            payload = payload,
            chainId = chainId,
            sourceAddress = sourceAddress,
            lastTransactionData = transactionDataList.last(),
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
    ): SignedGaslessData {
        val eip712Data = when (val payload = context.payload) {
            is GaslessPayload.Single -> Eip712TypedDataBuilder.build(
                gaslessTransaction = payload.data,
                chainId = context.chainId,
                verifyingContract = context.sourceAddress,
                includeGasLimit = true,
            )
            is GaslessPayload.Batch -> Eip712TypedDataBuilder.buildBatch(
                gaslessBatch = payload.data,
                chainId = context.chainId,
                verifyingContract = context.sourceAddress,
                includeGasLimit = true,
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
     * Pending-transaction tracking is keyed on the last user call — the one carrying the user's intent
     * (send, swap, yield enter/exit); preceding calls only prepare it.
     */
    private suspend fun signAndSendTransactionOnBackend(
        context: GaslessContext,
        signedData: SignedGaslessData,
    ): String {
        val txHash = when (val payload = context.payload) {
            is GaslessPayload.Single -> gaslessTransactionRepository.signGaslessTransaction(
                network = context.currency.network,
                gaslessTransactionData = payload.data,
                signature = signedData.eip712Signature,
                userAddress = context.sourceAddress,
                eip7702Auth = signedData.eip7702Auth,
            ).txHash
            is GaslessPayload.Batch -> gaslessTransactionRepository.signGaslessBatchTransaction(
                network = context.currency.network,
                gaslessBatchTransactionData = payload.data,
                signature = signedData.eip712Signature,
                userAddress = context.sourceAddress,
                eip7702Auth = signedData.eip7702Auth,
            ).txHash
        }

        (context.walletManager as? PendingTransactionHandler)?.addPendingGaslessTransaction(
            transactionData = context.lastTransactionData,
            txHash = txHash,
        )

        return txHash
    }

    private suspend fun signHashes(
        userWallet: UserWallet,
        walletManager: WalletManager,
        hashesToOperate: HashesToOperate,
    ): HashesToOperate {
        val signer = gaslessSignerProvider(userWallet)
        val hashesToSign = listOf(hashesToOperate.eip712Hash, hashesToOperate.eip7702Hash)

        return when (val signerResult = signer.sign(hashesToSign, walletManager.wallet.publicKey)) {
            is CompletionResult.Failure -> {
                val signerError = signerResult.error
                if (signerError is TangemSdkError.UserCancelled) throw signerError
                error("Signing failed: ${signerError.message ?: "sdk error"}")
            }
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
        return when (val dataResult = gaslessDataProvider.prepareEIP7702AuthorizationData(isV2 = true)) {
            is Result.Failure -> throw dataResult.error
            is Result.Success -> dataResult.data
        }
    }

    /**
     * Discriminated union of the gasless transaction payload to sign and send.
     *
     * [Single] carries exactly one user call. [Batch] carries several user calls in execution order,
     * wrapped by the fee-plan sub-calls of [GaslessFeePlan.TokenPay].
     */
    internal sealed interface GaslessPayload {
        data class Single(val data: GaslessTransactionData) : GaslessPayload

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
        val sourceAddress: String,
        val lastTransactionData: TransactionData.Uncompiled,
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
         * The batch is ordered `[approve, calls, withdraw]`: the approve of a [GaslessFeePlan.TokenPay] runs first, as
         * the user's [calls] spend the allowance it grants, and its yield withdraw runs last, topping up the fee token
         * before the gasless service settles the fee. A single call without either sub-call is a
         * [GaslessPayload.Single]. [GaslessFeePlan.NativePay] never reaches this use case — the standard send path
         * handles it.
         */
        @Suppress("LongParameterList")
        internal fun assembleGaslessPayload(
            calls: List<GaslessTransactionData.Transaction>,
            feeObj: GaslessTransactionData.Fee,
            nonce: BigInteger,
            plan: GaslessFeePlan?,
            withdrawGasLimit: BigInteger?,
            approveGasLimit: BigInteger?,
        ): GaslessPayload {
            require(calls.isNotEmpty()) { "At least one call is required for a gasless transaction" }
            val tokenPay = when (plan) {
                is GaslessFeePlan.TokenPay -> plan
                is GaslessFeePlan.NativePay -> error("NativePay must not reach the gasless send path")
                null -> null
            }
            val transactions = listOfNotNull(tokenPay?.approval?.toApproveTransaction(approveGasLimit)) +
                calls +
                listOfNotNull(tokenPay?.yieldWithdraw?.toWithdrawTransaction(withdrawGasLimit))

            return if (transactions.size == 1) {
                GaslessPayload.Single(
                    GaslessTransactionData(transaction = transactions.single(), fee = feeObj, nonce = nonce),
                )
            } else {
                GaslessPayload.Batch(
                    GaslessBatchTransactionData(transactions = transactions, fee = feeObj, nonce = nonce),
                )
            }
        }

        private fun GaslessApproval.toApproveTransaction(gasLimit: BigInteger?) = GaslessTransactionData.Transaction(
            to = approvalTransaction.destinationAddress,
            value = BigInteger.ZERO,
            gasLimit = gasLimit ?: error("Approve gas limit is required for an approve batch"),
            data = (approvalTransaction.extras as? EthereumTransactionExtras)?.callData?.data
                ?: error("Approve call data is required"),
        )

        private fun GaslessYieldWithdraw.toWithdrawTransaction(gasLimit: BigInteger?) =
            GaslessTransactionData.Transaction(
                to = moduleAddress,
                value = BigInteger.ZERO,
                gasLimit = gasLimit ?: error("Withdraw gas limit is required for a yield-withdraw batch"),
                data = callData.data,
            )

        /**
         * The yield module is one per wallet: when a user call already upgrades it, the fee withdraw, which runs after
         * the user calls, drops its own upgrade instead of upgrading the module twice in one batch.
         */
        internal fun unwrapFeePlanIfNeeded(
            plan: GaslessFeePlan?,
            transactionDataList: List<TransactionData.Uncompiled>,
        ): GaslessFeePlan? {
            val tokenPay = plan as? GaslessFeePlan.TokenPay ?: return plan
            val withdraw = tokenPay.yieldWithdraw ?: return plan
            if (!transactionDataList.upgradesYieldModule()) return plan

            return tokenPay.copy(
                yieldWithdraw = withdraw.copy(callData = withdraw.callData.unwrapYieldModuleUpgrade()),
            )
        }

        internal fun GaslessFeePlan?.upgradesYieldModule(): Boolean {
            val withdrawCallData = (this as? GaslessFeePlan.TokenPay)?.yieldWithdraw?.callData
            return withdrawCallData is EthereumYieldSupplyUpgradeToAndCallCallData
        }

        private fun List<TransactionData.Uncompiled>.upgradesYieldModule(): Boolean = any { transactionData ->
            val callData = (transactionData.extras as? EthereumTransactionExtras)?.callData
            callData is EthereumYieldSupplyUpgradeToAndCallCallData
        }

        fun BigInteger.toFormattedHex(bytes: Int): String {
            return toByteArray().normalizeByteArray(bytes).toHexString().formatHex()
        }

        /**
         * Resolves the on-chain `to` of a user's gasless sub-call.
         *
         * - Yield-mode calls ([isCalledOnDestination]): the call runs against
         *   [TransactionData.Uncompiled.destinationAddress] — the user's yield module (send, swap, init, enter,
         *   exit, withdraw, Merkl claim, module upgrade), the yield factory (deploy) or the token contract
         *   (approve), or the DEX router for a compiled swap call ([CompiledSmartContractCallData]).
         *   For a yield-supply send, `send(token, dest, amount)` is a method ON the module and the
         *   recipient is encoded inside the call data; `destinationAddress` is patched to the module in
         *   `DefaultTransactionRepository.createTransaction`. Targeting the recipient instead reverts the whole
         *   batch with GAS_ESTIMATION_FAILED / require(false).
         * - Otherwise (e.g. ERC-20 transfer): `to` is the contract the calldata runs against
         *   ([TransactionData.Uncompiled.contractAddress], the token contract).
         */
        internal fun getDestinationAddress(txData: TransactionData.Uncompiled): String {
            val ethereumCallData = (txData.extras as? EthereumTransactionExtras)?.callData
            return if (ethereumCallData?.isCalledOnDestination() == true) {
                txData.destinationAddress
            } else {
                txData.contractAddress ?: error("supports only Token transaction with contract address")
            }
        }

        /**
         * An explicit allow-list: ordinary token transfers and other callers keep resolving `to` from
         * [TransactionData.Uncompiled.contractAddress].
         */
        private fun SmartContractCallData.isCalledOnDestination(): Boolean {
            if (this is EthereumYieldSupplyUpgradeToAndCallCallData) return true
            return when (unwrapYieldModuleUpgrade()) {
                is CompiledSmartContractCallData,
                is EthereumYieldSupplySendCallData,
                is EthereumYieldSupplySwapCallData,
                is EthereumYieldSupplyDeployCallData,
                is ApprovalERC20TokenCallData,
                is EthereumYieldSupplyInitTokenCallData,
                is EthereumYieldSupplyReactivateTokenCallData,
                is EthereumYieldSupplyEnterCallData,
                is EthereumYieldSupplyExitCallData,
                is EthereumYieldSupplyWithdrawCallData,
                is MerklClaimCallData,
                -> true
                else -> false
            }
        }

        /**
         * Last line of defence before the meta-transaction is signed: the gasless path never reaches
         * `TransactionSender.send`, so the `TransactionValidator` guarding the regular EVM send never runs for it.
         * The user signs the payload himself, so neither the gasless service nor the chain can reject it afterwards.
         */
        internal fun validateTransactionRecipients(
            blockchain: Blockchain,
            transactionData: TransactionData.Uncompiled,
        ) {
            val destination = transactionData.destinationAddress
            require(blockchain.validateAddress(destination) && !destination.isBurnAddress()) {
                "Invalid destination address for a gasless transaction"
            }

            val callData = (transactionData.extras as? EthereumTransactionExtras)?.callData
                ?: error("Ethereum call data is required")
            require(callData.validate(blockchain)) {
                "Invalid call data for a gasless transaction"
            }
            require(callData.recipientOrNull()?.isBurnAddress() != true) {
                "Burn address recipient in the call data of a gasless transaction"
            }
        }

        /**
         * The recipient encoded inside the call data, when it differs from the transaction's own destination.
         * Only the yield-supply send needs it: its `to` is the user's yield module.
         */
        private fun SmartContractCallData.recipientOrNull(): String? = when (val inner = unwrapYieldModuleUpgrade()) {
            is EthereumYieldSupplySendCallData -> inner.destinationAddress
            else -> null
        }
    }
}