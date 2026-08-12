package com.tangem.common.ui.userwallet.picker.converter

import com.tangem.common.ui.userwallet.converter.UserWalletItemUMConverter
import com.tangem.common.ui.userwallet.picker.state.ChooseWalletUM
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.converter.Converter

/**
 * Converts a [UserWallet] into a row of the wallet picker.
 *
 * @property selectedWalletId wallet currently chosen
 * @property balances total fiat balance per wallet; a missing or unloaded entry renders as cards only
 * @property appCurrency currency the balance is formatted in
 * @property isBalanceHidden whether the user hid balances app-wide
 * @property onClick invoked with the tapped wallet
 */
class WalletItemConverter(
    private val selectedWalletId: UserWalletId,
    private val balances: Map<UserWalletId, TotalFiatBalance>,
    private val images: Map<UserWalletId, UserWalletItemUM.ImageState>,
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val onClick: (UserWalletId) -> Unit,
) : Converter<UserWallet, ChooseWalletUM.WalletItemUM> {

    override fun convert(value: UserWallet): ChooseWalletUM.WalletItemUM {
        val item = UserWalletItemUMConverter(
            onClick = onClick,
            appCurrency = appCurrency,
            balance = balances[value.walletId],
            isBalanceHidden = isBalanceHidden,
            artwork = images[value.walletId],
        ).convert(value)

        return ChooseWalletUM.WalletItemUM(
            id = item.id,
            name = item.name,
            image = item.imageState,
            info = buildInfo(item),
            isSelected = value.walletId == selectedWalletId,
            onClick = item.onClick,
        )
    }

    private fun buildInfo(item: UserWalletItemUM): TextReference {
        val cards = when (val information = item.information) {
            is UserWalletItemUM.Information.Loaded -> information.value
            UserWalletItemUM.Information.Failed,
            UserWalletItemUM.Information.Loading,
            -> TextReference.EMPTY
        }

        val balance = (item.balance as? UserWalletItemUM.Balance.Loaded)?.value ?: return cards

        return TextReference.Combined(
            refs = wrappedList(cards, stringReference(value = " $DOT "), stringReference(value = balance)),
        )
    }
}