package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.common.ui.expressStatus.state.ExpressLinkUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemState
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.common.ui.notifications.ExpressNotificationsUM
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExpressTransactionStateInfoUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExpressTransactionStateUM
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class TokenDetailsOnrampTransactionStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
    private val cryptoCurrency: CryptoCurrency,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Converter<OnrampTransaction, ExpressTransactionStateUM.OnrampUM> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: OnrampTransaction): ExpressTransactionStateUM.OnrampUM {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val appCurrency = appCurrencyProvider()
        return ExpressTransactionStateUM.OnrampUM(
            info = ExpressTransactionStateInfoUM(
                status = convertStatuses(value.status, value.externalTxUrl),
                notification = getNotification(value.status, value.externalTxUrl),
                txId = value.txId,
                txExternalId = value.externalTxId,
                txUrl = value.externalTxUrl,
                timestamp = resourceReference(
                    R.string.send_date_format,
                    wrappedList(
                        value.timestamp.toTimeFormat(DateTimeFormatters.dateFormatter),
                        value.timestamp.toTimeFormat(),
                    ),
                ),
                toAmount = stringReference(
                    value.toAmount.format { crypto(cryptoCurrency) },
                ),
                toFiatAmount = stringReference(
                    cryptoCurrencyStatus?.value?.fiatRate?.multiply(value.toAmount).format {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                        )
                    },
                ),
                toAmountSymbol = cryptoCurrency.symbol,
                toCurrencyIcon = iconStateConverter.convert(cryptoCurrency),
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
                onGoToProviderClick = clickIntents::onGoToProviderClick,
                onClick = { clickIntents.onOnrampTransactionClick(value.txId) },
            ),
            providerName = value.providerName,
            providerImageUrl = value.providerImageUrl,
            providerType = value.providerType,
            cryptoCurrencyName = cryptoCurrency.name,
            activeStatus = value.status,
        )
    }

    private fun getNotification(status: OnrampStatus.Status, externalTxUrl: String?): NotificationUM? {
        if (externalTxUrl == null) return null
        return when (status) {
            OnrampStatus.Status.Verifying -> {
                ExpressNotificationsUM.NeedVerification {
                    clickIntents.onGoToProviderClick(externalTxUrl)
                }
            }
            OnrampStatus.Status.Failed -> {
                ExpressNotificationsUM.FailedByProvider {
                    clickIntents.onGoToProviderClick(externalTxUrl)
                }
            }
            else -> null
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
                resourceReference(R.string.express_status_buying, wrappedList(cryptoCurrency.name))
            }
            this == OnrampStatus.Status.Paid -> {
                resourceReference(
                    R.string.express_status_buying_active,
                    wrappedList(cryptoCurrency.name),
                )
            }
            else -> {
                resourceReference(R.string.express_status_bought, wrappedList(cryptoCurrency.name))
            }
        },
        state = getStatusState(OnrampStatus.Status.Paid),
    )

    private fun OnrampStatus.Status.getSendingItem() = ExpressStatusItemUM(
        text = when {
            order < OnrampStatus.Status.Sending.order -> {
                resourceReference(
                    R.string.express_exchange_status_sending,
                    wrappedList(cryptoCurrency.name),
                )
            }
            this == OnrampStatus.Status.Sending -> {
                resourceReference(
                    R.string.express_exchange_status_sending_active,
                    wrappedList(cryptoCurrency.name),
                )
            }
            else -> {
                resourceReference(
                    R.string.express_exchange_status_sent,
                    wrappedList(cryptoCurrency.name),
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
