package com.tangem.common.ui.account

import com.tangem.common.ui.R
import com.tangem.common.ui.tokens.toPriceChangeState
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import java.math.BigDecimal

class AccountRowStateConverter(private val appCurrency: AppCurrency) {

    fun convert(value: AccountStatus.CryptoPortfolio, onClick: (() -> Unit)?): TangemAccountRow.State {
        val account = value.account
        return convert(
            header = Header(
                id = account.accountId.value,
                title = account.accountName.toUM().value,
                label = tokensCount(account.tokensCount),
                description = membersCount(value),
                onClick = onClick,
            ),
            balance = value.tokenList.totalFiatBalance,
            priceChange = value.priceChangeLce.getOrNull()?.value,
            isUnreachableOnly = { value.hasUnreachableCurrencyOnly() },
        )
    }

    fun convert(value: AccountStatus.Payment, tokensCount: Int, onClick: (() -> Unit)?): TangemAccountRow.State {
        val account = value.account
        return convert(
            header = Header(
                id = account.accountId.value,
                title = account.accountName.toUM().value,
                label = tokensCount(tokensCount),
                description = null,
                onClick = onClick,
            ),
            balance = value.value.totalFiatBalance,
            priceChange = null,
            isUnreachableOnly = { false },
        )
    }

    private fun convert(
        header: Header,
        balance: TotalFiatBalance,
        priceChange: BigDecimal?,
        isUnreachableOnly: () -> Boolean,
    ): TangemAccountRow.State = when (balance) {
        TotalFiatBalance.Loading -> TangemAccountRow.State.Shimmer(id = header.id)
        is TotalFiatBalance.Loaded -> TangemAccountRow.State.Content(
            id = header.id,
            title = header.title,
            label = header.label,
            description = header.description,
            balance = stringReference(
                balance.amount.format {
                    fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                },
            ),
            priceChange = priceChange?.toPriceChangeState(),
            shouldShowUpdateWarning = balance.source == StatusSource.ONLY_CACHE,
            isBalanceFlickering = balance.source == StatusSource.CACHE,
            onClick = header.onClick,
        )
        TotalFiatBalance.Failed -> if (isUnreachableOnly()) {
            TangemAccountRow.State.Unreachable(
                id = header.id,
                title = header.title,
                badge = resourceReference(R.string.common_unreachable),
                label = header.label,
                description = header.description,
                onClick = header.onClick,
            )
        } else {
            TangemAccountRow.State.BalanceError(
                id = header.id,
                title = header.title,
                label = header.label,
                description = header.description,
                onClick = header.onClick,
            )
        }
    }

    private fun AccountStatus.CryptoPortfolio.hasUnreachableCurrencyOnly(): Boolean {
        val statuses = tokenList.flattenCurrencies().map(CryptoCurrencyStatus::value)
        return statuses.none { it is CryptoCurrencyStatus.MissedDerivation } &&
            statuses.any { it is CryptoCurrencyStatus.Unreachable }
    }

    private fun tokensCount(count: Int): TextReference = pluralReference(
        id = R.plurals.common_tokens_count,
        count = count,
        formatArgs = wrappedList(count),
    )

    private fun membersCount(value: AccountStatus.CryptoPortfolio): TextReference? {
        val activeJoint = (value as? AccountStatus.Joint)?.value as? JointAccountStatusValue.Active ?: return null
        return pluralReference(
            id = R.plurals.joint_account_members_count,
            count = activeJoint.totalMembers,
            formatArgs = wrappedList(activeJoint.totalMembers),
        )
    }

    private class Header(
        val id: String,
        val title: TextReference,
        val label: TextReference,
        val description: TextReference?,
        val onClick: (() -> Unit)?,
    )
}