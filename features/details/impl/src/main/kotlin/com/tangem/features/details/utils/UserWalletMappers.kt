package com.tangem.features.details.utils

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.details.entity.UserWalletListUM.UserWalletUM
import com.tangem.features.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun List<UserWallet>.toUiModels(
    onClick: (UserWalletId) -> Unit,
    appCurrency: AppCurrency? = null,
    balances: Map<UserWalletId, TotalFiatBalance> = emptyMap(),
): ImmutableList<UserWalletUM> = this.map { model ->
    val balance = balances[model.walletId]
    model.mapToUiModel(
        balance = balance,
        appCurrency = appCurrency,
        onClick = { onClick(model.walletId) },
    )
}.toImmutableList()

private fun UserWallet.mapToUiModel(
    balance: TotalFiatBalance?,
    appCurrency: AppCurrency?,
    onClick: () -> Unit,
): UserWalletUM = UserWalletUM(
    id = walletId,
    name = name,
    information = getInfo(appCurrency, balance),
    imageResId = resolveImage(),
    onClick = onClick,
)

private fun UserWallet.getInfo(appCurrency: AppCurrency?, balance: TotalFiatBalance?): TextReference {
    val cardCount = getCardCount()
    val cardCountRef = TextReference.PluralRes(
        id = R.plurals.card_label_card_count,
        count = cardCount,
        formatArgs = wrappedList(cardCount),
    )
    val amount = when (balance) {
        is TotalFiatBalance.Loaded -> balance.amount.takeIf { balance.isAllAmountsSummarized }
        is TotalFiatBalance.Failed,
        is TotalFiatBalance.Loading,
        null,
        -> null
    }

    return if (amount != null && appCurrency != null) {
        val divider = stringReference(value = " • ")
        val formattedAmount = BigDecimalFormatter.formatFiatAmount(
            fiatAmount = amount,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
        val amountRef = stringReference(formattedAmount)
        TextReference.Combined(wrappedList(cardCountRef, divider, amountRef))
    } else {
        cardCountRef
    }
}

private fun UserWallet.getCardCount() = when (val status = scanResponse.card.backupStatus) {
    is CardDTO.BackupStatus.Active -> status.cardCount.inc()
    is CardDTO.BackupStatus.CardLinked -> status.cardCount.inc()
    is CardDTO.BackupStatus.NoBackup,
    null,
    -> 1
}

@DrawableRes
private fun UserWallet.resolveImage(): Int {
    // TODO: Implement image resolving [REDACTED_JIRA]
    return R.drawable.ill_card_wallet_2_211_343
}