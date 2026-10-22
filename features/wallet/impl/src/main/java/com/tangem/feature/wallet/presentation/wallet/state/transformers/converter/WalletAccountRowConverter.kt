package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.avatar.TangemAvatar
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
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2
import kotlinx.collections.immutable.ImmutableList

internal class WalletAccountRowConverter(
    private val appCurrency: AppCurrency,
    private val expandedAccounts: Set<AccountId>,
    private val onAccountCollapseClick: (account: Account) -> Unit,
    private val onAccountExpandClick: (account: Account) -> Unit,
    private val onManageTokensClick: (accountId: AccountId) -> Unit,
) {

    fun convert(
        value: AccountStatus.CryptoPortfolio,
        tokenList: ImmutableList<TokensListItemUM2>,
    ): TokensListItemUM2.AccountV2 {
        val account = value.account
        val isExpanded = expandedAccounts.contains(account.accountId)

        return TokensListItemUM2.AccountV2(
            state = toState(value = value, isExpanded = isExpanded),
            avatar = toAvatar(account),
            isExpanded = isExpanded,
            shouldShowCompactBalance = account.tokensCount > 0,
            onEmptyClick = { onManageTokensClick(account.accountId) },
            tokenList = tokenList,
        )
    }

    private fun toState(value: AccountStatus.CryptoPortfolio, isExpanded: Boolean): TangemAccountRow.State {
        val account = value.account
        val id = account.accountId.value
        val title = account.accountName.toUM().value
        val label = tokensCount(account.tokensCount)
        val description = membersCount(value)
        val onClick = {
            if (isExpanded) onAccountCollapseClick(account) else onAccountExpandClick(account)
        }

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

    // TODO([REDACTED_TASK_KEY]): render the initial for Letter accounts and the pending-operation indicator
    private fun toAvatar(account: Account.CryptoPortfolio): TangemAvatar.UiState {
        return TangemAvatar.UiState.Account(
            imageVector = account.icon.value.toAvatarIcon(),
            color = account.icon.color.toFillColor(),
        )
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