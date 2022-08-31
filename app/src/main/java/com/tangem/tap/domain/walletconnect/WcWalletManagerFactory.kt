package com.tangem.tap.domain.walletconnect

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.tokens.CurrenciesRepository
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.details.redux.walletconnect.WalletForSession
import com.tangem.tap.features.wallet.redux.WalletState

class WcWalletManagerFactory(
    private val factory: WalletManagerFactory,
    private val currenciesRepository: CurrenciesRepository,
) {
    fun getWalletManager(
        wallet: WalletForSession, blockchain: Blockchain, walletState: WalletState,
    ): WalletManager? {
        val blockchainToMake = if (blockchain == Blockchain.Ethereum && wallet.isTestNet) {
            Blockchain.EthereumTestnet
        } else {
            blockchain
        }
        val blockchainNetwork = BlockchainNetwork(
            blockchain = blockchainToMake,
            derivationPath = wallet.derivationPath?.rawPath,
            tokens = emptyList(),
        )
        return walletState.getWalletManager(blockchainNetwork)
    }

    suspend fun getWalletManager(
        scanResponse: ScanResponse, blockchain: Blockchain, walletState: WalletState,
    ): WalletManager? {
        val card = scanResponse.card
        val blockchainToMake = if (blockchain == Blockchain.Ethereum && card.isTestCard) {
            Blockchain.EthereumTestnet
        } else {
            blockchain
        }
        val blockchainNetwork = BlockchainNetwork(
            blockchain = blockchainToMake,
            card = card,
        )

        return if (walletState.cardId == card.cardId) {
            walletState.getWalletManager(blockchainNetwork)
        } else {
            if (currenciesRepository
                    .loadSavedCurrencies(card.cardId, card.settings.isHDWalletAllowed)
                    .contains(blockchainNetwork)
            ) {
                factory.makeWalletManagerForApp(
                    scanResponse = scanResponse,
                    blockchainNetwork = blockchainNetwork,
                )
            } else {
                null
            }
        }
    }
}