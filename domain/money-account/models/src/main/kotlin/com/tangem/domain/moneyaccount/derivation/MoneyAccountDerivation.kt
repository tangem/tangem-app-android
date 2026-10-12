package com.tangem.domain.moneyaccount.derivation

import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath

val MONEY_ACCOUNT_DERIVATION_PATH: DerivationPath = DerivationPath("m/44'/60'/999998'/0/0")

private const val RESERVED_NODE_POSITION = 2

private val MONEY_ACCOUNT_RESERVED_NODE: DerivationNode by lazy {
    MONEY_ACCOUNT_DERIVATION_PATH.nodes[RESERVED_NODE_POSITION]
}

fun isMoneyAccountReservedPath(rawPath: String): Boolean {
    val path = runCatching { DerivationPath(rawPath) }.getOrNull() ?: return false
    return path.nodes.getOrNull(RESERVED_NODE_POSITION) == MONEY_ACCOUNT_RESERVED_NODE
}