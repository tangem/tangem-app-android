package com.tangem.domain.wallets.extension

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.card.common.util.hasDerivation
import com.tangem.domain.card.configs.Wallet2CardConfig
import com.tangem.domain.models.wallet.UserWallet
import kotlin.collections.first
import kotlin.collections.orEmpty

fun UserWallet.hasDerivation(blockchain: Blockchain, derivationPath: String): Boolean {
    return when (this) {
        is UserWallet.Cold -> scanResponse.hasDerivation(blockchain, derivationPath)
        is UserWallet.Hot -> {
            val primaryCurve = Wallet2CardConfig.primaryCurve(blockchain) // TODO [REDACTED_TASK_KEY]: handle hot wallet config
            val list = if (blockchain == Blockchain.Cardano) {
                listOf(
                    CardanoUtils.extendedDerivationPath(DerivationPath(derivationPath)),
                    DerivationPath(derivationPath),
                )
            } else {
                listOf(DerivationPath(derivationPath))
            }
            list.all { dp ->
                wallets.orEmpty().first { it.curve == primaryCurve }.derivedKeys.keys.any { it == dp }
            }
        }
    }
}