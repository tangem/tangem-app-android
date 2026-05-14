package com.tangem.data.assetsdiscovery

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.walletmanager.extensions.makePublicKey
import com.tangem.domain.assetsdiscovery.AssetsDiscoveryFacade
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.config.curvesConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultAssetsDiscoveryFacade @Inject constructor(
    private val blockchainSDKFactory: BlockchainSDKFactory,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : AssetsDiscoveryFacade {

    override suspend fun getAssetsDiscoveryService(
        userWalletId: UserWalletId,
        network: Network,
    ): AssetsDiscoveryFacade.AssetsDiscoveryServiceInfo? = withContext(dispatchers.io) {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        if (userWallet !is UserWallet.Hot) return@withContext null

        val assetsDiscoveryServiceFactory = blockchainSDKFactory.getAssetsDiscoveryServiceFactorySync()
            ?: return@withContext null

        val blockchain = network.toBlockchain()
        val address = makeAddress(userWallet, blockchain, network.derivationPath.value)
            ?: return@withContext null

        AssetsDiscoveryFacade.AssetsDiscoveryServiceInfo(
            address = address,
            service = assetsDiscoveryServiceFactory.create(blockchain),
        )
    }

    private fun makeAddress(hotWallet: UserWallet.Hot, blockchain: Blockchain, derivationPath: String?): String? {
        val curve = hotWallet.curvesConfig.primaryCurve(blockchain)
        val selectedWallet = hotWallet.wallets.orEmpty().firstOrNull { it.curve == curve }
            ?: return null

        val path = derivationPath?.let { DerivationPath(rawPath = it) }

        val publicKey = if (path != null) {
            makePublicKey(
                seedKey = selectedWallet.publicKey,
                blockchain = blockchain,
                derivationPath = path,
                derivedWalletKeys = selectedWallet.derivedKeys,
                isWallet2 = true,
            ) ?: return null
        } else {
            null
        }

        return try {
            val addresses = if (publicKey != null) {
                blockchain.makeAddresses(
                    walletPublicKey = publicKey.blockchainKey,
                    pairPublicKey = null,
                    curve = selectedWallet.curve,
                )
            } else {
                blockchain.makeAddresses(
                    walletPublicKey = selectedWallet.publicKey,
                    pairPublicKey = null,
                    curve = selectedWallet.curve,
                )
            }
            addresses.find { it.type == AddressType.Default }?.value
        } catch (e: Throwable) {
            TangemLogger.w("Failed to derive address for $blockchain", e)
            null
        }
    }
}