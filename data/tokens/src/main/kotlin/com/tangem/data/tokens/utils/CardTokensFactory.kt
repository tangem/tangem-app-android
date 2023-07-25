package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.Network
import timber.log.Timber
import com.tangem.blockchain.common.Token as SdkToken
import com.tangem.domain.tokens.model.Token as DomainToken

@Suppress("unused") // TODO: Will be used in next MR
internal class CardTokensFactory(private val demoConfig: DemoConfig) {

    fun createDefaultTokensForMultiCurrencyCard(card: CardDTO): Set<DomainToken> {
        var blockchains = if (demoConfig.isDemoCardId(card.cardId)) {
            demoConfig.demoBlockchains
        } else {
            listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }

        if (card.isTestCard) {
            blockchains = blockchains.mapNotNull { it.getTestnetVersion() }
        }

        return blockchains.mapNotNull { createCoin(it, card) }.toSet()
    }

    fun createTokensForSingleCurrencyCard(scanResponse: ScanResponse): Set<DomainToken> {
        val card = scanResponse.card
        val resolver = scanResponse.cardTypesResolver
        val blockchain = resolver.getBlockchain()

        val coin = requireNotNull(createCoin(blockchain, card)) {
            "Coin for the single currency card cannot be null"
        }
        val primaryToken = resolver.getPrimaryToken()?.let { token ->
            createToken(token, blockchain, card)
        }

        return buildSet {
            add(coin)

            if (primaryToken != null) {
                add(primaryToken)
            }
        }
    }

    private fun createToken(sdkToken: SdkToken, blockchain: Blockchain, card: CardDTO): DomainToken? {
        if (blockchain != Blockchain.Unknown) {
            Timber.e("Unable to map the SDK token to the domain token with Unknown blockchain")
            return null
        }

        return DomainToken(
            id = getTokenId(sdkToken, blockchain),
            networkId = Network.ID(blockchain.id),
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            iconUrl = getCoinOrTokenIconUrl(sdkToken, blockchain),
            decimals = sdkToken.decimals,
            isCustom = false,
            contractAddress = sdkToken.contractAddress,
            derivationPath = getDerivationPath(blockchain, card),
        )
    }

    private fun createCoin(blockchain: Blockchain, card: CardDTO): DomainToken? {
        if (blockchain != Blockchain.Unknown) {
            Timber.e("Unable to map the SDK token to the domain token with Unknown blockchain")
            return null
        }

        return DomainToken(
            id = getCoinId(blockchain),
            networkId = Network.ID(blockchain.id),
            name = blockchain.fullName,
            symbol = blockchain.currency,
            iconUrl = getCoinIconId(blockchain),
            decimals = blockchain.decimals(),
            isCustom = false,
            contractAddress = null,
            derivationPath = getDerivationPath(blockchain, card),
        )
    }
}