package com.tangem.feature.swap.converters

import com.tangem.data.tokens.utils.ResponseCryptoCurrenciesFactory
import com.tangem.data.tokens.utils.UserTokensResponseFactory
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModelInner
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import com.tangem.utils.converter.Converter

class SavedSwapTransactionListConverter :
    Converter<SavedSwapTransactionListModel, SavedSwapTransactionListModelInner> {

    private val responseCryptoCurrenciesFactory = ResponseCryptoCurrenciesFactory()
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
        scanResponse: ScanResponse,
        txStatuses: Map<String, ExchangeStatusModel>,
    ): SavedSwapTransactionListModel? {
        val fromToken = value.fromTokensResponse
        val toToken = value.toTokensResponse
        return if (fromToken == null || toToken == null) {
            null
        } else {
            val fromCryptoCurrency = responseCryptoCurrenciesFactory.createCurrency(
                responseToken = fromToken,
                scanResponse = scanResponse,
            ) ?: return null
            val toCryptoCurrency = responseCryptoCurrenciesFactory.createCurrency(
                responseToken = toToken,
                scanResponse = scanResponse,
            ) ?: return null

            return SavedSwapTransactionListModel(
                transactions = value.transactions.map { tx ->
                    tx.copy(status = txStatuses[tx.txId])
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
