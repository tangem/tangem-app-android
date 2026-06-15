package com.tangem.domain.transaction.usecase

import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.domain.demo.DemoTransactionSender
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.mapToFeeError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.isNullOrZero
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Use case to get transaction fee
 *
 * !!!IMPORTANT!!!
 * Use when transaction data is already compiled by external service or provider
 */
class GetFeeUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
) {
    suspend operator fun invoke(
        userWallet: UserWallet,
        network: Network,
        transactionData: TransactionData,
        spenderAddress: String? = null,
        isSimulateEstimation: Boolean = false,
    ) = either {
        catch(
            block = {
                val transactionSender = if (userWallet is UserWallet.Cold &&
                    demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)
                ) {
                    demoTransactionSender(userWallet, network)
                } else {
                    walletManagersFacade.getOrCreateWalletManager(
                        userWalletId = userWallet.walletId,
                        network = network,
                    )
                }
                val isEthereumWalletManager = transactionSender is EthereumWalletManager
                val result = if (isSimulateEstimation && spenderAddress != null && isEthereumWalletManager) {
                    transactionSender.estimateFeeWithOverride(
                        transactionData = transactionData,
                        spenderAddress = spenderAddress,
                        isSimulate = true,
                    )
                } else {
                    transactionSender?.getFee(transactionData = transactionData)
                        ?: error("Fee is null")
                }

                val maybeFee = when (result) {
                    is Result.Success -> result.data
                    is Result.Failure -> raise(result.mapToFeeError())
                }
                maybeFee.fixYieldSupplyGasLimit(transactionData = transactionData)
            },
            catch = {
                raise(GetFeeError.DataError(it))
            },
        )
    }

    suspend operator fun invoke(
        amount: BigDecimal,
        destination: String,
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ) = either {
        catch(
            block = {
                val amountData = convertCryptoCurrencyToAmount(cryptoCurrency, amount)

                val result = if (userWallet is UserWallet.Cold &&
                    demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)
                ) {
                    demoTransactionSender(userWallet, cryptoCurrency.network).getFee(
                        amount = amountData,
                        destination = destination,
                    )
                } else {
                    walletManagersFacade.getFee(
                        amount = amountData,
                        destination = destination,
                        userWalletId = userWallet.walletId,
                        network = cryptoCurrency.network,
                    ) ?: error("Fee is null")
                }

                val maybeFee = when (result) {
                    is Result.Success -> result.data
                    is Result.Failure -> raise(result.mapToFeeError())
                }
                maybeFee
            },
            catch = {
                raise(GetFeeError.DataError(it))
            },
        )
    }

    private suspend fun demoTransactionSender(userWallet: UserWallet, network: Network): DemoTransactionSender {
        return DemoTransactionSender(
            walletManagersFacade.getOrCreateWalletManager(userWallet.walletId, network)
                ?: error("WalletManager is null"),
        )
    }

    private fun convertCryptoCurrencyToAmount(cryptoCurrency: CryptoCurrency, amount: BigDecimal) = Amount(
        currencySymbol = cryptoCurrency.symbol,
        value = amount,
        decimals = cryptoCurrency.decimals,
        type = when (cryptoCurrency) {
            is CryptoCurrency.Coin -> AmountType.Coin
            is CryptoCurrency.Token -> AmountType.Token(
                token = Token(
                    symbol = cryptoCurrency.symbol,
                    contractAddress = cryptoCurrency.contractAddress,
                    decimals = cryptoCurrency.decimals,
                ),
            )
        },
    )

    private fun TransactionFee.fixYieldSupplyGasLimit(transactionData: TransactionData): TransactionFee {
        val uncompiledTransactionData = transactionData as? TransactionData.Uncompiled ?: return this
        val ethereumExtras = uncompiledTransactionData.extras as? EthereumTransactionExtras ?: return this

        return if (ethereumExtras.callData is EthereumYieldSupplySendCallData) {
            val patchedFee = when (this) {
                is TransactionFee.Choosable -> {
                    copy(
                        normal = normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
                        minimum = minimum.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
                        priority = priority.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
                    )
                }
                is TransactionFee.Single -> copy(
                    normal = normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
                )
            }
            TangemLogger.withTag("GAS_FEE_USECASE").i("Fee for Yield Mode adjusted: $patchedFee")
            patchedFee
        } else {
            TangemLogger.withTag("GAS_FEE_USECASE").i("Fee as is: $this")
            this
        }
    }

    /**
     * Increase gasLimit for Fee.Ethereum
     */
    private fun Fee.increaseGasLimitBy(percent: BigInteger): Fee {
        if (this !is Fee.Ethereum) return this
        val gasLimit = gasLimit

        if (gasLimit == BigInteger.ZERO || amount.value.isNullOrZero()) return this

        val increasedGasPrice = amount.value?.movePointRight(amount.decimals)
            ?.divide(gasLimit.toBigDecimal(), RoundingMode.HALF_UP)
        val increasedGasLimit = gasLimit
            .multiply(percent)
            .divide(HUNDRED_PERCENT)
        val increasedAmount = this.amount.copy(
            value = increasedGasLimit.toBigDecimal().multiply(increasedGasPrice).movePointLeft(amount.decimals),
        )
        return when (this) {
            is Fee.Ethereum.TokenCurrency -> error("handle in [REDACTED_TASK_KEY]")
            is Fee.Ethereum.EIP1559 -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.Legacy -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
        }
    }

    private companion object {
        private val HUNDRED_PERCENT = 100.toBigInteger() // base 100%
        val INCREASE_GAS_LIMIT_FOR_SUPPLY = 140.toBigInteger() // 40% increase [there is also in Yield FeeExtensions.kt]
    }
}