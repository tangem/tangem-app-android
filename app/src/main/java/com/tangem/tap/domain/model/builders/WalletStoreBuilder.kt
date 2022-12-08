package com.tangem.tap.domain.model.builders

import com.tangem.blockchain.blockchains.polkadot.ExistentialDepositProvider
import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.reducers.createAddressesData
import java.math.BigDecimal

interface WalletStoreBuilder {
    fun build(): WalletStoreModel

    interface BlockchainNetworkWalletStoreBuilder : WalletStoreBuilder {
        fun walletManager(walletManager: WalletManager?): WalletStoreBuilder
    }

    interface WalletMangerWalletStoreBuilder : WalletStoreBuilder {
        fun blockchainNetwork(blockchainNetwork: BlockchainNetwork?): WalletStoreBuilder
    }

    companion object {
        operator fun invoke(
            userWallet: UserWallet,
            blockchainNetwork: BlockchainNetwork,
        ): BlockchainNetworkWalletStoreBuilder {
            return BlockchainNetworkWalletStoreBuilderImpl(userWallet, blockchainNetwork)
        }

        operator fun invoke(
            userWallet: UserWallet,
            walletManager: WalletManager,
        ): WalletMangerWalletStoreBuilder {
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
        val blockchainWalletData = blockchainNetwork.getBlockchainWalletData(walletManager)
        val tokensWalletsData = blockchainNetwork.getTokensWalletsData(walletManager)

        return WalletStoreModel(
            userWalletId = userWallet.walletId,
            blockchainNetwork = blockchainNetwork,
            walletManager = walletManager,
            walletsData = (listOf(blockchainWalletData) + tokensWalletsData),
            walletRent = null,
        )
    }
}

private class WalletMangerWalletStoreBuilderImpl(
    private val userWallet: UserWallet,
    private val walletManager: WalletManager,
) : WalletStoreBuilder.WalletMangerWalletStoreBuilder {
    private var blockchainNetwork: BlockchainNetwork? = null

    override fun blockchainNetwork(blockchainNetwork: BlockchainNetwork?) = this.apply {
        this.blockchainNetwork = blockchainNetwork
    }

    override fun build(): WalletStoreModel {
        val blockchainNetwork = this.blockchainNetwork ?: BlockchainNetwork.fromWalletManager(walletManager)
        val blockchainWalletData = blockchainNetwork.getBlockchainWalletData(walletManager)
        val tokensWalletsData = blockchainNetwork.getTokensWalletsData(walletManager)

        return WalletStoreModel(
            userWalletId = userWallet.walletId,
            blockchainNetwork = blockchainNetwork,
            walletManager = walletManager,
            walletsData = (listOf(blockchainWalletData) + tokensWalletsData),
            walletRent = null,
        )
    }
}

private fun BlockchainNetwork.getBlockchainWalletData(walletManager: WalletManager?): WalletDataModel {
    return WalletDataModel(
        currency = Currency.Blockchain(
            blockchain = blockchain,
            derivationPath = derivationPath,
        ),
        status = WalletDataModel.Loading,
        walletAddresses = walletManager?.wallet?.createAddressesData().orEmpty(),
        existentialDeposit = getExistentialDeposit(walletManager),
        fiatRate = null,
    )
}

private fun BlockchainNetwork.getTokensWalletsData(walletManager: WalletManager?): List<WalletDataModel> {
    return this.tokens
        .map { token ->
            WalletDataModel(
                currency = Currency.Token(
                    token = token,
                    blockchain = blockchain,
                    derivationPath = derivationPath,
                ),
                status = WalletDataModel.Loading,
                walletAddresses = walletManager?.wallet?.createAddressesData().orEmpty(),
                existentialDeposit = getExistentialDeposit(walletManager),
                fiatRate = null,
            )
        }
}

private fun getExistentialDeposit(walletManager: WalletManager?): BigDecimal? {
    return (walletManager as? ExistentialDepositProvider)?.getExistentialDeposit()
}