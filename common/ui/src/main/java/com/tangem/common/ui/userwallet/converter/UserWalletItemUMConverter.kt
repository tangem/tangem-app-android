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
import com.tangem.domain.models.ArtworkModel
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.tokens.model.TotalFiatBalance
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
    private val endIcon: UserWalletItemUM.EndIcon = UserWalletItemUM.EndIcon.None,
    private val artwork: ArtworkModel? = null,
) : Converter<UserWallet, UserWalletItemUM> {

    private val artworkUMConverter = ArtworkUMConverter()

    override fun convert(value: UserWallet): UserWalletItemUM {
        return with(value) {
            UserWalletItemUM(
                id = walletId,
                name = stringReference(name),
                information = getInfo(userWallet = this),
                balance = getBalanceInfo(userWallet = this),
                isEnabled = !isLocked,
                endIcon = endIcon,
                onClick = { onClick(value.walletId) },
                imageState = artwork?.let {
                    UserWalletItemUM.ImageState.Image(artworkUMConverter.convert(it))
                } ?: UserWalletItemUM.ImageState.Loading,
            )
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