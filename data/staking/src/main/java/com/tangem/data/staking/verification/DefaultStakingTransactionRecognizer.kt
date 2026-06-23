package com.tangem.data.staking.verification

import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.model.Array as CborArray
import co.nstant.`in`.cbor.model.Map as CborMap
import co.nstant.`in`.cbor.model.UnsignedInteger
import com.squareup.moshi.Moshi
import com.tangem.common.extensions.hexToBytes
import com.tangem.data.staking.verification.model.EvmStakingTx
import com.tangem.data.staking.verification.model.TronStakingRawTx
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.staking.verification.StakingTransactionRecognizer
import java.math.BigInteger

internal class DefaultStakingTransactionRecognizer : StakingTransactionRecognizer {

    private val tronAdapter by lazy {
        Moshi.Builder().build().adapter(TronStakingRawTx::class.java)
    }

    private val evmAdapter by lazy {
        Moshi.Builder().build().adapter(EvmStakingTx::class.java)
    }

    override fun isRecognizedStakingTransaction(network: NetworkType, unsignedTransaction: String): Boolean {
        return runCatching {
            when (network) {
                NetworkType.TRON -> isTronStaking(unsignedTransaction)
                NetworkType.COSMOS -> isCosmosStaking(unsignedTransaction)
                NetworkType.CARDANO -> isCardanoStaking(unsignedTransaction)
                NetworkType.SOLANA -> isSolanaStaking(unsignedTransaction)
                NetworkType.POLYGON -> isPolygonStaking(unsignedTransaction)
                NetworkType.BINANCE -> isEvmStakingTo(unsignedTransaction, BSC_STAKEHUB_CONTRACT)
                else -> false
            }
        }.getOrDefault(false)
    }

    // Fail-closed: a Tron tx is staking only if it has at least one contract and EVERY contract is a
    // staking operation, so a rogue contract can't ride along with a staking one in a bundled tx.
    private fun isTronStaking(unsignedTransaction: String): Boolean {
        val contracts = tronAdapter.fromJson(unsignedTransaction)?.rawData?.contract
        return !contracts.isNullOrEmpty() && contracts.all { it.type in TRON_STAKING_CONTRACT_TYPES }
    }

    // No Cosmos protobuf in blockchain-sdk: detect the staking message by its type URL, which is
    // serialized as plain ASCII. ISO-8859-1 preserves every byte 1:1 so the ASCII marker is findable.
    private fun isCosmosStaking(unsignedTransaction: String): Boolean {
        val decoded = String(unsignedTransaction.hexToBytes(), Charsets.ISO_8859_1)
        return decoded.contains(COSMOS_STAKING_TYPE_URL_PREFIX)
    }

    private fun isCardanoStaking(unsignedTransaction: String): Boolean {
        val items = CborDecoder.decode(unsignedTransaction.hexToBytes())
        val body = (items.firstOrNull() as? CborArray)?.dataItems?.firstOrNull() as? CborMap ?: return false
        return body.keys.any { key ->
            key is UnsignedInteger &&
                (key.value == CARDANO_CERTIFICATES_KEY || key.value == CARDANO_WITHDRAWALS_KEY)
        }
    }

    private fun isSolanaStaking(unsignedTransaction: String): Boolean =
        unsignedTransaction.lowercase().contains(SOLANA_STAKE_PROGRAM_HEX)

    private fun isEvmStakingTo(unsignedTransaction: String, contractAddress: String): Boolean {
        val to = evmAdapter.fromJson(unsignedTransaction)?.to ?: return false
        return to.equals(contractAddress, ignoreCase = true)
    }

    // Polygon staking is either a direct call to the StakeKit contract, or an ERC-20 approve of the
    // POL token whose spender is that same StakeKit contract (the pre-stake allowance transaction).
    private fun isPolygonStaking(unsignedTransaction: String): Boolean {
        val tx = evmAdapter.fromJson(unsignedTransaction) ?: return false
        val to = tx.to ?: return false
        return when {
            to.equals(POLYGON_STAKEKIT_CONTRACT, ignoreCase = true) -> true
            to.equals(POL_TOKEN_CONTRACT, ignoreCase = true) -> isApproveTo(tx.data, POLYGON_STAKEKIT_CONTRACT)
            else -> false
        }
    }

    // Checks that calldata is an ERC-20 approve(spender, amount) whose spender equals [spender].
    // Layout: 4-byte methodId | 32-byte spender (left-padded address) | 32-byte amount.
    private fun isApproveTo(data: String?, spender: String): Boolean {
        val hex = (data ?: return false).removePrefix("0x").removePrefix("0X")
        if (hex.length < APPROVE_DATA_MIN_HEX_LENGTH) return false
        if (!hex.startsWith(ERC20_APPROVE_METHOD_ID, ignoreCase = true)) return false
        val spenderWord = hex.substring(METHOD_ID_HEX_LENGTH, METHOD_ID_HEX_LENGTH + WORD_HEX_LENGTH)
        val spenderAddress = "0x" + spenderWord.takeLast(ADDRESS_HEX_LENGTH)
        return spenderAddress.equals(spender, ignoreCase = true)
    }

    private companion object {
        val TRON_STAKING_CONTRACT_TYPES = setOf(
            "FreezeBalanceV2Contract",
            "UnfreezeBalanceV2Contract",
            "CancelAllUnfreezeV2Contract",
            "DelegateResourceContract",
            "WithdrawExpireUnfreezeContract",
            "VoteWitnessContract",
        )
        const val COSMOS_STAKING_TYPE_URL_PREFIX = "/cosmos.staking."
        val CARDANO_CERTIFICATES_KEY: BigInteger = BigInteger.valueOf(4)
        val CARDANO_WITHDRAWALS_KEY: BigInteger = BigInteger.valueOf(5)
        const val SOLANA_STAKE_PROGRAM_HEX = "06a1d8179137542a983437bdfe2a7ab2557f535c8a78722b68a49dc000000000"
        const val POLYGON_STAKEKIT_CONTRACT = "0x467585AaEa860F9D8B3B43bb994E4Da8A93788a7"
        const val POL_TOKEN_CONTRACT = "0x455e53CBB86018Ac2B8092FdCd39d8444aFFC3F6"
        const val BSC_STAKEHUB_CONTRACT = "0x0000000000000000000000000000000000002002"
        const val ERC20_APPROVE_METHOD_ID = "095ea7b3"
        const val METHOD_ID_HEX_LENGTH = 8
        const val WORD_HEX_LENGTH = 64
        const val ADDRESS_HEX_LENGTH = 40
        const val APPROVE_DATA_MIN_HEX_LENGTH = METHOD_ID_HEX_LENGTH + WORD_HEX_LENGTH
    }
}