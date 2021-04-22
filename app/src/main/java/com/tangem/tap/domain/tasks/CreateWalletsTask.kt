package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.EllipticCurve
import com.tangem.commands.wallet.CreateWalletCommand
import com.tangem.commands.wallet.WalletConfig
import com.tangem.common.CompletionResult
import com.tangem.tasks.PreflightReadSettings
import com.tangem.tasks.PreflightReadTask

class CreateWalletsTask(wallets: List<WalletConfig> = emptyList()) : CardSessionRunnable<Card> {
    override val requiresPin2 = false

    val wallets = if (wallets.isEmpty()) {
        listOf(
                WalletConfig(null, null, EllipticCurve.Secp256k1, null),
                WalletConfig(null, null, EllipticCurve.Ed25519, null),
                WalletConfig(null, null, EllipticCurve.Secp256r1, null),
        )
    } else {
        wallets
    }

    var index = 0

    override fun run(session: CardSession, callback: (result: CompletionResult<Card>) -> Unit) {
        val walletConfig = wallets[index]
        createWallet(walletConfig, session, callback)
    }

    private fun createWallet(
            walletConfig: WalletConfig, session: CardSession,
            callback: (result: CompletionResult<Card>) -> Unit
    ) {

        CreateWalletCommand(
                walletConfig, walletIndexValue = walletConfig.curveId!!.toWalletIndex()
        ).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (index == wallets.lastIndex) {
                        PreflightReadTask(PreflightReadSettings.FullCardRead).run(session, callback)
                        return@run
                    }
                    index += 1
                    createWallet(wallets[index], session, callback)
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }
}

private fun EllipticCurve.toWalletIndex(): Int {
    return when (this) {
        EllipticCurve.Secp256k1 -> 0
        EllipticCurve.Ed25519 -> 1
        EllipticCurve.Secp256r1 -> 2
    }
}