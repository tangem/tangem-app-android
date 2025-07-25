package com.tangem.features.swap.v2.impl.amount.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.features.swap.v2.impl.R
import com.tangem.utils.StringsSigns.PERCENT
import javax.inject.Inject

@ModelScoped
internal class SwapAmountAlertFactory @Inject constructor(
    private val uiMessageSender: UiMessageSender,
) {

    fun priceImpactAlert(hasPriceImpact: Boolean, currencySymbol: String, provider: ExpressProvider) {
        val slippage = provider.slippage?.let { "${it.parseBigDecimal(1)}$PERCENT" }
        val combinedMessage = buildList {
            when (provider.type) {
                ExpressProviderType.CEX -> {
                    if (slippage != null) {
                        add(
                            resourceReference(
                                id = R.string.swapping_alert_cex_description_with_slippage,
                                formatArgs = wrappedList(currencySymbol, slippage),
                            ),
                        )
                    } else {
                        add(resourceReference(R.string.swapping_alert_cex_description, wrappedList(currencySymbol)))
                    }
                }
                ExpressProviderType.DEX,
                ExpressProviderType.DEX_BRIDGE,
                -> {
                    if (hasPriceImpact) {
                        add(resourceReference(R.string.swapping_high_price_impact_description))
                        add(stringReference("\n\n"))
                    }
                    if (slippage != null) {
                        add(
                            resourceReference(
                                id = R.string.swapping_alert_dex_description_with_slippage,
                                formatArgs = wrappedList(slippage),
                            ),
                        )
                    } else {
                        add(resourceReference(R.string.swapping_alert_dex_description, wrappedList(currencySymbol)))
                    }
                }
                else -> Unit
            }
        }
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.swapping_alert_title),
                message = combinedReference(combinedMessage.toWrappedList()),
                firstActionBuilder = { okAction() },
            ),
        )
    }

    fun showCloseSendWithSwapAlert(onConfirm: () -> Unit) {
        // todo fix localization [REDACTED_TASK_KEY]
        uiMessageSender.send(
            DialogMessage(
                title = stringReference("Confirm cancellation"),
                message = stringReference(
                    "Are you sure you want to cancel the conversion? After changing, previous data will be reset.",
                ),
                firstActionBuilder = {
                    EventMessageAction(
                        title = stringReference("Confirm"),
                        onClick = onConfirm,
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = stringReference("Not Now"),
                        onClick = onDismissRequest,
                    )
                },
                dismissOnFirstAction = true,
            ),
        )
    }
}