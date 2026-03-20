package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAdditionalInfo
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM

internal class SetTokenSyncProgressTransformer(
    userWalletId: UserWalletId,
    private val progressPercent: Int,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                val updatedCardState = updateCardState(prevState.walletCardState)
                prevState.copy(walletCardState = updatedCardState)
            }
            else -> {
                prevState
            }
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return walletUM
    }

    private fun updateCardState(cardState: WalletCardState): WalletCardState {
        val additionalInfo = WalletAdditionalInfo(
            hideable = false,
            content = resourceReference(
                id = R.string.initial_wallet_sync_restore_progress,
                formatArgs = wrappedList(progressPercent),
            ),
            shouldShowProgress = true,
        )
        return when (cardState) {
            is WalletCardState.Loading -> {
                cardState.copy(additionalInfo = additionalInfo)
            }
            is WalletCardState.Content -> {
                cardState.copy(additionalInfo = additionalInfo)
            }
            else -> cardState
        }
    }
}