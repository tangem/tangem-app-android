package com.tangem.domain.transaction.usecase

import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.SmartContractCallDataProviderFactory
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.demo.DemoTransactionSender
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.mapToFeeError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import java.math.BigDecimal

/**
 * Use case to get transfer transaction fee
 *
 * !!!IMPORTANT
 * Use when transaction data is compiled by us using BlockchainSDK methods
 */
class GetTransferFeeUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
) {
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
                    demoTransactionSender(userWallet, cryptoCurrency).getFee(
                        amount = amountData,
                        destination = destination,
                    )
                } else {
                    val walletManager = walletManagersFacade.getOrCreateWalletManager(
                        userWalletId = userWallet.walletId,
                        network = cryptoCurrency.network,
                    )
                    val smartContractCallData = if (amountData.type is AmountType.Token) {
                        SmartContractCallDataProviderFactory.getTokenTransferCallData(
                            amount = amountData,
                            destinationAddress = destination,
                            blockchain = Blockchain.fromId(cryptoCurrency.network.id.value),
                        )
                    } else {
                        null
                    }

                    (walletManager as? TransactionSender)?.getFee(
                        amount = amountData,
                        destination = destination,
                        callData = smartContractCallData,
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

    private suspend fun demoTransactionSender(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ): DemoTransactionSender {
        return DemoTransactionSender(
            walletManagersFacade
                .getOrCreateWalletManager(userWallet.walletId, cryptoCurrency.network)
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