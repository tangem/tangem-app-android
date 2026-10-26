package com.tangem.features.moneyaccount.details

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.core.ui.res.generated.icons.ic_mail_20
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.features.moneyaccount.details.impl.R
import kotlinx.collections.immutable.persistentListOf

internal class MoneyAccountDetailsStateFactory(
    private val onAddFundsClick: () -> Unit,
    private val onSendClick: () -> Unit,
    private val onTermsClick: () -> Unit,
    private val onSupportClick: () -> Unit,
    private val onBackClick: () -> Unit,
) {
    fun createInitialState(): MoneyAccountDetailsUM = MoneyAccountDetailsUM(
        title = resourceReference(R.string.virtual_account_title),
        subtitle = resourceReference(R.string.tangempay_usdc_on_polygon_network),
        balance = MoneyAccountBalanceBlockState.Loading,
        isBalanceHidden = false,
        menuItems = persistentListOf(
            MoneyAccountMenuItemUM(
                title = stringReference("Terms and Limits"),
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_document_20,
                    tintReference = { TangemTheme.colors3.icon.primary },
                ),
                onClick = onTermsClick,
            ),
            MoneyAccountMenuItemUM(
                title = resourceReference(R.string.tangempay_pay_support),
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_mail_20,
                    tintReference = { TangemTheme.colors3.icon.primary },
                ),
                onClick = onSupportClick,
            ),
        ),
        onBackClick = onBackClick,
        onAddFundsClick = onAddFundsClick,
        onSendClick = onSendClick,
    )

    fun createAccountState(account: MoneyAccountStatusValue.Account): MoneyAccountDetailsUM {
        val balance = account.balance
        return createInitialState().copy(
            balance = if (balance == null) {
                MoneyAccountBalanceBlockState.Unavailable
            } else {
                val currency = getJavaCurrencyByCode(balance.currencyCode)
                MoneyAccountBalanceBlockState.Content(
                    fiatBalance = balance.cryptoAmount.formatStyled {
                        fiat(
                            fiatCurrencyCode = currency.currencyCode,
                            fiatCurrencySymbol = currency.symbol,
                            spanStyleReference = { TangemTheme.typography3.heading.medium.toSpanStyle() },
                        )
                    },
                    isBalanceFlickering = account.source == StatusSource.CACHE,
                )
            },
        )
    }
}