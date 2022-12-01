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

class WalletStoreBuilder(
    private val userWallet: UserWallet,
) {
    private var walletManager: WalletManager? = null
    private var blockchainNetwork: BlockchainNetwork? = null

    fun setWalletManager(walletManager: WalletManager?) = this.apply {
        this.walletManager = walletManager
    }

    fun setBlockchainNetwork(blockchainNetwork: BlockchainNetwork?) = this.apply {
        this.blockchainNetwork = blockchainNetwork
    }

    fun build(): WalletStoreModel {
        val blockchainNetwork = this.blockchainNetwork
            ?: walletManager?.let(BlockchainNetwork::fromWalletManager)
            ?: error("Blockchain network and wallet manager must not be null")

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