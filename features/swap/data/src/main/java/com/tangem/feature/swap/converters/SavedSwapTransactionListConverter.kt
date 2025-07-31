package com.tangem.feature.swap.converters

import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModelInner
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import com.tangem.utils.converter.Converter

internal class SavedSwapTransactionListConverter(
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
) : Converter<SavedSwapTransactionListModel, SavedSwapTransactionListModelInner> {

    private val userTokensResponseFactory = UserTokensResponseFactory()

    override fun convert(value: SavedSwapTransactionListModel) = SavedSwapTransactionListModelInner(
        userWalletId = value.userWalletId,
        fromCryptoCurrencyId = value.fromCryptoCurrencyId,
        toCryptoCurrencyId = value.toCryptoCurrencyId,
        fromTokensResponse = userTokensResponseFactory.createResponseToken(
            value.fromCryptoCurrency,
        ),
        toTokensResponse = userTokensResponseFactory.createResponseToken(
            value.toCryptoCurrency,
        ),
        transactions = value.transactions,
    )

    fun convertBack(
        value: SavedSwapTransactionListModelInner,
        userWallet: UserWallet,
        txStatuses: Map<String, ExchangeStatusModel>,
        onFilter: (SavedSwapTransactionModel) -> Boolean = { true },
    ): SavedSwapTransactionListModel? {
        val fromToken = value.fromTokensResponse
        val toToken = value.toTokensResponse
        return if (fromToken == null || toToken == null) {
            null
        } else {
            val fromCryptoCurrency = responseCryptoCurrenciesFactory.createCurrency(
                responseToken = fromToken,
                userWallet = userWallet,
            ) ?: return null
            val toCryptoCurrency = responseCryptoCurrenciesFactory.createCurrency(
                responseToken = toToken,
                userWallet = userWallet,
            ) ?: return null

            return SavedSwapTransactionListModel(
                transactions = value.transactions
                    .filter(onFilter)
                    .map { tx ->
                        val status = txStatuses[tx.txId]
                        val refundCurrency = status?.refundTokensResponse?.let { id ->
                            responseCryptoCurrenciesFactory.createCurrency(
                                responseToken = id,
                                userWallet = userWallet,
                            )
                        }
                        val statusWithRefundCurrency = status?.copy(refundCurrency = refundCurrency)
                        tx.copy(status = statusWithRefundCurrency)
                    },
                userWalletId = value.userWalletId,
                fromCryptoCurrencyId = value.fromCryptoCurrencyId,
                toCryptoCurrencyId = value.toCryptoCurrencyId,
                fromCryptoCurrency = fromCryptoCurrency,
                toCryptoCurrency = toCryptoCurrency,
            )
        }
    }

    fun default(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        tokenTransactions: List<SavedSwapTransactionModel>,
    ) = SavedSwapTransactionListModelInner(
        userWalletId = userWalletId.stringValue,
        fromCryptoCurrencyId = fromCryptoCurrency.id.value,
        toCryptoCurrencyId = toCryptoCurrency.id.value,
        fromTokensResponse = userTokensResponseFactory.createResponseToken(fromCryptoCurrency),
        toTokensResponse = userTokensResponseFactory.createResponseToken(toCryptoCurrency),
        transactions = tokenTransactions,
    )
}