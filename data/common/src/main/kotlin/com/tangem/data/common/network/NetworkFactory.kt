package com.tangem.data.common.network

import androidx.annotation.VisibleForTesting
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.FeePaidCurrency
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.getSupportedTransactionExtras
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.card.common.extensions.canHandleToken
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.derivations.DerivationStyleProvider
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.lib.crypto.derivation.toMutable
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Factory for creating [Network]
 *
 * @property excludedBlockchains excluded blockchains
 *
[REDACTED_AUTHOR]
 */
@Suppress("LargeClass")
class NetworkFactory @Inject constructor(
    private val excludedBlockchains: ExcludedBlockchains,
) {

    /**
     * Create
     *
     * @param blockchain          blockchain
     * @param extraDerivationPath extra derivation path
     * @param userWallet          user wallet
     */
    fun create(
        blockchain: Blockchain,
        extraDerivationPath: String?,
        userWallet: UserWallet,
        accountIndex: DerivationIndex? = null,
    ): Network? {
        return create(
            blockchain = blockchain,
            derivationPath = createDerivationPath(
                blockchain = blockchain,
                extraDerivationPath = extraDerivationPath,
                cardDerivationStyleProvider = userWallet.derivationStyleProvider,
                accountIndex = accountIndex,
            ),
            canHandleTokens = userWallet.canHandleToken(
                blockchain = blockchain,
                excludedBlockchains = excludedBlockchains,
            ),
            accountIndex = accountIndex,
        )
    }

    /**
     * Create
     *
     * @param networkId      network id
     * @param derivationPath derivation path
     * @param userWallet     user wallet
     */
    fun create(networkId: Network.ID, derivationPath: Network.DerivationPath, userWallet: UserWallet): Network? {
        val blockchain = networkId.toBlockchain()

        return create(
            blockchain = blockchain,
            derivationPath = derivationPath,
            canHandleTokens = userWallet.canHandleToken(
                blockchain = blockchain,
                excludedBlockchains = excludedBlockchains,
            ),
            shouldCheckChia = false,
        )
    }

    /**
     * Create
     *
     * @param blockchain     blockchain
     * @param derivationPath derivation path
     * @param userWallet     user wallet
     */
    fun create(blockchain: Blockchain, derivationPath: Network.DerivationPath, userWallet: UserWallet): Network? {
        return create(
            blockchain = blockchain,
            derivationPath = derivationPath,
            canHandleTokens = userWallet.canHandleToken(
                blockchain = blockchain,
                excludedBlockchains = excludedBlockchains,
            ),
        )
    }

    /**
     * Create
     *
     * @param blockchain              blockchain
     * @param extraDerivationPath     extra derivation path
     * @param derivationStyleProvider derivation style provider
     * @param canHandleTokens         flag that indicates whether the network can handle tokens
     */
    fun create(
        blockchain: Blockchain,
        extraDerivationPath: String?,
        derivationStyleProvider: DerivationStyleProvider?,
        canHandleTokens: Boolean,
        accountIndex: DerivationIndex? = null,
    ): Network? {
        return create(
            blockchain = blockchain,
            derivationPath = createDerivationPath(
                blockchain = blockchain,
                extraDerivationPath = extraDerivationPath,
                cardDerivationStyleProvider = derivationStyleProvider,
                accountIndex = accountIndex,
            ),
            canHandleTokens = canHandleTokens,
            accountIndex = accountIndex,
        )
    }

    private fun create(
        blockchain: Blockchain,
        derivationPath: Network.DerivationPath,
        canHandleTokens: Boolean,
        accountIndex: DerivationIndex? = null,
        shouldCheckChia: Boolean = true,
    ): Network? {
        if (!blockchain.isBlockchainSupported()) return null
        if (shouldCheckChia && blockchain == Blockchain.Chia && accountIndex != DerivationIndex.Main) return null

        return runCatching {
            Network(
                id = Network.ID(value = blockchain.toNetworkId(), derivationPath = derivationPath),
                name = blockchain.fullName,
                isTestnet = blockchain.isTestnet(),
                derivationPath = derivationPath,
                currencySymbol = blockchain.currency,
                standardType = getNetworkStandardType(blockchain),
                hasFiatFeeRate = blockchain.feePaidCurrency() !is FeePaidCurrency.FeeResource,
                canHandleTokens = canHandleTokens,
                transactionExtrasType = blockchain.getSupportedTransactionExtras(),
                nameResolvingType = blockchain.getNameResolvingType(),
            )
        }
            .getOrNull()
    }

    private fun Blockchain.isBlockchainSupported(): Boolean {
        if (this == Blockchain.Unknown) {
            TangemLogger.w("Unable to convert Unknown blockchain to the domain network model")
            return false
        }
        if (this in excludedBlockchains) {
            TangemLogger.w("Unable to convert excluded blockchain to the domain network model")
            return false
        }

        return true
    }

    fun createDerivationPath(
        blockchain: Blockchain,
        extraDerivationPath: String?,
        cardDerivationStyleProvider: DerivationStyleProvider?,
        accountIndex: DerivationIndex? = null,
    ): Network.DerivationPath {
        if (cardDerivationStyleProvider == null) return Network.DerivationPath.None

        val defaultDerivationPath = getDefaultDerivationPath(blockchain, cardDerivationStyleProvider, accountIndex)

        return if (extraDerivationPath.isNullOrBlank()) {
            if (defaultDerivationPath.isNullOrBlank()) {
                Network.DerivationPath.None
            } else {
                Network.DerivationPath.Card(defaultDerivationPath)
            }
        } else {
            if (extraDerivationPath == defaultDerivationPath) {
                Network.DerivationPath.Card(defaultDerivationPath)
            } else {
                Network.DerivationPath.Custom(extraDerivationPath)
            }
        }
    }

    private fun getDefaultDerivationPath(
        blockchain: Blockchain,
        derivationStyleProvider: DerivationStyleProvider,
        accountIndex: DerivationIndex?,
    ): String? {
        val default = blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())
            ?: return null

        return if (accountIndex == null || accountIndex == DerivationIndex.Main) {
            default
        } else {
            default.toMutable()
                .replaceAccountNode(value = accountIndex.value.toLong(), blockchain = blockchain)
                .apply()
        }
            .rawPath
    }

    private fun getNetworkStandardType(blockchain: Blockchain): Network.StandardType {
        return when (blockchain) {
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> Network.StandardType.ERC20
            Blockchain.BSC, Blockchain.BSCTestnet -> Network.StandardType.BEP20
            Blockchain.Binance, Blockchain.BinanceTestnet -> Network.StandardType.BEP2
            Blockchain.Tron, Blockchain.TronTestnet -> Network.StandardType.TRC20
            else -> Network.StandardType.Unspecified(blockchain.name)
        }
    }

    private fun Blockchain.getNameResolvingType(): Network.NameResolvingType {
        return when (this) {
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> Network.NameResolvingType.ENS
            else -> Network.NameResolvingType.NONE
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun createNetworkStandardType(blockchain: Blockchain) = getNetworkStandardType(blockchain)

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun createSupportedTransactionExtras(blockchain: Blockchain) = blockchain.getSupportedTransactionExtras()
}