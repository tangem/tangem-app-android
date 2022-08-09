package com.tangem.tap.domain.walletconnect

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.tap.domain.extensions.getPrimaryCurve
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.tokens.CurrenciesRepository
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.details.redux.walletconnect.WalletForSession
import com.tangem.tap.features.wallet.redux.WalletState

class WcWalletManagerFactory(
    private val factory: WalletManagerFactory,
    private val currenciesRepository: CurrenciesRepository,
    ) {

    suspend fun getWalletManager(
        wallet: WalletForSession, blockchain: Blockchain, walletState: WalletState
    ): WalletManager? {
        val blockchainToMake = if (blockchain == Blockchain.Ethereum && wallet.isTestNet) {
            Blockchain.EthereumTestnet
        } else {
            blockchain
        }

        val blockchainNetwork = BlockchainNetwork(
            blockchain = blockchainToMake,
            derivationPath = wallet.derivationPath?.rawPath,
            tokens = emptyList()
        )

        return if (walletState.cardId == wallet.cardId) {
            walletState.getWalletManager(blockchainNetwork)
        } else {
            val blockchainNetworkWithTokens = currenciesRepository
                .loadSavedCurrencies(
                    cardId = wallet.cardId,
                    isHdWalletSupported = wallet.derivationPath != null
                ).firstOrNull { it == blockchainNetwork }

            if (blockchainNetworkWithTokens != null) {
                factory.makeWalletManager(
                    blockchain = blockchainToMake,
                    publicKey = Wallet.PublicKey(
                        wallet.walletPublicKey!!,
                        wallet.derivedPublicKey,
                        wallet.derivationPath
                    ),
                    tokens = blockchainNetworkWithTokens.tokens,
                    curve = blockchainToMake.getPrimaryCurve() ?: EllipticCurve.Secp256k1
                )
            } else {
                null
            }
        }
    }

    suspend fun getWalletManager(
        scanResponse: ScanResponse, blockchain: Blockchain, walletState: WalletState
    ): WalletManager? {
        val card = scanResponse.card
        val blockchainToMake = if (blockchain == Blockchain.Ethereum && card.isTestCard) {
            Blockchain.EthereumTestnet
        } else {
            blockchain
        }

        val blockchainNetwork = BlockchainNetwork(
            blockchain = blockchainToMake,
            card = card
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
                    blockchainNetwork = blockchainNetwork
                )
            } else {
                null
            }
        }
    }
}