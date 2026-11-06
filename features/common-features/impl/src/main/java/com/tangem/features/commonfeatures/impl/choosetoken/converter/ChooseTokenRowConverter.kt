package com.tangem.features.commonfeatures.impl.choosetoken.converter

import com.tangem.common.ui.account.AccountRowStateConverter
import com.tangem.common.ui.account.toAvatarState
import com.tangem.common.ui.tokens.TokenRowStateConverter
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.features.commonfeatures.api.choosetoken.model.AccountRowUM
import com.tangem.features.commonfeatures.api.choosetoken.model.TokenRowItemUM
import com.tangem.features.commonfeatures.impl.R
import kotlinx.collections.immutable.toPersistentList

internal class ChooseTokenRowConverter(
    appCurrency: AppCurrency,
    private val onTokenClick: (AccountStatus, CryptoCurrencyStatus) -> Unit,
) {

    private val accountRowStateConverter = AccountRowStateConverter(appCurrency)
    private val tokenRowStateConverter = TokenRowStateConverter(appCurrency)

    fun accountRow(
        account: AccountStatus.CryptoPortfolio,
        displayed: AccountStatus.CryptoPortfolio,
        onClick: (() -> Unit)?,
    ): AccountRowUM = AccountRowUM(
        state = accountRowStateConverter.convert(value = displayed, onClick = onClick),
        avatar = displayed.account.toAvatarState(),
        tokens = displayed.tokenList.flattenCurrencies().map { tokenRow(account, it) }.toPersistentList(),
    )

    fun paymentRow(
        account: AccountStatus.Payment,
        currencies: List<CryptoCurrencyStatus>,
        onClick: (() -> Unit)?,
    ): AccountRowUM = AccountRowUM(
        state = accountRowStateConverter.convert(value = account, tokensCount = currencies.size, onClick = onClick),
        avatar = account.account.toAvatarState(),
        tokens = currencies.map { tokenRow(account, it) }.toPersistentList(),
    )

    fun tokenRows(tokenList: TokenList, account: AccountStatus): List<TokenRowItemUM> = when (tokenList) {
        TokenList.Empty -> emptyList()
        is TokenList.Ungrouped -> tokenList.currencies.map { TokenRowItemUM.Token(tokenRow(account, it)) }
        is TokenList.GroupedByNetwork -> tokenList.groups.flatMap { group ->
            val section = TokenRowItemUM.Section(
                id = group.network.hashCode().toString(),
                title = resourceReference(
                    id = R.string.wallet_network_group_title,
                    formatArgs = wrappedList(group.network.name),
                ),
            )
            listOf(section) + group.currencies.map { TokenRowItemUM.Token(tokenRow(account, it)) }
        }
    }

    // TODO [REDACTED_TASK_KEY] Show the network instead of the price once TangemTokenRow supports a network subtitle
    private fun tokenRow(account: AccountStatus, status: CryptoCurrencyStatus): TangemTokenRow.State =
        tokenRowStateConverter.convert(currencyStatus = status, onClick = { onTokenClick(account, status) })
}