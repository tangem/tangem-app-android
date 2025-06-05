package com.tangem.lib.crypto

import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.l2BlockchainsList
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.lib.crypto.converter.XrpTaggedAddressConverter
import com.tangem.lib.crypto.models.XrpTaggedAddress
import java.math.BigDecimal

/**
 * !!!IMPORTANT!!!
 * Methods for working with different blockchains
 * All methods are depend on specific blockchain or check for specific blockchain
 *
 * Temporary solution for domain specific logic for Blockchain.
 * Instead of creating repositories and unnecessary and overkill use cases
 */
@Suppress("TooManyFunctions")
object BlockchainUtils {

    private const val XRP_X_ADDRESS = 'X'

    /** Decodes XRP Blockchain address */
    fun decodeRippleXAddress(xAddress: String, blockchainId: String): XrpTaggedAddress? {
        return if (blockchainId == Blockchain.XRP.id && xAddress.firstOrNull() == XRP_X_ADDRESS) {
            val decodedAddress = XrpAddressService.decodeXAddress(xAddress)
            return decodedAddress?.let(XrpTaggedAddressConverter()::convert)
        } else {
            null
        }
    }

    /** If current [networkId] is Bitcoin */
    fun isBitcoin(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.Bitcoin || blockchain == Blockchain.BitcoinTestnet
    }

    /** If current [networkId] is use custom fee */
    fun isUseBitcoinFeeConverter(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return isBitcoin(blockchainId) || blockchain == Blockchain.Fact0rn
    }

    /** If current [blockchainId] is Tezos */
    fun isTezos(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.Tezos
    }

    fun isCardano(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.Cardano
    }

    /** If current [blockchainId] is BeaconChain */
    fun isBeaconChain(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.Binance || blockchain == Blockchain.BinanceTestnet
    }

    /** If current [blockchainId] is Polygon */
    fun isPolygonChain(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.Polygon || blockchain == Blockchain.PolygonTestnet
    }

    fun isTron(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.Tron || blockchain == Blockchain.TronTestnet
    }

    fun isTon(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.TON || blockchain == Blockchain.TONTestnet
    }

    fun isSupportedNetworkId(blockchainId: String, excludedBlockchains: ExcludedBlockchains): Boolean {
        val blockchain = Blockchain.fromNetworkId(blockchainId)

        return blockchain != null && blockchain !in excludedBlockchains
    }

    fun isArbitrum(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.Arbitrum
    }

    fun isSolana(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.Solana
    }

    fun isPolkadot(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.Polkadot || blockchain == Blockchain.PolkadotTestnet
    }

    fun isCosmos(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.Cosmos || blockchain == Blockchain.CosmosTestnet
    }

    fun isBSC(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return blockchain == Blockchain.BSC || blockchain == Blockchain.BSCTestnet
    }

    data class BlockchainInfo(
        val blockchainId: String,
        val name: String,
        val protocolName: String,
    )

    fun getNetworkInfo(networkId: String): BlockchainInfo? {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: return null

        return BlockchainInfo(
            blockchainId = blockchain.id,
            name = getNetworkNameWithoutTestnet(blockchain),
            protocolName = getNetworkStandardName(blockchain),
        )
    }

    fun isL2Network(networkId: String): Boolean {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: return false
        return l2BlockchainsList.contains(blockchain)
    }

    fun getTezosThreshold(): BigDecimal = BigDecimal.ONE.movePointLeft(Blockchain.Tezos.decimals())

    /**
     * Blockchains not affecting total balance counting on errors
     */
    fun isIncludeToBalanceOnError(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)
        return when (blockchain) {
            Blockchain.Binance, Blockchain.BinanceTestnet -> true
            else -> false
        }
    }

    fun isIncludeStakingTotalBalance(blockchainId: String): Boolean {
        val blockchain = Blockchain.fromId(blockchainId)

        return blockchain != Blockchain.Cardano
    }

    fun isStakingRewardUnavailable(blockchainId: String): Boolean {
        return isSolana(blockchainId) || isBSC(blockchainId) || isTon(blockchainId)
    }

    private fun getNetworkStandardName(blockchain: Blockchain): String {
        return when (blockchain) {
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> "ERC20"
            Blockchain.BSC, Blockchain.BSCTestnet -> "BEP20"
            Blockchain.Binance, Blockchain.BinanceTestnet -> "BEP2"
            Blockchain.Tron, Blockchain.TronTestnet -> "TRC20"
            Blockchain.TON -> "TON"
            else -> ""
        }
    }

    private fun getNetworkNameWithoutTestnet(blockchain: Blockchain): String {
        return blockchain.fullName.replace(oldValue = " Testnet", newValue = "")
    }
}