package com.tangem.features.send.send.confirm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorBlockComponent
import com.tangem.features.send.common.ui.SignaturesBanner
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.common.ui.state.SendSignaturesUM
import com.tangem.features.send.common.ui.tapHelp
import com.tangem.features.send.send.ui.state.SendUM
import com.tangem.features.send.subcomponents.amount.DefaultSendAmountBlockComponent
import com.tangem.features.send.subcomponents.destination.DefaultSendDestinationBlockComponent
import com.tangem.features.send.subcomponents.notifications
import com.tangem.features.send.subcomponents.notifications.DefaultSendNotificationsComponent
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.material3.Text
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference

private const val BLOCKS_KEY = "BLOCKS_KEY"

private val JointFeeCardShape = RoundedCornerShape(24.dp)

@Suppress("LongParameterList")
@Composable
internal fun SendConfirmContent(
    sendUM: SendUM,
    destinationBlockComponent: DefaultSendDestinationBlockComponent,
    amountBlockComponent: DefaultSendAmountBlockComponent,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
    notificationsComponent: DefaultSendNotificationsComponent,
    notificationsUM: ImmutableList<NotificationUM>,
) {
    val confirmUM = sendUM.confirmUM as? ConfirmUM.Content

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            blocks(
                destinationBlockComponent = destinationBlockComponent,
                amountBlockComponent = amountBlockComponent,
                feeSelectorBlockComponent = feeSelectorBlockComponent,
                signaturesUM = confirmUM?.signaturesUM,
                setupFeeNote = confirmUM?.setupFeeNote,
            )
            if (confirmUM != null) {
                tapHelp(isDisplay = confirmUM.isShowTapHelp)
                with(notificationsComponent) {
                    content(
                        state = notificationsUM,
                        isClickDisabled = confirmUM.isSending,
                    )
                }
                notifications(
                    notifications = confirmUM.notifications,
                    isClickDisabled = confirmUM.isSending,
                )
            }
            item("SPACER_KEY") {
                SpacerH(16.dp)
            }
        }
    }
}

private fun LazyListScope.blocks(
    destinationBlockComponent: DefaultSendDestinationBlockComponent,
    amountBlockComponent: DefaultSendAmountBlockComponent,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
    signaturesUM: SendSignaturesUM?,
    setupFeeNote: TextReference?,
) {
    item(key = BLOCKS_KEY) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            amountBlockComponent.Content(modifier = Modifier)
            destinationBlockComponent.Content(modifier = Modifier)
            if (signaturesUM != null) {
                JointFeeCard(feeSelectorBlockComponent = feeSelectorBlockComponent, setupFeeNote = setupFeeNote)
                SignaturesBanner(signaturesUM = signaturesUM)
            } else {
                feeSelectorBlockComponent.Content(
                    modifier = Modifier
                        .clip(TangemTheme.shapes.roundedCornersXMedium)
                        .background(TangemTheme.colors3.bg.secondary),
                )
            }
        }
    }
}

@Composable
private fun JointFeeCard(feeSelectorBlockComponent: FeeSelectorBlockComponent, setupFeeNote: TextReference?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(JointFeeCardShape)
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        feeSelectorBlockComponent.Content(modifier = Modifier.fillMaxWidth())
        if (setupFeeNote != null) {
            HorizontalDivider(
                thickness = 1.dp,
                color = TangemTheme.colors3.border.secondary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = setupFeeNote.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.tertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}