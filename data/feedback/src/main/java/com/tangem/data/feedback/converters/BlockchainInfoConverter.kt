package com.tangem.data.feedback.converters

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.address.Address
import com.tangem.domain.feedback.models.BlockchainInfo
import com.tangem.domain.feedback.models.BlockchainInfo.Addresses.Multiple.AddressInfo
import com.tangem.utils.converter.Converter
import com.tangem.domain.feedback.models.BlockchainInfo.Addresses as BlockchainAddresses

/**
 * Converter from [WalletManager] to [BlockchainInfo]
 *
[REDACTED_AUTHOR]
 */
internal object BlockchainInfoConverter : Converter<WalletManager, BlockchainInfo> {

    override fun convert(value: WalletManager): BlockchainInfo {
        return BlockchainInfo(
            blockchain = value.wallet.blockchain.fullName,
            derivationPath = value.wallet.publicKey.derivationPath?.rawPath ?: "",
            outputsCount = value.outputsCount?.toString(),
            host = value.currentHost,
            addresses = value.wallet.mapAddresses(Address::value),
            explorerLinks = value.wallet.mapAddresses { value.wallet.getExploreUrl(it.value) },
            tokens = value.cardTokens.map { token ->
                BlockchainInfo.TokenInfo(
                    id = token.id,
                    name = token.name,
                    contractAddress = token.contractAddress,
                    decimals = token.decimals.toString(),
                )
            },
        )
    }

    private inline fun Wallet.mapAddresses(map: (Address) -> String): BlockchainAddresses {
        return if (addresses.size == 1) {
            BlockchainAddresses.Single(value = map(addresses.first()))
        } else {
            BlockchainAddresses.Multiple(
                addresses.map {
                    AddressInfo(type = it.type.name, value = map(it))
                },
            )
        }
    }
}