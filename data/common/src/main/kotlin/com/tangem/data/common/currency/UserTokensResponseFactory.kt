package com.tangem.data.common.currency

import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.common.tokens.getDefaultWalletBlockchains
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import javax.inject.Inject

class UserTokensResponseFactory @Inject constructor() {

    fun createUserTokensResponse(
        currencies: List<CryptoCurrency>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
        accountId: AccountId? = null,
    ): UserTokensResponse {
        return UserTokensResponse(
            tokens = currencies.map { createResponseToken(currency = it, accountId = accountId) },
            group = if (isGroupedByNetwork) {
                UserTokensResponse.GroupType.NETWORK
            } else {
                UserTokensResponse.GroupType.NONE
            },
            sort = if (isSortedByBalance) {
                UserTokensResponse.SortType.BALANCE
            } else {
                UserTokensResponse.SortType.MANUAL
            },
        )
    }

    fun createResponseToken(currency: CryptoCurrency, accountId: AccountId? = null): UserTokensResponse.Token {
        return with(currency) {
            UserTokensResponse.Token(
                id = id.rawCurrencyId?.value,
                accountId = accountId?.value,
                networkId = network.backendId,
                derivationPath = network.derivationPath.value,
                name = name,
                symbol = symbol,
                decimals = decimals,
                contractAddress = (this as? CryptoCurrency.Token)?.contractAddress,
            )
        }
    }

    fun createDefaultResponse(
        userWallet: UserWallet?,
        networkFactory: NetworkFactory,
        accountId: AccountId?,
    ): UserTokensResponse {
        val tokens = if (userWallet != null) {
            getDefaultWalletBlockchains(userWallet = userWallet, demoConfig = DemoConfig)
                .map { blockchain ->
                    val derivationPath = networkFactory.createDerivationPath(
                        blockchain = blockchain,
                        extraDerivationPath = null,
                        cardDerivationStyleProvider = userWallet.derivationStyleProvider,
                    ).value

                    UserTokensResponse.Token(
                        id = blockchain.toCoinId(),
                        accountId = accountId?.value,
                        networkId = blockchain.toNetworkId(),
                        derivationPath = derivationPath,
                        name = blockchain.getCoinName(),
                        symbol = blockchain.currency,
                        decimals = blockchain.decimals(),
                        contractAddress = null,
                    )
                }
        } else {
            emptyList()
        }

        return UserTokensResponse(
            group = UserTokensResponse.GroupType.NONE,
            sort = UserTokensResponse.SortType.MANUAL,
            tokens = tokens,
        )
    }
}