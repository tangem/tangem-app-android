package com.tangem.tap.domain.model.builders

import com.tangem.blockchain.blockchains.polkadot.ExistentialDepositProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.domain.common.util.UserWalletId
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

    interface WalletMangerWalletStoreBuilder : WalletStoreBuilder

    companion object {
        operator fun invoke(
            userWalletId: UserWalletId,
            blockchainNetwork: BlockchainNetwork,
        ): BlockchainNetworkWalletStoreBuilder {
            return BlockchainNetworkWalletStoreBuilderImpl(userWalletId, blockchainNetwork)
        }

        operator fun invoke(
            userWalletId: UserWalletId,
            walletManager: WalletManager,
        ): WalletMangerWalletStoreBuilder {
            return WalletMangerWalletStoreBuilderImpl(userWalletId, walletManager)
        }
    }
}

private class BlockchainNetworkWalletStoreBuilderImpl(
    private val userWalletId: UserWalletId,
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
            userWalletId = userWalletId,
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
    private val userWalletId: UserWalletId,
    private val walletManager: WalletManager,
) : WalletStoreBuilder.WalletMangerWalletStoreBuilder {

    override fun build(): WalletStoreModel {
        val wallet = walletManager.wallet
        val blockchainWalletData = wallet.blockchain.toBlockchainWalletData(walletManager)
        val tokenWalletsData = wallet.getTokens().firstOrNull()?.toTokenWalletData(walletManager)

        return WalletStoreModel(
            userWalletId = userWalletId,
            blockchain = wallet.blockchain,
            derivationPath = wallet.publicKey.derivationPath,
            walletsData = listOf(blockchainWalletData) + listOfNotNull(tokenWalletsData),
            walletRent = null,
            walletManager = walletManager,
            blockchainNetwork = BlockchainNetwork.fromWalletManager(walletManager),
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

private fun Blockchain.toBlockchainWalletData(walletManager: WalletManager): WalletDataModel {
    val wallet = walletManager.wallet
    return WalletDataModel(
        currency = Currency.Blockchain(
            blockchain = this,
            derivationPath = wallet.publicKey.derivationPath?.rawPath,
        ),
        status = WalletDataModel.Loading,
        walletAddresses = wallet.createAddressesData(),
        existentialDeposit = getExistentialDeposit(walletManager),
        fiatRate = null,
    )
}

private fun Token.toTokenWalletData(walletManager: WalletManager): WalletDataModel {
    val wallet = walletManager.wallet
    return WalletDataModel(
        currency = Currency.Token(
            token = this,
            blockchain = wallet.blockchain,
            derivationPath = wallet.publicKey.derivationPath?.rawPath,
        ),
        status = WalletDataModel.Loading,
        walletAddresses = wallet.createAddressesData(),
        existentialDeposit = getExistentialDeposit(walletManager),
        fiatRate = null,
    )
}

private fun getExistentialDeposit(walletManager: WalletManager?): BigDecimal? {
    return (walletManager as? ExistentialDepositProvider)?.getExistentialDeposit()
}
