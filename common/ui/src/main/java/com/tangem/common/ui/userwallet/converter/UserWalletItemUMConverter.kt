package com.tangem.common.ui.userwallet.converter

import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.util.getCardsCount
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.StringsSigns.DOT
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
                information = getInfo(
                    appCurrency = appCurrency,
                    balance = balance,
                    isBalanceHidden = isBalanceHidden,
                    isLoading = isLoading,
                ),
                imageUrl = artworkUrl,
                isEnabled = !isLocked,
                endIcon = endIcon,
                onClick = { onClick(value.walletId) },
            )
        }
    }

    private fun UserWallet.getInfo(
        appCurrency: AppCurrency?,
        balance: TotalFiatBalance?,
        isBalanceHidden: Boolean,
        isLoading: Boolean,
    ): TextReference {
        val dividerRef = stringReference(value = " $DOT ")

        val cardCount = getCardsCount() ?: 1
        val cardCountRef = TextReference.PluralRes(
            id = R.plurals.card_label_card_count,
            count = cardCount,
            formatArgs = wrappedList(cardCount),
        )

        return when {
            isBalanceHidden -> combinedReference(cardCountRef, dividerRef, TextReference.STARS)
            isLocked -> combinedReference(cardCountRef, dividerRef, resourceReference(R.string.common_locked))
            isLoading -> cardCountRef
            else -> getBalanceInfo(balance, appCurrency, cardCountRef, dividerRef)
        }
    }

    private fun getBalanceInfo(
        balance: TotalFiatBalance?,
        appCurrency: AppCurrency?,
        cardCountRef: TextReference,
        dividerRef: TextReference,
    ): TextReference {
        val amount = when (balance) {
            is TotalFiatBalance.Loaded -> balance.amount
            is TotalFiatBalance.Failed,
            is TotalFiatBalance.Loading,
            null,
            -> null
        }

        return if (amount != null && appCurrency != null) {
            val formattedAmount = BigDecimalFormatter.formatFiatAmount(
                fiatAmount = amount,
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
            val amountRef = stringReference(formattedAmount)
            combinedReference(cardCountRef, dividerRef, amountRef)
        } else {
            combinedReference(cardCountRef, dividerRef, stringReference(BigDecimalFormatter.EMPTY_BALANCE_SIGN))
        }
    }
}