package com.tangem.features.swap.v2.impl.sendviaswap.confirm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.footers.SendingText
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.notifications
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.SwapAmountBlockComponent
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsComponent
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongParameterList")
@Composable
internal fun SendWithSwapConfirmContent(
    sendWithSwapUM: SendWithSwapUM,
    amountBlockComponent: SwapAmountBlockComponent,
    sendDestinationBlockComponent: SendDestinationBlockComponent,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
    sendNotificationsComponent: SendNotificationsComponent,
    sendNotificationsUM: ImmutableList<NotificationUM>,
    swapNotificationsComponent: SwapNotificationsComponent,
    swapNotificationsUM: ImmutableList<NotificationUM>,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val confirmUM = sendWithSwapUM.confirmUM as? ConfirmUM.Content

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            item(key = "SendWithSwapBlocks") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    amountBlockComponent.Content(Modifier)
                    sendDestinationBlockComponent.Content(Modifier)
                    feeSelectorBlockComponent.Content(Modifier.clip(RoundedCornerShape(16.dp)))
                }
            }
            if (confirmUM != null) {
                // tapHelp(isDisplay = confirmUM.showTapHelp) // todo
                with(swapNotificationsComponent) {
                    content(
                        state = swapNotificationsUM,
                        isClickDisabled = confirmUM.isTransactionInProcess,
                    )
                }
                with(sendNotificationsComponent) {
                    content(
                        state = sendNotificationsUM,
                        isClickDisabled = confirmUM.isTransactionInProcess,
                    )
                }
                notifications(
                    notifications = confirmUM.notifications,
                    isClickDisabled = confirmUM.isTransactionInProcess,
                )
            }
        }
        val sendFooter = confirmUM?.sendingFooter ?: TextReference.EMPTY
        val legalFooter = getAnnotatedStringForLegals(confirmUM?.tosUM, onClick = onLinkClick)
        SendingText(
            footerText = if (sendFooter != TextReference.EMPTY || legalFooter != TextReference.EMPTY) {
                combinedReference(sendFooter, legalFooter)
            } else {
                TextReference.EMPTY
            },
        )
    }
}

@Composable
private fun getAnnotatedStringForLegals(tosUM: ConfirmUM.Content.TosUM?, onClick: (String) -> Unit): TextReference {
    if (tosUM == null) return TextReference.EMPTY
    val tos = tosUM.tosLink
    val policy = tosUM.policyLink
    return if (tos != null && policy != null) {
        val tosTitle = tos.title.resolveReference()
        val policyTitle = policy.title.resolveReference()
        val fullString = stringResourceSafe(id = R.string.express_legal_two_placeholders, tosTitle, policyTitle)
        val tosIndex = fullString.indexOf(tosTitle)
        val policyIndex = fullString.indexOf(policyTitle)

        annotatedReference {
            append(StringsSigns.POINT_SIGN)
            appendSpace()
            append(fullString.substring(0, tosIndex))
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "TOS_TAG",
                    linkInteractionListener = { onClick(tos.link) },
                ),
                block = {
                    appendColored(
                        text = fullString.substring(tosIndex, tosIndex + tosTitle.length),
                        color = TangemTheme.colors.text.accent,
                    )
                },
            )
            append(fullString.substring(tosIndex + tosTitle.length, policyIndex))
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "POLICY_TAG",
                    linkInteractionListener = { onClick(policy.link) },
                ),
                block = {
                    appendColored(
                        text = fullString.substring(policyIndex, policyIndex + policyTitle.length),
                        color = TangemTheme.colors.text.accent,
                    )
                },
            )
        }
    } else {
        val legal = requireNotNull(tos ?: policy) { "tos or policy must not be null" }
        val legalTitle = legal.title.resolveReference()
        val fullString = stringResourceSafe(id = R.string.express_legal_one_placeholder, legalTitle)
        val legalIndex = fullString.indexOf(legalTitle)

        annotatedReference {
            append(fullString.substring(0, legalIndex))
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "LEGAL_TAG",
                    linkInteractionListener = { onClick(legal.link) },
                ),
                block = {
                    appendColored(
                        text = fullString.substring(legalIndex, legalIndex + legalTitle.length),
                        color = TangemTheme.colors.text.accent,
                    )
                },
            )
        }
    }
}