package com.tangem.features.details.utils

import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.util.getCardsCount
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.details.impl.R
import com.tangem.utils.StringsSigns.STARS
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun List<UserWallet>.toUiModels(
    onClick: (UserWalletId) -> Unit,
    appCurrency: AppCurrency? = null,
    balances: Map<UserWalletId, TotalFiatBalance> = emptyMap(),
    isLoading: Boolean = true,
    isBalancesHidden: Boolean = false,
): ImmutableList<UserWalletItemUM> = this.map { model ->
    val balance = balances[model.walletId]

    model.toUiModel(
        balance = balance,
        appCurrency = appCurrency,
        isLoading = isLoading,
        isBalanceHidden = isBalancesHidden,
        onClick = { onClick(model.walletId) },
    )
}.toImmutableList()

private fun UserWallet.toUiModel(
    balance: TotalFiatBalance?,
    appCurrency: AppCurrency?,
    isLoading: Boolean,
    isBalanceHidden: Boolean,
    onClick: () -> Unit,
): UserWalletItemUM = UserWalletItemUM(
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
    onClick = onClick,
)

private fun UserWallet.getInfo(
    appCurrency: AppCurrency?,
    balance: TotalFiatBalance?,
    isBalanceHidden: Boolean,
    isLoading: Boolean,
): TextReference {
    val dividerRef = stringReference(value = " • ")

    val cardCount = getCardsCount() ?: 1
    val cardCountRef = TextReference.PluralRes(
        id = R.plurals.card_label_card_count,
        count = cardCount,
        formatArgs = wrappedList(cardCount),
    )

    return when {
        isBalanceHidden -> combinedReference(cardCountRef, dividerRef, stringReference(STARS))
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