package com.tangem.feature.wallet.presentation.wallet.state.transformers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDropDownItems
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.child.wallet.model.intents.WalletCardClickIntents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class SetWalletCardDropDownItemsTransformer(
    private val dropdownEnabled: Boolean,
    private val clickIntents: WalletCardClickIntents,
) : WalletScreenStateTransformer {
    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(wallets = prevState.wallets.map(::transformWalletState).toImmutableList())
    }

    private fun transformWalletState(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> prevState.copy(
                walletCardState = prevState.walletCardState.copySealed(
                    dropDownItems = constructDropDownItems(prevState.walletCardState.id),
                ),
            )
            is WalletState.SingleCurrency.Content -> prevState.copy(
                walletCardState = prevState.walletCardState.copySealed(
                    dropDownItems = constructDropDownItems(prevState.walletCardState.id),
                ),
            )
            is WalletState.Visa.Content -> prevState.copy(
                walletCardState = prevState.walletCardState.copySealed(
                    dropDownItems = constructDropDownItems(prevState.walletCardState.id),
                ),
            )
            is WalletState.MultiCurrency.Locked -> prevState.copy(
                walletCardState = prevState.walletCardState.copySealed(
                    dropDownItems = constructDropDownItems(prevState.walletCardState.id),
                ),
            )
            is WalletState.SingleCurrency.Locked -> prevState.copy(
                walletCardState = prevState.walletCardState.copySealed(
                    dropDownItems = constructDropDownItems(prevState.walletCardState.id),
                ),
            )
            is WalletState.Visa.Locked -> prevState.copy(
                walletCardState = prevState.walletCardState.copySealed(
                    dropDownItems = constructDropDownItems(prevState.walletCardState.id),
                ),
            )
            is WalletState.Visa.AccessTokenLocked -> prevState.copy(
                walletCardState = prevState.walletCardState.copySealed(
                    dropDownItems = constructDropDownItems(prevState.walletCardState.id),
                ),
            )
        }
    }

    private fun constructDropDownItems(userWalletId: UserWalletId): ImmutableList<WalletDropDownItems> {
        return if (dropdownEnabled) {
            persistentListOf(
                WalletDropDownItems(
                    text = resourceReference(id = R.string.common_rename),
                    icon = Icons.Outlined.Edit,
                    onClick = { clickIntents.onRenameBeforeConfirmationClick(userWalletId) },
                ),
                WalletDropDownItems(
                    text = resourceReference(id = R.string.common_delete),
                    icon = Icons.Outlined.Delete,
                    onClick = { clickIntents.onDeleteBeforeConfirmationClick(userWalletId) },
                ),
            )
        } else {
            persistentListOf()
        }
    }
}