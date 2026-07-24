package com.tangem.data.staking.verification

import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.encodeBase64NoWrap
import com.tangem.common.extensions.hexToBytes
import com.tangem.data.staking.verification.model.EvmStakingTx
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.staking.verification.StakingBlockAidRequestFactory

internal class DefaultStakingBlockAidRequestFactory : StakingBlockAidRequestFactory {

    private val moshi = Moshi.Builder().build()
    private val evmAdapter = moshi.adapter(EvmStakingTx::class.java)
    private val evmListAdapter = moshi.adapter<List<EvmStakingTx>>(
        Types.newParameterizedType(List::class.java, EvmStakingTx::class.java),
    )

    override fun create(network: NetworkType, accountAddress: String, unsignedTransaction: String): TransactionData {
        return when (network) {
            NetworkType.ETHEREUM,
            NetworkType.POLYGON,
            NetworkType.BINANCE,
            -> TransactionData(
                chain = evmChainName(network),
                accountAddress = accountAddress,
                method = EVM_METHOD,
                domainUrl = NON_DAPP_DOMAIN,
                params = TransactionParams.Evm(params = normalizeEvmParams(unsignedTransaction)),
            )
            NetworkType.SOLANA -> TransactionData(
                chain = SOLANA_CHAIN,
                // Blockaid's Solana scan expects the address as base64 of its base58-decoded bytes
                // (same conversion as the WalletConnect flow), not the raw base58 string.
                accountAddress = accountAddress.decodeBase58()?.encodeBase64NoWrap() ?: accountAddress,
                method = SOLANA_METHOD,
                domainUrl = NON_DAPP_DOMAIN,
                params = TransactionParams.Solana(
                    transactions = listOf(unsignedTransaction.hexToBytes().encodeBase64NoWrap()),
                ),
            )
            else -> throw IllegalArgumentException("Unsupported Blockaid staking network: $network")
        }
    }

    // Blockaid's eth_sendTransaction expects only the standard tx fields. StakeKit's payload also
    // carries gasLimit/nonce/maxFeePerGas/chainId/type etc., so strip it down to {from,to,data,value}
    // (matching iOS); value is omitted when absent. Falls back to the raw payload if it can't be parsed.
    private fun normalizeEvmParams(unsignedTransaction: String): String {
        val tx = evmAdapter.fromJson(unsignedTransaction) ?: return "[$unsignedTransaction]"
        return evmListAdapter.toJson(listOf(tx))
    }

    private fun evmChainName(network: NetworkType): String = when (network) {
        NetworkType.ETHEREUM -> "ethereum"
        NetworkType.POLYGON -> "polygon"
        NetworkType.BINANCE -> "bsc"
        else -> throw IllegalArgumentException("Not an EVM staking network: $network")
    }

    private companion object {
        const val EVM_METHOD = "eth_sendTransaction"
        const val SOLANA_METHOD = "signTransaction"
        const val SOLANA_CHAIN = "mainnet"
        const val NON_DAPP_DOMAIN = ""
    }
}