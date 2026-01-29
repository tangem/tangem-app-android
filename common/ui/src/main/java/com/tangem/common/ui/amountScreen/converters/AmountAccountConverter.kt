package com.tangem.common.ui.amountScreen.converters

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.Account
import com.tangem.utils.converter.Converter

class AmountAccountConverter(
    private val prefixText: TextReference,
    private val isAccountsMode: Boolean,
    private val walletTitle: TextReference,
) : Converter<Account?, AccountTitleUM> {
    override fun convert(value: Account?): AccountTitleUM {
        return if (value != null && isAccountsMode) {
            AccountTitleUM.Account(
                name = value.accountName.toUM().value,
                icon = getAccountIcon(account = value),
                prefixText = prefixText,
            )
        } else {
            AccountTitleUM.Text(
                title = walletTitle,
            )
        }
    }

    private fun getAccountIcon(account: Account): AccountIconUM {
        return when (account) {
            is Account.Crypto.Portfolio -> CryptoPortfolioIconConverter.convert(account.icon)
            is Account.Payment -> AccountIconUM.Payment
        }
    }
}