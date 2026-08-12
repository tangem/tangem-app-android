package com.tangem.features.jointaccount.creation.config.state.transformers

import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.creation.config.state.transformers.converter.WalletItemConverter
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class UpdateWalletsTransformer(
    private val walletsInfo: WalletsInfo,
    private val intents: Intents,
) : Transformer<JointAccountConfigUM> {

    override fun transform(prevState: JointAccountConfigUM): JointAccountConfigUM {
        val items = WalletItemConverter(
            selectedWalletId = walletsInfo.selectedWalletId,
            balances = walletsInfo.balances,
            images = walletsInfo.images,
            appCurrency = walletsInfo.appCurrency,
            isBalanceHidden = walletsInfo.isBalanceHidden,
            onClick = intents.onWalletSelect,
        ).convertList(input = walletsInfo.wallets)

        val selectedName = walletsInfo.wallets.firstOrNull { it.walletId == walletsInfo.selectedWalletId }?.name

        return prevState.copy(
            // A single wallet leaves nothing to choose, so the row is not shown at all
            wallet = if (items.size > 1 && selectedName != null) {
                JointAccountConfigUM.WalletUM(name = selectedName, onClick = intents.onWalletRowClick)
            } else {
                null
            },
            chooseWallet = if (walletsInfo.isSheetShown) {
                JointAccountConfigUM.ChooseWalletUM(
                    wallets = items.toImmutableList(),
                    onDismiss = intents.onChooseWalletDismiss,
                )
            } else {
                null
            },
        )
    }

    data class WalletsInfo(
        val wallets: List<UserWallet>,
        val selectedWalletId: UserWalletId,
        val isSheetShown: Boolean,
        val balances: Map<UserWalletId, TotalFiatBalance>,
        val images: Map<UserWalletId, UserWalletItemUM.ImageState>,
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
    )

    data class Intents(
        val onWalletSelect: (UserWalletId) -> Unit,
        val onWalletRowClick: () -> Unit,
        val onChooseWalletDismiss: () -> Unit,
    )
}