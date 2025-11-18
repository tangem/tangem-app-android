package com.tangem.feature.swap.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModelInner
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import com.tangem.utils.converter.Converter

internal class SavedSwapTransactionListConverter(
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    private val networkFactory: NetworkFactory,
) : Converter<SavedSwapTransactionListModel, SavedSwapTransactionListModelInner> {

    private val userTokensResponseFactory = UserTokensResponseFactory()

    override fun convert(value: SavedSwapTransactionListModel) = SavedSwapTransactionListModelInner(
        userWalletId = value.userWalletId,
        fromCryptoCurrencyId = value.fromCryptoCurrencyId,
        toCryptoCurrencyId = value.toCryptoCurrencyId,
        fromTokensResponse = userTokensResponseFactory.createResponseToken(
            currency = value.fromCryptoCurrency,
            accountId = null,
        ),
        toTokensResponse = userTokensResponseFactory.createResponseToken(
            currency = value.toCryptoCurrency,
            accountId = null,
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
            val fromNetwork = createSwapTransactionNetwork(fromToken, userWallet) ?: return null
            val fromCryptoCurrency = responseCryptoCurrenciesFactory.createCurrency(
                responseToken = fromToken,
                userWallet = userWallet,
                network = fromNetwork,
            ) ?: return null
            val toNetwork = createSwapTransactionNetwork(toToken, userWallet) ?: return null
            val toCryptoCurrency = responseCryptoCurrenciesFactory.createCurrency(
                responseToken = toToken,
                userWallet = userWallet,
                network = toNetwork,
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
        fromTokensResponse = userTokensResponseFactory.createResponseToken(
            currency = fromCryptoCurrency,
            accountId = null,
        ),
        toTokensResponse = userTokensResponseFactory.createResponseToken(currency = toCryptoCurrency, accountId = null),
        transactions = tokenTransactions,
    )

    private fun createSwapTransactionNetwork(token: UserTokensResponse.Token, userWallet: UserWallet): Network? {
        val blockchain = Blockchain.fromNetworkId(token.networkId) ?: return null

        return if (token.derivationPath == null) {
            networkFactory.create(
                blockchain = blockchain,
                derivationPath = Network.DerivationPath.None,
                userWallet = userWallet,
            )
        } else {
            networkFactory.create(
                blockchain = blockchain,
                extraDerivationPath = token.derivationPath,
                userWallet = userWallet,
            )
        }
    }
}