package com.tangem.features.moneyaccount.details

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.core.ui.res.generated.icons.ic_mail_20
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.features.moneyaccount.details.impl.R
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongParameterList")
internal class MoneyAccountDetailsStateFactory(
    private val onAddFundsClick: () -> Unit,
    private val onSendClick: () -> Unit,
    private val onTermsClick: () -> Unit,
    private val onSupportClick: () -> Unit,
    private val onBackClick: () -> Unit,
    private val onRefresh: () -> Unit,
    private val onRenewSession: () -> Unit,
) {
    fun createInitialState(): MoneyAccountDetailsUM = MoneyAccountDetailsUM(
        title = resourceReference(R.string.virtual_account_title),
        subtitle = resourceReference(R.string.tangempay_usdc_on_polygon_network),
        balance = MoneyAccountBalanceBlockState.Loading,
        isBalanceHidden = false,
        messageBanner = null,
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
        txHistoryCurrency = null,
        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = { onRefresh() }),
    )

    fun createAccountState(
        prevState: MoneyAccountDetailsUM,
        account: MoneyAccountStatusValue.Account,
    ): MoneyAccountDetailsUM {
        val balance = account.balance
        return prevState.copy(
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
            txHistoryCurrency = account.cryptoCurrency,
            messageBanner = createErrorBanner(account.error)
                ?: createBalanceUnavailableBanner().takeIf { balance == null },
        )
    }

    private fun createErrorBanner(error: MoneyAccountStatusValue.Error?): TangemMessageBanner.State? = when (error) {
        null -> null
        MoneyAccountStatusValue.Error.NotSynced -> createRenewSessionBanner()
        else -> createAccountUnavailableBanner()
    }

    private fun createRenewSessionBanner() = TangemMessageBanner.State(
        title = resourceReference(R.string.tangempay_sync_needed_title),
        variant = TangemMessageBanner.Variant.Error,
        description = resourceReference(R.string.tangempay_sync_needed_body),
        secondaryButton = TangemMessageBanner.Button(
            text = resourceReference(R.string.tangempay_sync_needed_button),
            onClick = onRenewSession,
            iconEnd = TangemIconUM.Icon(
                iconRes = R.drawable.ic_tangem_24,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
        ),
    )

    private fun createBalanceUnavailableBanner() = TangemMessageBanner.State(
        title = resourceReference(R.string.money_account_balance_unavailable_title),
        variant = TangemMessageBanner.Variant.Error,
        description = resourceReference(R.string.money_account_balance_unavailable_description),
        iconEnd = TangemIconUM.Icon(
            imageVector = Icons.ic_error_20,
            tintReference = { TangemTheme.colors3.icon.primary },
        ),
    )

    private fun createAccountUnavailableBanner() = TangemMessageBanner.State(
        title = resourceReference(R.string.tangempay_temporarily_unavailable),
        variant = TangemMessageBanner.Variant.Error,
        description = resourceReference(R.string.tangempay_service_unreachable_try_later),
        iconEnd = TangemIconUM.Icon(
            imageVector = Icons.ic_error_20,
            tintReference = { TangemTheme.colors3.icon.primary },
        ),
    )
}