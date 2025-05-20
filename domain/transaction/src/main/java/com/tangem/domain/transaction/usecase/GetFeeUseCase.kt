package com.tangem.domain.transaction.usecase

import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.demo.DemoTransactionSender
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.mapToFeeError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import java.math.BigDecimal

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
    suspend operator fun invoke(userWallet: UserWallet, network: Network, transactionData: TransactionData) = either {
        catch(
            block = {
                val transactionSender = if (demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)) {
                    demoTransactionSender(userWallet, network)
                } else {
                    walletManagersFacade.getOrCreateWalletManager(
                        userWalletId = userWallet.walletId,
                        network = network,
                    )
                }
                val result = transactionSender?.getFee(transactionData = transactionData)
                    ?: error("Fee is null")

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

    suspend operator fun invoke(
        amount: BigDecimal,
        destination: String,
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ) = either {
        catch(
            block = {
                val amountData = convertCryptoCurrencyToAmount(cryptoCurrency, amount)

                val result = if (demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)) {
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
}