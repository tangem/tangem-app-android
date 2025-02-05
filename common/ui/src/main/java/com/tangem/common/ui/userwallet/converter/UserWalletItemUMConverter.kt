package com.tangem.common.ui.userwallet.converter

import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.util.getCardsCount
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.converter.Converter

/**
 * Converter from [UserWallet] to [UserWalletItemUM]
 *
 * @property onClick         lambda be invoked when item is clicked
 * @property appCurrency     selected app currency
 * @property balance         wallet balance
 * @property isLoading       wallet loading state
 * @property isBalanceHidden wallet balance is hidden
 *
[REDACTED_AUTHOR]
 */
class UserWalletItemUMConverter(
    private val onClick: (UserWalletId) -> Unit,
    private val appCurrency: AppCurrency? = null,
    private val balance: TotalFiatBalance? = null,
    private val isLoading: Boolean = true,
    private val isBalanceHidden: Boolean = false,
    private val endIcon: UserWalletItemUM.EndIcon = UserWalletItemUM.EndIcon.None,
) : Converter<UserWallet, UserWalletItemUM> {

    override fun convert(value: UserWallet): UserWalletItemUM {
        return with(value) {
            UserWalletItemUM(
                id = walletId,
                name = stringReference(name),
                information = getInfo(userWallet = this),
                balance = getBalanceInfo(userWallet = this),
                imageUrl = artworkUrl,
                isEnabled = !isLocked,
                endIcon = endIcon,
                onClick = { onClick(value.walletId) },
            )
        }
    }

    private fun getInfo(userWallet: UserWallet): TextReference {
        val cardCount = userWallet.getCardsCount() ?: 1
        return TextReference.PluralRes(
            id = R.plurals.card_label_card_count,
            count = cardCount,
            formatArgs = wrappedList(cardCount),
        )
    }

    private fun getBalanceInfo(userWallet: UserWallet): UserWalletItemUM.Balance {
        return when {
            isBalanceHidden -> UserWalletItemUM.Balance.Hidden
            userWallet.isLocked -> UserWalletItemUM.Balance.Locked
            balance == null -> UserWalletItemUM.Balance.Loading
            else -> {
                when (balance) {
                    is TotalFiatBalance.Loading -> UserWalletItemUM.Balance.Loading
                    is TotalFiatBalance.Failed -> UserWalletItemUM.Balance.Failed
                    is TotalFiatBalance.Loaded -> {
                        if (appCurrency != null) {
                            val formattedAmount = balance.amount.format {
                                fiat(appCurrency.code, appCurrency.symbol)
                            }

                            UserWalletItemUM.Balance.Loaded(
                                value = formattedAmount,
                                isFlickering = isLoading,
                            )
                        } else {
                            UserWalletItemUM.Balance.Failed
                        }
                    }
                }
            }
        }
    }
}