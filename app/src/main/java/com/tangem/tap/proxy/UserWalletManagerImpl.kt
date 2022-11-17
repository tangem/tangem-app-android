package com.tangem.tap.proxy

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.lib.crypto.models.Token
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.extensions.getUserWalletId
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.redux.WalletAction
import org.rekotlin.Store

class UserWalletManagerImpl(
    private val userTokensRepository: UserTokensRepository,
    private val cardStateHolder: CardStateHolder,
    private val mainStore: Store<AppState>,
) : UserWalletManager {

    override suspend fun getUserTokens(): List<Token> {
        val card = cardStateHolder.getActualCard()
        if (card != null) {
            userTokensRepository.getUserTokens(card)
                .filter { it.isToken() }
                .map {
                    Token(
                        id = it.coinId ?: "",
                        name = it.currencyName,
                        symbol = it.currencySymbol,
                        networkId = it.blockchain.toNetworkId(),
                        contractAddress = null,
                        decimalCount = it.decimals,
                    )
                }
        }
        return emptyList()
    }

    override fun getWalletId(): String {
        return cardStateHolder.getActualCard()?.getUserWalletId() ?: ""
    }

    override suspend fun isTokenAdded(token: Token): Boolean {
        val card = cardStateHolder.getActualCard()
        if (card != null) {
            return userTokensRepository.getUserTokens(card)
                .any { it.coinId.equals(token.id) } //todo ensure that its the same ids
        }
        return false
    }

    //todo check is it sync operation
    override fun addToken(token: Token) {
        val card = cardStateHolder.getActualCard()
        val blockchain = Blockchain.fromNetworkId(token.networkId)
        if (card != null && blockchain != null) {
            val addAction = WalletAction.MultiWallet.AddToken(
                token = com.tangem.blockchain.common.Token(
                    id = token.id,
                    name = token.name,
                    symbol = token.symbol,
                    contractAddress = token.contractAddress
                        ?: "", //todo clarify where to find that if data from back null
                    decimals = token.decimalCount ?: 0, //todo clarify where to find that if data from back null
                ),
                blockchain = BlockchainNetwork(
                    blockchain,
                    card,
                ),
                save = true,
            )
            mainStore.dispatch(addAction)
        }
    }

    override fun getWalletAddress(token: Token): String {
        return ""
    }
}