package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.common.ui.account.AccountIconItemStateConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.common.ui.tokens.TokenRowStateConverter
import com.tangem.common.ui.tokens.toTokenIconState
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.tokenlist.entity.AccountAvailabilityUM
import com.tangem.features.onramp.tokenlist.entity.AccountCurrencyUM
import com.tangem.features.onramp.tokenlist.entity.OnrampTokenGroupUM
import com.tangem.features.onramp.tokenlist.entity.OnrampTokenRowUM
import com.tangem.features.onramp.tokenlist.entity.utils.OnrampTokenItemStateConverterFactory
import kotlinx.collections.immutable.toPersistentList

internal class OnrampTokenGroupConverter(
    private val appCurrency: AppCurrency,
    private val unavailableErrorText: TextReference,
    private val onItemClick: (TokenItemState, CryptoCurrencyStatus) -> Unit,
) {

    private val tokenRowStateConverter = TokenRowStateConverter(appCurrency)
    private val accountIconConverter = AccountIconItemStateConverter(size = AccountIconSize.ExtraSmall)
    private val unavailableIconConverter = CryptoCurrencyToIconStateConverter(isAvailable = false)
    private val legacyItemConverter: TokenItemStateConverter =
        OnrampTokenItemStateConverterFactory.createAvailableItemConverter(appCurrency, onItemClick)

    fun convert(accountList: List<AccountAvailabilityUM>, isAccountsMode: Boolean): List<OnrampTokenGroupUM> {
        if (!isAccountsMode) {
            val rows = accountList.flatMap { it.currencyList }.map(::toRow)
            return if (rows.isEmpty()) emptyList() else listOf(availableTokensGroup(rows))
        }
        return accountList.map { (account, currencyList) ->
            OnrampTokenGroupUM(
                id = account.accountId.value,
                header = accountHeader(account = account, tokensCount = currencyList.size),
                rows = currencyList.map(::toRow).toPersistentList(),
            )
        }
    }

    fun convertLoading(
        accountList: List<AccountStatus.CryptoPortfolio>,
        isAccountsMode: Boolean,
    ): List<OnrampTokenGroupUM> {
        fun shimmerRows(account: AccountStatus.CryptoPortfolio) = account.tokenList.flattenCurrencies()
            .map { OnrampTokenRowUM.Available(TangemTokenRow.State.Shimmer(id = it.currency.id.value)) }

        if (!isAccountsMode) {
            val rows = accountList.flatMap(::shimmerRows)
            return if (rows.isEmpty()) emptyList() else listOf(availableTokensGroup(rows))
        }
        return accountList.map { status ->
            OnrampTokenGroupUM(
                id = status.account.accountId.value,
                header = accountHeader(
                    account = status.account,
                    tokensCount = status.tokenList.flattenCurrencies().size,
                ),
                rows = shimmerRows(status).toPersistentList(),
            )
        }
    }

    private fun accountHeader(account: Account.CryptoPortfolio, tokensCount: Int) = OnrampTokenGroupUM.Header.Account(
        headerRowUM = TangemHeaderRowUM(
            id = account.accountId.value,
            startIconUM = TangemIconUM.Currency(currencyIconState = accountIconConverter.convert(account)),
            title = account.accountName.toUM().value,
            subtitle = pluralReference(
                id = R.plurals.common_tokens_count,
                count = tokensCount,
                formatArgs = wrappedList(tokensCount),
            ),
        ),
    )

    private fun availableTokensGroup(rows: List<OnrampTokenRowUM>) = OnrampTokenGroupUM(
        id = AVAILABLE_TOKENS_GROUP_ID,
        header = OnrampTokenGroupUM.Header.Section(
            title = resourceReference(R.string.exchange_tokens_available_tokens_header),
        ),
        rows = rows.toPersistentList(),
    )

    private fun toRow(currency: AccountCurrencyUM): OnrampTokenRowUM {
        val status = currency.cryptoCurrencyStatus
        return if (currency.isAvailable) {
            // TODO [REDACTED_TASK_KEY] Show the ticker instead of the price once TangemTokenRow supports a text subtitle
            OnrampTokenRowUM.Available(
                state = tokenRowStateConverter.convert(
                    currencyStatus = status,
                    onClick = { onItemClick(legacyItemConverter.convert(status), status) },
                ),
            )
        } else {
            OnrampTokenRowUM.Unavailable(
                id = status.currency.id.value,
                icon = unavailableIconConverter.convert(status).toTokenIconState(),
                title = stringReference(status.currency.name),
                reason = unavailableErrorText,
                fiatBalance = status.fiatBalance(),
                cryptoBalance = status.cryptoBalance(),
            )
        }
    }

    private fun CryptoCurrencyStatus.fiatBalance(): TextReference? = takeIf { hasBalance() }?.let {
        stringReference(
            getTotalFiatAmount().format {
                fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
            },
        )
    }

    private fun CryptoCurrencyStatus.cryptoBalance(): TextReference? = takeIf { hasBalance() }?.let {
        stringReference(getTotalCryptoAmount().format { crypto(cryptoCurrency = currency) })
    }

    private fun CryptoCurrencyStatus.hasBalance(): Boolean = when (value) {
        is CryptoCurrencyStatus.Loaded,
        is CryptoCurrencyStatus.Custom,
        is CryptoCurrencyStatus.NoQuote,
        is CryptoCurrencyStatus.NoAccount,
        -> true
        is CryptoCurrencyStatus.Loading,
        is CryptoCurrencyStatus.MissedDerivation,
        is CryptoCurrencyStatus.Unreachable,
        is CryptoCurrencyStatus.NoAmount,
        -> false
    }

    private companion object {
        const val AVAILABLE_TOKENS_GROUP_ID = "available_tokens"
    }
}