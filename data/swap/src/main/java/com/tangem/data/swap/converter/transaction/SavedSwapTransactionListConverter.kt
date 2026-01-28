package com.tangem.data.swap.converter.transaction

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.swap.models.SwapStatusDTO
import com.tangem.data.swap.models.SwapTransactionDTO
import com.tangem.data.swap.models.SwapTransactionListDTO
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapTransactionListModel
import com.tangem.lib.crypto.derivation.AccountNodeRecognizer
import com.tangem.utils.converter.Converter

internal class SavedSwapTransactionListConverter(
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    private val networkFactory: NetworkFactory,
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
            currency = value.fromCryptoCurrency,
            accountId = value.fromAccount?.accountId,
        ),
        toTokensResponse = userTokensResponseFactory.createResponseToken(
            currency = value.toCryptoCurrency,
            accountId = value.toAccount?.accountId,
        ),
        transactions = savedSwapTransactionConverter.convertList(value.transactions),
    )

    fun convertBack(
        value: SwapTransactionListDTO,
        userWallet: UserWallet,
        txStatuses: Map<String, SwapStatusDTO>,
        multiAccountList: List<AccountList>,
    ): SwapTransactionListModel? {
        val fromToken = value.fromTokensResponse
        val toToken = value.toTokensResponse
        val fromDerivationIndex = fromToken?.getDerivationIndex()
        val toDerivationIndex = toToken?.getDerivationIndex()
        return if (fromToken == null || toToken == null) {
            null
        } else {
            val fromNetwork = createSwapTransactionNetwork(fromToken, userWallet) ?: return null
            val fromCryptoCurrency = responseCryptoCurrenciesFactory.createCurrency(
                responseToken = fromToken,
                userWallet = userWallet,
                network = fromNetwork,
            ) ?: return null
            val toNetwork = createSwapTransactionNetwork(fromToken, userWallet) ?: return null
            val toCryptoCurrency = responseCryptoCurrenciesFactory.createCurrency(
                responseToken = toToken,
                userWallet = userWallet,
                network = toNetwork,
            ) ?: return null

            return SwapTransactionListModel(
                transactions = value.transactions.map { tx ->
                    savedSwapTransactionConverter.convertBack(
                        value = tx,
                        userWallet = userWallet,
                        txStatuses = txStatuses,
                    )
                },
                userWalletId = value.userWalletId,
                fromCryptoCurrencyId = value.fromCryptoCurrencyId,
                toCryptoCurrencyId = value.toCryptoCurrencyId,
                fromCryptoCurrency = fromCryptoCurrency,
                toCryptoCurrency = toCryptoCurrency,
                fromAccount = getAccountByDerivationIndex(
                    multiAccountList = multiAccountList,
                    derivationIndex = fromDerivationIndex,
                ),
                toAccount = getAccountByDerivationIndex(
                    multiAccountList = multiAccountList,
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
        fromAccount: Account.CryptoPortfolio?,
        toAccount: Account.CryptoPortfolio?,
        tokenTransactions: List<SwapTransactionDTO>,
    ) = SwapTransactionListDTO(
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
                accountIndex = token.getDerivationIndex(),
            )
        }
    }

    private fun getAccountByDerivationIndex(
        multiAccountList: List<AccountList>,
        derivationIndex: DerivationIndex?,
    ): Account.CryptoPortfolio? {
        if (derivationIndex == null) return null

        return multiAccountList.asSequence().firstNotNullOfOrNull { accountList ->
            accountList.accounts.asSequence().filterIsInstance<Account.CryptoPortfolio>()
                .firstOrNull { it.derivationIndex == derivationIndex }
        }
    }

    private fun UserTokensResponse.Token.getDerivationIndex(): DerivationIndex? {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: return null
        val accountNodeRecognizer = AccountNodeRecognizer(blockchain)
        return derivationPath
            ?.let { accountNodeRecognizer.recognize(it) }
            ?.let { DerivationIndex(it.toInt()).getOrNull() }
    }
}