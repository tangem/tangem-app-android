package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.feature.wallet.child.wallet.model.intents.WalletContentClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayGroupUM
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import java.math.BigDecimal

internal class TangemPayGroupConverter(
    private val expandedAccounts: Set<AccountId>,
    private val clickIntents: WalletContentClickIntents,
) {

    fun convert(
        paymentStatus: AccountStatus.Payment?,
        moneyStatus: AccountStatus.Money?,
        tangemPayMainUM: TangemPayMainUM,
        moneyAccountMainUM: MoneyAccountMainUM,
    ): TangemPayGroupUM {
        if (paymentStatus == null || moneyStatus == null) {
            return TangemPayGroupUM.Hidden
        }
        if (tangemPayMainUM is TangemPayMainUM.Empty || moneyAccountMainUM is MoneyAccountMainUM.Empty) {
            return TangemPayGroupUM.Hidden
        }

        val groupAccountId = paymentStatus.account.accountId
        val isExpanded = expandedAccounts.contains(groupAccountId)
        val paymentBalance = paymentStatus.value.availableBalance()
        val moneyBalance = moneyStatus.value.availableBalance()
        val balances = listOfNotNull(paymentBalance, moneyBalance)
        val issuedAccountsCount = listOf(paymentStatus.value.isIssued(), moneyStatus.value.isIssued()).count { it }

        return TangemPayGroupUM.Content(
            id = "TangemPayGroup",
            title = resourceReference(R.string.tangempay_main_group_title),
            label = pluralReference(
                id = R.plurals.tangempay_main_group_subtitle,
                count = issuedAccountsCount,
                formatArgs = wrappedList(issuedAccountsCount),
            ),
            balance = if (balances.isEmpty()) {
                stringReference(DASH_SIGN)
            } else {
                formatUsd(balances.fold(BigDecimal.ZERO, BigDecimal::add))
            },
            balanceCaption = stringReference(USD_CURRENCY_CODE),
            isBalanceFlickering = paymentStatus.value.isRefreshing() || moneyStatus.value.isRefreshing(),
            isExpanded = isExpanded,
            onClick = { clickIntents.onTangemPayGroupClick(accountId = groupAccountId, isExpanded = isExpanded) },
        )
    }

    private fun formatUsd(amount: BigDecimal): TextReference {
        val currency = getJavaCurrencyByCode(USD_CURRENCY_CODE)
        return stringReference(
            amount.format {
                fiat(
                    fiatCurrencyCode = currency.currencyCode,
                    fiatCurrencySymbol = currency.symbol,
                )
            },
        )
    }

    private fun PaymentAccountStatusValue.availableBalance(): BigDecimal? = when (this) {
        is PaymentAccountStatusValue.Loaded -> balance?.fiatBalance?.availableBalance
        is PaymentAccountStatusValue.Deactivated -> balance?.fiatBalance?.availableBalance
        else -> null
    }

    private fun MoneyAccountStatusValue.availableBalance(): BigDecimal? = when (this) {
        is MoneyAccountStatusValue.Account.Active -> balance?.cryptoAmount
        else -> null
    }

    private fun PaymentAccountStatusValue.isIssued(): Boolean =
        this is PaymentAccountStatusValue.Loaded || this is PaymentAccountStatusValue.Deactivated

    private fun MoneyAccountStatusValue.isIssued(): Boolean = this is MoneyAccountStatusValue.Account

    private fun PaymentAccountStatusValue.isRefreshing(): Boolean =
        this is PaymentAccountStatusValue.Loading || source == StatusSource.CACHE

    private fun MoneyAccountStatusValue.isRefreshing(): Boolean = source == StatusSource.CACHE

    private companion object {
        const val USD_CURRENCY_CODE = "USD"
    }
}