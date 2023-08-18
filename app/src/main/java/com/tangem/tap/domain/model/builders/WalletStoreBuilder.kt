package com.tangem.tap.domain.model.builders

import com.tangem.blockchain.blockchains.polkadot.ExistentialDepositProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.reducers.createAddressesData
import java.math.BigDecimal

interface WalletStoreBuilder {
    fun build(): WalletStoreModel

    interface BlockchainNetworkWalletStoreBuilder : WalletStoreBuilder {
        fun walletManager(walletManager: WalletManager?): WalletStoreBuilder
    }

    interface WalletMangerWalletStoreBuilder : WalletStoreBuilder

    companion object {
        operator fun invoke(
            userWallet: UserWallet,
            blockchainNetwork: BlockchainNetwork,
        ): BlockchainNetworkWalletStoreBuilder {
            return BlockchainNetworkWalletStoreBuilderImpl(userWallet, blockchainNetwork)
        }

        operator fun invoke(userWallet: UserWallet, walletManager: WalletManager): WalletMangerWalletStoreBuilder {
            return WalletMangerWalletStoreBuilderImpl(userWallet, walletManager)
        }
    }
}

private class BlockchainNetworkWalletStoreBuilderImpl(
    private val userWallet: UserWallet,
    private val blockchainNetwork: BlockchainNetwork,
) : WalletStoreBuilder.BlockchainNetworkWalletStoreBuilder {
    private var walletManager: WalletManager? = null

    override fun walletManager(walletManager: WalletManager?) = this.apply {
        this.walletManager = walletManager
    }

    override fun build(): WalletStoreModel {
        val cardDerivationStyle = userWallet.scanResponse.derivationStyleProvider.getDerivationStyle()
        val blockchainWalletData = blockchainNetwork.getBlockchainWalletData(walletManager, cardDerivationStyle)
        val tokensWalletsData = blockchainNetwork.getTokensWalletsData(
            walletManager = walletManager,
            cardDerivationStyle = cardDerivationStyle,
            primaryToken = userWallet.scanResponse.cardTypesResolver.getPrimaryToken(),
        )

        return WalletStoreModel(
            userWalletId = userWallet.walletId,
            blockchain = blockchainNetwork.blockchain,
            derivationPath = blockchainNetwork.derivationPath?.let { DerivationPath(it) },
            walletsData = listOf(blockchainWalletData) + tokensWalletsData,
            walletRent = null,
            walletManager = walletManager,
            blockchainNetwork = blockchainNetwork,
        )
    }
}

private class WalletMangerWalletStoreBuilderImpl(
    private val userWallet: UserWallet,
    private val walletManager: WalletManager,
) : WalletStoreBuilder.WalletMangerWalletStoreBuilder {

    override fun build(): WalletStoreModel {
        val wallet = walletManager.wallet
        val blockchainWalletData = wallet.blockchain.toBlockchainWalletData(walletManager)
        val tokenWalletsData = wallet.getTokens().firstOrNull()?.toTokenWalletData(
            walletManager = walletManager,
            primaryToken = userWallet.scanResponse.cardTypesResolver.getPrimaryToken(),
        )

        return WalletStoreModel(
            userWalletId = userWallet.walletId,
            blockchain = wallet.blockchain,
            derivationPath = wallet.publicKey.derivationPath,
            walletsData = listOf(blockchainWalletData) + listOfNotNull(tokenWalletsData),
            walletRent = null,
            walletManager = walletManager,
            blockchainNetwork = BlockchainNetwork.fromWalletManager(walletManager),
        )
    }
}

private fun BlockchainNetwork.getBlockchainWalletData(
    walletManager: WalletManager?,
    cardDerivationStyle: DerivationStyle?,
): WalletDataModel {
    val currency = Currency.Blockchain(
        blockchain = blockchain,
        derivationPath = derivationPath,
    )
    return WalletDataModel(
        currency = currency,
        status = WalletDataModel.Loading,
        walletAddresses = walletManager?.wallet?.getWalletAddresses(),
        existentialDeposit = getExistentialDeposit(walletManager),
        fiatRate = null,
        isCardSingleToken = false,
        isCustom = currency.isCustomCurrency(cardDerivationStyle),
    )
}

private fun BlockchainNetwork.getTokensWalletsData(
    walletManager: WalletManager?,
    cardDerivationStyle: DerivationStyle?,
    primaryToken: Token?,
): List<WalletDataModel> {
    return this.tokens
        .map { token ->
            val currency = Currency.Token(
                token = token,
                blockchain = blockchain,
                derivationPath = derivationPath,
            )
            WalletDataModel(
                currency = currency,
                status = WalletDataModel.Loading,
                walletAddresses = walletManager?.wallet?.getWalletAddresses(),
                existentialDeposit = getExistentialDeposit(walletManager),
                fiatRate = null,
                isCardSingleToken = token == primaryToken,
                isCustom = currency.isCustomCurrency(cardDerivationStyle),
            )
        }
}

private fun Blockchain.toBlockchainWalletData(walletManager: WalletManager): WalletDataModel {
    val wallet = walletManager.wallet
    return WalletDataModel(
        currency = Currency.Blockchain(
            blockchain = this,
            derivationPath = wallet.publicKey.derivationPath?.rawPath,
        ),
        status = WalletDataModel.Loading,
        walletAddresses = wallet.getWalletAddresses(),
        existentialDeposit = getExistentialDeposit(walletManager),
        fiatRate = null,
        isCardSingleToken = false,
        isCustom = false,
    )
}

private fun Token.toTokenWalletData(walletManager: WalletManager, primaryToken: Token?): WalletDataModel {
    val wallet = walletManager.wallet
    return WalletDataModel(
        currency = Currency.Token(
            token = this,
            blockchain = wallet.blockchain,
            derivationPath = wallet.publicKey.derivationPath?.rawPath,
        ),
        status = WalletDataModel.Loading,
        walletAddresses = wallet.getWalletAddresses(),
        existentialDeposit = getExistentialDeposit(walletManager),
        fiatRate = null,
        isCardSingleToken = this == primaryToken,
        isCustom = false,
    )
}

private fun getExistentialDeposit(walletManager: WalletManager?): BigDecimal? {
    return (walletManager as? ExistentialDepositProvider)?.getExistentialDeposit()
}

private fun Wallet.getWalletAddresses(): WalletDataModel.WalletAddresses? {
    return this.createAddressesData()
        .takeIf { it.isNotEmpty() }
        ?.let { addresses ->
            WalletDataModel.WalletAddresses(
                list = addresses,
                selectedAddress = addresses.first(),
            )
        }
}
