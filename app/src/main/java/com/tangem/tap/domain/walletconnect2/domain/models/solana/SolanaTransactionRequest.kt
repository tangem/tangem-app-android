package com.tangem.tap.domain.walletconnect2.domain.models.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.Transaction
import com.tangem.common.extensions.toByteArray
import com.tangem.tap.domain.walletconnect2.domain.WcRequestData
import kotlinx.serialization.Serializable
import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction

@Serializable
data class SolanaTransactionRequest(
    val feePayer: String,
    val recentBlockhash: String,
    val instructions: List<Instruction>,
) : WcRequestData

@Serializable
data class Instruction(
    val programId: String,
    val data: List<Int>,
    val keys: List<Key>,
)

@Serializable
data class Key(
    val isSigner: Boolean,
    val isWritable: Boolean,
    val pubkey: String,
)

fun Key.toSolanaJKey(): AccountMeta {
    return AccountMeta(PublicKey(this.pubkey), this.isSigner, this.isWritable)
}

fun Instruction.toSolanaJInstruction(): TransactionInstruction {
    val data = this.data.map { it.toByteArray(1) }.fold(byteArrayOf()) { r, t -> r + t }
    return TransactionInstruction(
        PublicKey(this.programId),
        keys.map { it.toSolanaJKey() },
        data,
    )
}

fun SolanaTransactionRequest.toSolanaJTx(): Transaction {
    val from = PublicKey(this.feePayer)
    val instructions = this.instructions.map { it.toSolanaJInstruction() }

    return Transaction(from)
        .apply {
            instructions.forEach { addInstruction(it) }
            setRecentBlockHash(this@toSolanaJTx.recentBlockhash)
        }
}