package com.tangem.data.walletconnect.network.ton

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.common.extensions.toHexString
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class TonSessionPropertiesProvider @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend fun getSessionProperties(userWalletId: UserWalletId, networks: List<Network>): Map<String, String>? {
        val tonNetwork = networks.find { it.toBlockchain().isTon() } ?: return null

        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, tonNetwork)
            ?: return null

        val publicKeyHex = walletManager.wallet.publicKey.blockchainKey.toHexString()
        TangemLogger.withTag(WC_TAG).i("TON session properties: publicKey=$publicKeyHex")

        return buildMap {
            put(TON_PUBLIC_KEY_PROPERTY, publicKeyHex)
        }
    }

    private fun Blockchain.isTon(): Boolean = this == Blockchain.TON || this == Blockchain.TONTestnet

    companion object {
        private const val TON_PUBLIC_KEY_PROPERTY = "ton_getPublicKey"
    }
}