package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState

internal class WalletHiddenBalanceStateConverter {

    fun updateHiddenState(walletCardState: WalletCardState, hiddenBalance: Boolean): WalletCardState {
        return when {
            walletCardState is WalletCardState.Content && hiddenBalance -> {
                contentToHidden(walletCardState)
            }
            walletCardState is WalletCardState.HiddenContent && !hiddenBalance -> {
                hiddenToContent(walletCardState)
            }
            else -> walletCardState
        }
    }

    private fun contentToHidden(content: WalletCardState.Content): WalletCardState.HiddenContent {
        return WalletCardState.HiddenContent(
            id = content.id,
            title = content.title,
            additionalInfo = content.additionalInfo,
            imageResId = content.imageResId,
            onRenameClick = content.onRenameClick,
            onDeleteClick = content.onDeleteClick,
            balance = content.balance,
        )
    }

    private fun hiddenToContent(hiddenContent: WalletCardState.HiddenContent): WalletCardState.Content {
        return WalletCardState.Content(
            id = hiddenContent.id,
            title = hiddenContent.title,
            additionalInfo = hiddenContent.additionalInfo,
            imageResId = hiddenContent.imageResId,
            onRenameClick = hiddenContent.onRenameClick,
            onDeleteClick = hiddenContent.onDeleteClick,
            balance = hiddenContent.balance,
        )
    }
}