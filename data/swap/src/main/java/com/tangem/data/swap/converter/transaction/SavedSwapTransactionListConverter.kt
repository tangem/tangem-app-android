package com.tangem.data.swap.converter.transaction

import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.swap.models.SwapStatusDTO
import com.tangem.data.swap.models.SwapTransactionDTO
import com.tangem.data.swap.models.SwapTransactionListDTO
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapTransactionListModel
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.converter.Converter

internal class SavedSwapTransactionListConverter(
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
) : Converter<SwapTransactionListModel, SwapTransactionListDTO> {

    private val userTokensResponseFactory = UserTokensResponseFactory()
    private val savedSwapTransactionConverter by lazy(LazyThreadSafetyMode.NONE) {
        SavedSwapTransactionConverter(responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory)
    }

    override fun convert(value: SwapTransactionListModel) = SwapTransactionListDTO(
        userWalletId = value.userWalletId,
        fromCryptoCurrencyId = value.fromCryptoCurrencyId,
        toCryptoCurrencyId = value.toCryptoCurrencyId,
        fromTokensResponse = userTokensResponseFactory.createResponseToken(
            value.fromCryptoCurrency,
        ),
        toTokensResponse = userTokensResponseFactory.createResponseToken(
            value.toCryptoCurrency,
        ),
        transactions = savedSwapTransactionConverter.convertList(value.transactions),
    )

    fun convertBack(
        value: SwapTransactionListDTO,
        userWallet: UserWallet,
        txStatuses: Map<String, SwapStatusDTO>,
    ): SwapTransactionListModel? {
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

            return SwapTransactionListModel(
                transactions = value.transactions.map { tx ->
                    savedSwapTransactionConverter.convertBack(tx, userWallet, txStatuses)
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
        tokenTransactions: List<SwapTransactionDTO>,
    ) = SwapTransactionListDTO(
        userWalletId = userWalletId.stringValue,
        fromCryptoCurrencyId = fromCryptoCurrency.id.value,
        toCryptoCurrencyId = toCryptoCurrency.id.value,
        fromTokensResponse = userTokensResponseFactory.createResponseToken(fromCryptoCurrency),
        toTokensResponse = userTokensResponseFactory.createResponseToken(toCryptoCurrency),
        transactions = tokenTransactions,
    )
}