package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.expressStatus.state.ExpressLinkUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemState
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.common.ui.notifications.ExpressNotificationsUM
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.analytics.TokenOnrampAnalyticsEvent
import com.tangem.feature.wallet.impl.R
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateIconUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateInfoUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class SingleWalletOnrampTransactionConverter(
    private val clickIntents: WalletClickIntents,
    cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Converter<OnrampTransaction, ExpressTransactionStateUM.OnrampUM> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    private val currency = cryptoCurrencyStatus.currency
    private val status = cryptoCurrencyStatus.value

    override fun convert(value: OnrampTransaction): ExpressTransactionStateUM.OnrampUM {
        return ExpressTransactionStateUM.OnrampUM(
            info = ExpressTransactionStateInfoUM(
                title = resourceReference(
                    id = R.string.express_status_buying,
                    wrappedList(currency.name),
                ),
                status = convertStatuses(value.status, value.externalTxUrl),
                notification = getNotification(value.status, value.externalTxUrl, value.providerName),
                txId = value.txId,
                txExternalId = value.externalTxId,
                txExternalUrl = value.externalTxUrl,
                timestamp = value.timestamp,
                timestampFormatted = resourceReference(
                    R.string.send_date_format,
                    wrappedList(
                        value.timestamp.toDateFormatWithTodayYesterday(),
                        value.timestamp.toTimeFormat(),
                    ),
                ),
                toAmount = stringReference(value.toAmount.format { crypto(currency) }),
                toFiatAmount = stringReference(
                    status.fiatRate?.multiply(value.toAmount).format {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                        )
                    },
                ),
                toAmountSymbol = currency.symbol,
                toCurrencyIcon = iconStateConverter.convert(currency),
                fromAmount = stringReference(
                    value.fromAmount.format {
                        fiat(
                            fiatCurrencyCode = value.fromCurrency.name,
                            fiatCurrencySymbol = value.fromCurrency.code,
                        )
                    },
                ),
                fromFiatAmount = null,
                fromAmountSymbol = value.fromCurrency.code,
                fromCurrencyIcon = CurrencyIconState.FiatIcon(
                    url = value.fromCurrency.image,
                    fallbackResId = R.drawable.ic_currency_24,
                ),
                iconState = getIconState(value.status),
                onGoToProviderClick = {
                    analyticsEventHandler.send(TokenOnrampAnalyticsEvent.GoToProvider)
                    clickIntents.onGoToProviderClick(it)
                },
                onDisposeExpressStatus = clickIntents::onConfirmDisposeExpressStatus,
                onClick = {
                    val analyticEvent = TokenOnrampAnalyticsEvent.OnrampStatusOpened(
                        tokenSymbol = currency.symbol,
                        provider = value.providerName,
                        fiatCurrency = value.fromCurrency.code,
                    )
                    analyticsEventHandler.send(analyticEvent)
                    clickIntents.onExpressTransactionClick(value.txId)
                },
            ),
            providerName = value.providerName,
            providerImageUrl = value.providerImageUrl,
            providerType = value.providerType,
            activeStatus = value.status,
            fromCurrencyCode = value.fromCurrency.code,
        )
    }

    private fun getNotification(
        status: OnrampStatus.Status,
        externalTxUrl: String?,
        providerName: String,
    ): NotificationUM? {
        return when (status) {
            OnrampStatus.Status.Verifying -> {
                analyticsEventHandler.send(
                    TokenOnrampAnalyticsEvent.NoticeKYC(currency.symbol, providerName),
                )
                ExpressNotificationsUM.NeedVerification(
                    onGoToProviderClick = onProviderClick(externalTxUrl),
                )
            }
            OnrampStatus.Status.Failed -> {
                ExpressNotificationsUM.FailedByProvider(
                    onGoToProviderClick = onProviderClick(externalTxUrl),
                )
            }
            else -> null
        }
    }

    private fun onProviderClick(externalTxUrl: String?) = if (externalTxUrl != null) {
        { clickIntents.onGoToProviderClick(externalTxUrl) }
    } else {
        null
    }

    private fun getIconState(status: OnrampStatus.Status): ExpressTransactionStateIconUM {
        return when (status) {
            OnrampStatus.Status.Verifying -> ExpressTransactionStateIconUM.Warning
            OnrampStatus.Status.Failed -> ExpressTransactionStateIconUM.Error
            else -> ExpressTransactionStateIconUM.None
        }
    }

    private fun convertStatuses(status: OnrampStatus.Status, externalTxUrl: String?): ExpressStatusUM {
        val statuses = with(status) {
            persistentListOf(
                getAwaitingDepositItem(),
                getPaymentProcessingItem(),
                getBuyingItem(),
                getSendingItem(),
            )
        }

        return ExpressStatusUM(
            title = resourceReference(R.string.common_transaction_status),
            link = getStatusLink(status, externalTxUrl),
            statuses = statuses,
        )
    }

    private fun OnrampStatus.Status.getAwaitingDepositItem() = ExpressStatusItemUM(
        text = when {
            order < OnrampStatus.Status.WaitingForPayment.order -> {
                resourceReference(R.string.express_exchange_status_receiving)
            }
            this == OnrampStatus.Status.WaitingForPayment -> {
                resourceReference(R.string.express_exchange_status_receiving_active)
            }
            else -> {
                resourceReference(R.string.express_exchange_status_received)
            }
        },
        state = getStatusState(OnrampStatus.Status.WaitingForPayment),
    )

    private fun OnrampStatus.Status.getPaymentProcessingItem() = ExpressStatusItemUM(
        text = when {
            order < OnrampStatus.Status.PaymentProcessing.order -> {
                resourceReference(R.string.express_exchange_status_confirming)
            }
            this == OnrampStatus.Status.PaymentProcessing -> {
                resourceReference(R.string.express_exchange_status_confirming_active)
            }
            this == OnrampStatus.Status.Verifying -> {
                resourceReference(R.string.express_exchange_status_verifying)
            }
            this == OnrampStatus.Status.Failed -> {
                resourceReference(R.string.express_exchange_status_failed)
            }
            else -> resourceReference(R.string.express_exchange_status_confirmed)
        },
        state = when {
            order < OnrampStatus.Status.PaymentProcessing.order -> {
                ExpressStatusItemState.Default
            }
            this == OnrampStatus.Status.PaymentProcessing -> {
                ExpressStatusItemState.Active
            }
            this == OnrampStatus.Status.Verifying -> {
                ExpressStatusItemState.Warning
            }
            this == OnrampStatus.Status.Failed -> {
                ExpressStatusItemState.Error
            }
            else -> {
                ExpressStatusItemState.Done
            }
        },
    )

    private fun OnrampStatus.Status.getBuyingItem() = ExpressStatusItemUM(
        text = when {
            order < OnrampStatus.Status.Paid.order -> {
                resourceReference(R.string.express_status_buying, wrappedList(currency.name))
            }
            this == OnrampStatus.Status.Paid -> {
                resourceReference(
                    R.string.express_status_buying_active,
                    wrappedList(currency.name),
                )
            }
            else -> {
                resourceReference(R.string.express_status_bought, wrappedList(currency.name))
            }
        },
        state = getStatusState(OnrampStatus.Status.Paid),
    )

    private fun OnrampStatus.Status.getSendingItem() = ExpressStatusItemUM(
        text = when {
            order < OnrampStatus.Status.Sending.order -> {
                resourceReference(
                    R.string.express_exchange_status_sending,
                    wrappedList(currency.name),
                )
            }
            this == OnrampStatus.Status.Sending -> {
                resourceReference(
                    R.string.express_exchange_status_sending_active,
                    wrappedList(currency.name),
                )
            }
            else -> {
                resourceReference(
                    R.string.express_exchange_status_sent,
                    wrappedList(currency.name),
                )
            }
        },
        state = getStatusState(OnrampStatus.Status.Sending),
    )

    private fun getStatusLink(status: OnrampStatus.Status, externalTxUrl: String?): ExpressLinkUM {
        if (externalTxUrl == null) return ExpressLinkUM.Empty
        return when (status) {
            OnrampStatus.Status.Verifying,
            OnrampStatus.Status.Failed,
            -> {
                ExpressLinkUM.Content(
                    icon = R.drawable.ic_arrow_top_right_24,
                    text = resourceReference(R.string.common_go_to_provider),
                    onClick = {
                        clickIntents.onGoToProviderClick(externalTxUrl)
                    },
                )
            }
            else -> ExpressLinkUM.Empty
        }
    }

    private fun OnrampStatus.Status.getStatusState(targetState: OnrampStatus.Status) = when {
        order < targetState.order -> ExpressStatusItemState.Default
        this == targetState -> ExpressStatusItemState.Active
        else -> ExpressStatusItemState.Done
    }
}