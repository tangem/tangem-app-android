package com.tangem.tap.domain.walletconnect2.domain.mapper

import com.tangem.blockchain.blockchains.solana.solanaj.core.SolanaTransaction
import com.tangem.tap.domain.walletconnect2.domain.models.solana.SolanaTransactionRequest
import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction

internal fun SolanaTransactionRequest.mapToTransaction(): SolanaTransaction {
    val from = PublicKey(feePayer)
    val instructions = instructions.map(SolanaTransactionRequest.Instruction::mapToInstruction)

    return SolanaTransaction(from)
        .apply {
            instructions.forEach(::addInstruction)
            setRecentBlockHash(recentBlockhash)
        }
}

private fun SolanaTransactionRequest.Instruction.mapToInstruction() = TransactionInstruction(
    /* programId = */ PublicKey(programId),
    /* keys = */ keys.map(SolanaTransactionRequest.Key::mapToAccountMeta),
    /* data = */ data.toByteArray(),
)

private fun SolanaTransactionRequest.Key.mapToAccountMeta() = AccountMeta(
    /* publicKey = */ PublicKey(publicKey),
    /* isSigner = */ isSigner,
    /* isWritable = */ isWritable,
)