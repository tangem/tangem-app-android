package com.tangem.domain.dynamicaddresses

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.derivations.derivationStyleProvider

/**
 * Whether the Dynamic Addresses menu entry should be shown for a given (wallet, currency) pair.
 * Policy check only — hardware XPUB capability is verified by [IsXpubSupportedUseCase].
 */
class IsDynamicAddressesAvailableUseCase(
    private val featureToggles: DynamicAddressesFeatureToggles,
) {

    operator fun invoke(userWallet: UserWallet, cryptoCurrency: CryptoCurrency): Boolean {
        if (!featureToggles.isDynamicAddressesEnabled) return false
        if (cryptoCurrency !is CryptoCurrency.Coin) return false

        val network = cryptoCurrency.network
        if (!DynamicAddressesSupportedBlockchains.isSupportedByNetworkId(network.rawId)) return false

        return isWalletDefaultDerivation(userWallet, network)
    }

    private fun isWalletDefaultDerivation(userWallet: UserWallet, network: Network): Boolean {
        val actualPath = network.derivationPath.value ?: return false
        if (!DynamicAddressesDerivationChecker.isBaseDerivation(actualPath)) return false

        val style = userWallet.derivationStyleProvider.getDerivationStyle() ?: return false
        val expectedPath = network.toBlockchain().derivationPath(style)?.rawPath ?: return false

        val actualNodes = runCatching { DerivationPath(actualPath).nodes }.getOrNull() ?: return false
        val expectedNodes = runCatching { DerivationPath(expectedPath).nodes }.getOrNull() ?: return false
        if (actualNodes.size < BIP44_NODE_COUNT || expectedNodes.size < BIP44_NODE_COUNT) return false

        // Match purpose + coin_type; account is allowed to differ for secondary accounts.
        return actualNodes[PURPOSE_NODE_INDEX] == expectedNodes[PURPOSE_NODE_INDEX] &&
            actualNodes[COIN_TYPE_NODE_INDEX] == expectedNodes[COIN_TYPE_NODE_INDEX]
    }

    private companion object {
        const val BIP44_NODE_COUNT = 5
        const val PURPOSE_NODE_INDEX = 0
        const val COIN_TYPE_NODE_INDEX = 1
    }
}