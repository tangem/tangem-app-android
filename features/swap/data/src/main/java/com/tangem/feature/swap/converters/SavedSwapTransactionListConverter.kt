package com.tangem.feature.swap.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModelInner
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import com.tangem.lib.crypto.derivation.AccountNodeRecognizer
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
            accountId = value.fromAccount?.accountId,
        ),
        toTokensResponse = userTokensResponseFactory.createResponseToken(
            currency = value.toCryptoCurrency,
            accountId = value.toAccount?.accountId,
        ),
        transactions = value.transactions,
    )

    fun convertBack(
        value: SavedSwapTransactionListModelInner,
        accountList: AccountList?,
        userWallet: UserWallet,
        txStatuses: Map<String, ExchangeStatusModel>,
        onFilter: (SavedSwapTransactionModel) -> Boolean = { true },
    ): SavedSwapTransactionListModel? {
        val fromToken = value.fromTokensResponse
        val toToken = value.toTokensResponse
        val fromDerivationIndex = fromToken?.getDerivationIndex()
        val toDerivationIndex = toToken?.getDerivationIndex()

        return if (fromToken == null || toToken == null) {
            null
        } else {
            val fromNetwork = createSwapTransactionNetwork(fromToken, userWallet, fromDerivationIndex) ?: return null
            val fromCryptoCurrency = responseCryptoCurrenciesFactory.createCurrency(
                responseToken = fromToken,
                userWallet = userWallet,
                network = fromNetwork,
            ) ?: return null
            val toNetwork = createSwapTransactionNetwork(toToken, userWallet, toDerivationIndex) ?: return null
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
                            val blockchain = Blockchain.fromNetworkId(id.networkId) ?: return@let null
                            val derivationPath = id.derivationPath ?: return@let null

                            val accountIndex = if (blockchain == Blockchain.Chia) {
                                DerivationIndex.Main
                            } else {
                                val recognizer = AccountNodeRecognizer(blockchain = blockchain)
                                val index = recognizer.recognize(derivationPathValue = derivationPath)?.toInt()
                                    ?: return@let null

                                DerivationIndex(index).getOrNull() ?: return@let null
                            }

                            responseCryptoCurrenciesFactory.createCurrency(
                                responseToken = id,
                                userWallet = userWallet,
                                accountIndex = accountIndex,
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
                fromAccount = findAccountByDerivationIndex(
                    accountList = accountList,
                    derivationIndex = fromDerivationIndex,
                ),
                toAccount = findAccountByDerivationIndex(
                    accountList = accountList,
                    derivationIndex = toDerivationIndex,
                ),
            )
        }
    }

    @Suppress("LongParameterList")
    fun default(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        fromAccount: Account?,
        toAccount: Account?,
        tokenTransactions: List<SavedSwapTransactionModel>,
    ) = SavedSwapTransactionListModelInner(
        userWalletId = userWalletId.stringValue,
        fromCryptoCurrencyId = fromCryptoCurrency.id.value,
        toCryptoCurrencyId = toCryptoCurrency.id.value,
        fromTokensResponse = userTokensResponseFactory.createResponseToken(
            currency = fromCryptoCurrency,
            accountId = fromAccount?.accountId,
        ),
        toTokensResponse = userTokensResponseFactory.createResponseToken(
            currency = toCryptoCurrency,
            accountId = toAccount?.accountId,
        ),
        transactions = tokenTransactions,
    )

    private fun createSwapTransactionNetwork(
        token: UserTokensResponse.Token,
        userWallet: UserWallet,
        accountIndex: DerivationIndex?,
    ): Network? {
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
                accountIndex = accountIndex,
            )
        }
    }

    private fun findAccountByDerivationIndex(accountList: AccountList?, derivationIndex: DerivationIndex?): Account? {
        return accountList?.accounts?.asSequence()?.filterIsInstance<Account.Crypto.Portfolio>()
            ?.firstOrNull { it.derivationIndex == derivationIndex }
    }

    private fun UserTokensResponse.Token.getDerivationIndex(): DerivationIndex? {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: return null
        val accountNodeRecognizer = AccountNodeRecognizer(blockchain)
        return derivationPath
            ?.let { accountNodeRecognizer.recognize(it) }
            ?.let { DerivationIndex(it.toInt()).getOrNull() }
    }
}