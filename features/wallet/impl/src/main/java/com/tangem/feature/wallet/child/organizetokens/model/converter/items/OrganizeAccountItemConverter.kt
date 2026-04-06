package com.tangem.feature.wallet.child.organizetokens.model.converter.items

import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM
import com.tangem.utils.converter.Converter

internal class OrganizeAccountItemConverter(
    private val appCurrency: AppCurrency,
) : Converter<AccountStatus.CryptoPortfolio, OrganizeRowItemUM.Portfolio> {

    override fun convert(value: AccountStatus.CryptoPortfolio): OrganizeRowItemUM.Portfolio {
        val accountBalance = value.tokenList.totalFiatBalance as? TotalFiatBalance.Loaded
        return OrganizeRowItemUM.Portfolio(
            headerRowUM = TangemHeaderRowUM(
                id = value.accountId.value,
                title = value.account.accountName.toUM().value,
                subtitle = stringReference(
                    accountBalance?.amount.format {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                        )
                    },
                ),
            ),
        )
    }
}