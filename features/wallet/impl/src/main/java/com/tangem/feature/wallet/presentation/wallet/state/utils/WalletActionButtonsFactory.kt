package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletActionButtons
import com.tangem.utils.extensions.addIf
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal fun createWalletActionButtons(
    userWallet: UserWallet,
    clickIntents: WalletClickIntents,
    isAddFundsEnabled: Boolean,
    isSwapEnabled: Boolean,
    isTransferEnabled: Boolean,
): PersistentList<TangemButtonUM> {
    val isSingleWalletWithToken = userWallet is UserWallet.Cold &&
        userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()

    return buildList {
        add(
            WalletActionButtons.AddFunds(
                isEnabled = isAddFundsEnabled,
                onClick = { clickIntents.onAddFundsClick(userWalletId = userWallet.walletId) },
            ).buttonUM,
        )
        addIf(
            condition = !userWallet.isSingleWallet() && !isSingleWalletWithToken,
            element = WalletActionButtons.Swap(
                isEnabled = isSwapEnabled,
                onClick = { clickIntents.onMultiWalletSwapClick(userWalletId = userWallet.walletId) },
            ).buttonUM,
        )
        add(
            WalletActionButtons.Transfer(
                isEnabled = isTransferEnabled,
                onClick = { clickIntents.onTransferClick(userWalletId = userWallet.walletId) },
            ).buttonUM,
        )
    }.toPersistentList()
}