package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.gasless.EthereumGaslessDataProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.GaslessTransactionData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigInteger

class CreateAndSendGaslessTransactionUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
) {

    @Suppress("UnusedPrivateProperty")
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        transactionData: TransactionData,
        fee: TransactionFeeExtended,
    ): Either<SendTransactionError, Unit> {
        return either {
            catch(
                block = {
                    transactionData as? TransactionData.Uncompiled ?: error("Uncompiled transaction data required")

                    val tokenForFeeStatus = getSingleCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
                        userWalletId,
                        fee.feeTokenId,
                    ).getOrNull() ?: error("Token for fee not found")

                    val blockchain = Blockchain.fromId(tokenForFeeStatus.currency.network.rawId)

                    val gaslessTransactionData = createGaslessTransactionData(
                        userWalletId = userWalletId,
                        transactionData = transactionData,
                        txFee = fee,
                        tokenFeeStatus = tokenForFeeStatus,
                    )

                    val chainId =
                        blockchain.getChainId() ?: error("ChainId not found for blockchain ${blockchain.name}")

                    val eip712Data = Eip712TypedDataBuilder.build(
                        gaslessTransaction = gaslessTransactionData,
                        chainId = chainId,
                        verifyingContract = transactionData.sourceAddress,
                    )

                    val eip712HashToSign = EthereumUtils.makeTypedDataHash(eip712Data)
                },
                catch = {
                    raise(SendTransactionError.DataError(it.message))
                },
            )
        }
    }

    private suspend fun createGaslessTransactionData(
        userWalletId: UserWalletId,
        transactionData: TransactionData.Uncompiled,
        txFee: TransactionFeeExtended,
        tokenFeeStatus: CryptoCurrencyStatus,
    ): GaslessTransactionData {
        val bigIntegerAmount =
            transactionData.amount.value?.movePointRight(transactionData.amount.decimals)?.toBigInteger()
                ?: error("Amount value is null")
        val txData = (transactionData.extras as? EthereumTransactionExtras)?.callData ?: error("Call data required")

        val transaction = GaslessTransactionData.Transaction(
            to = getDestinationAddress(transactionData),
            value = bigIntegerAmount,
            data = txData.data,
        )

        val tokenForFee = tokenFeeStatus.currency as? CryptoCurrency.Token
            ?: error("only CryptoCurrency.Token supported for fee")

        val txFeeInTokenCurrency = txFee.transactionFee.normal as? Fee.Ethereum.TokenCurrency ?: error(
            "only Fee.Ethereum.TokenCurrency supported for gasless fee",
        )
        val fee = GaslessTransactionData.Fee(
            feeToken = tokenForFee.contractAddress,
            maxTokenFee = txFeeInTokenCurrency.gasLimit,
            coinPriceInToken = txFeeInTokenCurrency.coinPriceInToken,
            feeTransferGasLimit = txFeeInTokenCurrency.feeTransferGasLimit,
            baseGas = txFeeInTokenCurrency.baseGas,
        )

        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, tokenForFee.network)
            ?: error("WalletManager not found for network ${tokenForFee.network.id}")
        val gaslessDataProvider = walletManager as? EthereumGaslessDataProvider ?: error(
            "WalletManager for network ${tokenForFee.network.id} does not support gasless transactions",
        )

        val nonceResult = gaslessDataProvider.getGaslessContractNonce(
            userAddress = transactionData.sourceAddress,
        )
        val nonce = when (nonceResult) {
            is com.tangem.blockchain.extensions.Result.Failure -> BigInteger.ZERO
            is com.tangem.blockchain.extensions.Result.Success -> nonceResult.data
        }

        return GaslessTransactionData(
            transaction = transaction,
            fee = fee,
            nonce = nonce,
        )
    }

    private fun getDestinationAddress(txData: TransactionData.Uncompiled): String {
        val ethereumCallData = (txData.extras as? EthereumTransactionExtras)?.callData
        return if (ethereumCallData is EthereumYieldSupplySendCallData) {
            ethereumCallData.destinationAddress
        } else {
            txData.destinationAddress
        }
    }
}