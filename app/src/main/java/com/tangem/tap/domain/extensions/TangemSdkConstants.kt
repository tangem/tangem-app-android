package com.tangem.tap.domain.extensions

import com.tangem.commands.wallet.WalletIndex
import com.tangem.common.TangemSdkConstants

fun TangemSdkConstants.Companion.getDefaultWalletIndex(): WalletIndex {
    return WalletIndex.Index(oldCardDefaultWalletIndex)
}