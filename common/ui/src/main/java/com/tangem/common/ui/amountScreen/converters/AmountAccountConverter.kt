package com.tangem.common.ui.amountScreen.converters

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.Account
import com.tangem.utils.converter.Converter

class AmountAccountConverter(
    private val prefixText: TextReference,
    private val isAccountsMode: Boolean,
    private val walletTitle: TextReference,
) : Converter<Account.CryptoPortfolio?, AccountTitleUM> {
    override fun convert(value: Account.CryptoPortfolio?): AccountTitleUM {
        return if (value != null && isAccountsMode) {
            AccountTitleUM.Account(
                name = value.accountName.toUM().value,
                icon = value.icon.toUM(),
                prefixText = prefixText,
            )
        } else {
            AccountTitleUM.Text(
                title = walletTitle,
            )
        }
    }
}