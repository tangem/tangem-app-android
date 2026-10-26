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

class AccountRowStateConverter(private val appCurrency: AppCurrency) {

    fun convert(value: AccountStatus.CryptoPortfolio, onClick: (() -> Unit)?): TangemAccountRow.State {
        val account = value.account
        val id = account.accountId.value
        val title = account.accountName.toUM().value
        val label = tokensCount(account.tokensCount)
        val description = membersCount(value)

        return when (val balance = value.tokenList.totalFiatBalance) {
            TotalFiatBalance.Loading -> TangemAccountRow.State.Shimmer(id = id)
            is TotalFiatBalance.Loaded -> TangemAccountRow.State.Content(
                id = id,
                title = title,
                label = label,
                description = description,
                balance = stringReference(
                    balance.amount.format {
                        fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                    },
                ),
                priceChange = value.priceChangeLce.getOrNull()?.value?.toPriceChangeState(),
                shouldShowUpdateWarning = balance.source == StatusSource.ONLY_CACHE,
                isBalanceFlickering = balance.source == StatusSource.CACHE,
                onClick = onClick,
            )
            TotalFiatBalance.Failed -> if (value.hasUnreachableCurrencyOnly()) {
                TangemAccountRow.State.Unreachable(
                    id = id,
                    title = title,
                    badge = resourceReference(R.string.common_unreachable),
                    label = label,
                    description = description,
                    onClick = onClick,
                )
            } else {
                TangemAccountRow.State.BalanceError(
                    id = id,
                    title = title,
                    label = label,
                    description = description,
                    onClick = onClick,
                )
            }
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
}