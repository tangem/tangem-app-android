package com.tangem.common.ui.userwallet.converter

import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.util.getCardsCount
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.utils.converter.Converter

/**
 * Converter from [UserWallet] to [UserWalletItemUM]
 *
 * @property onClick         lambda be invoked when item is clicked
 * @property appCurrency     selected app currency
 * @property balance         wallet balance
 * @property isBalanceHidden wallet balance is hidden
 *
[REDACTED_AUTHOR]
 */
class UserWalletItemUMConverter(
    private val onClick: (UserWalletId) -> Unit,
    private val appCurrency: AppCurrency? = null,
    private val balance: TotalFiatBalance? = null,
    private val isBalanceHidden: Boolean = false,
    private val isAuthMode: Boolean = false,
    private val endIcon: UserWalletItemUM.EndIcon = UserWalletItemUM.EndIcon.None,
    artwork: UserWalletItemUM.ImageState? = null,
) : Converter<UserWallet, UserWalletItemUM> {

    private val artwork = artwork ?: UserWalletItemUM.ImageState.Loading

    override fun convert(value: UserWallet): UserWalletItemUM {
        return with(value) {
            UserWalletItemUM(
                id = walletId,
                name = stringReference(name),
                information = getInfo(userWallet = this),
                balance = getBalanceInfo(userWallet = this),
                isEnabled = isEnabled(userWallet = this),
                endIcon = endIcon,
                onClick = { onClick(value.walletId) },
                imageState = artwork,
                label = getLabelOrNull(userWallet = this),
            )
        }
    }

    private fun isEnabled(userWallet: UserWallet): Boolean {
        return isAuthMode || userWallet.isLocked.not()
    }

    private fun getLabelOrNull(userWallet: UserWallet): LabelUM? {
        return if (isAuthMode.not() && userWallet is UserWallet.Hot && !userWallet.backedUp) {
            LabelUM(
                text = resourceReference(R.string.hw_backup_no_backup),
                style = LabelStyle.WARNING,
            )
        } else {
            null
        }
    }

    private fun getInfo(userWallet: UserWallet): UserWalletItemUM.Information.Loaded {
        val text = when (userWallet) {
            is UserWallet.Cold -> {
                val cardCount = userWallet.getCardsCount() ?: 1
                TextReference.PluralRes(
                    id = R.plurals.card_label_card_count,
                    count = cardCount,
                    formatArgs = wrappedList(cardCount),
                )
            }
            is UserWallet.Hot -> {
                TextReference.Res(R.string.hw_mobile_wallet)
            }
        }
        return UserWalletItemUM.Information.Loaded(text)
    }

    private fun getBalanceInfo(userWallet: UserWallet): UserWalletItemUM.Balance {
        return when {
            userWallet.isLocked -> UserWalletItemUM.Balance.Locked
            isAuthMode -> UserWalletItemUM.Balance.NotShowing
            isBalanceHidden -> UserWalletItemUM.Balance.Hidden
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
                                isFlickering = balance.source == StatusSource.CACHE,
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