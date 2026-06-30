package com.tangem.features.tangempay.tiers.current

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun TangemPayCurrentPlanScreen(state: TangemPayCurrentPlanUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CurrentPlanTopBar(state = state)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                state.notification?.let { PlanNotification(notification = it) }
                state.sections.fastForEach { section -> PlanSection(section = section) }
            }

            ChangePlanFooter(state = state)
        }
    }
}

@Composable
private fun CurrentPlanTopBar(state: TangemPayCurrentPlanUM) {
    TangemTopBar(
        modifier = Modifier.statusBarsPadding(),
        title = resourceReference(R.string.tangempay_current_plan_title),
        subtitle = state.planName,
        startContent = {
            TangemButton(
                iconStart = TangemIconUM.Icon(iconRes = CoreUiR.drawable.ic_arrow_back_28),
                onClick = state.onBackClick,
                size = TangemButton.Size.X11,
                variant = TangemButton.Variant.Material,
            )
        },
    )
}

@Composable
private fun PlanNotification(notification: TangemPayCurrentPlanUM.Notification, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.status.infoSubtle)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = CoreUiR.drawable.ic_information_24),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.info,
            )
            Text(
                text = notification.text.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
        notification.button?.let { button ->
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X11,
                text = button.text,
                onClick = button.onClick,
            )
        }
    }
}

@Composable
private fun PlanSection(section: TangemPayCurrentPlanUM.Section, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 8.dp,
            ),
            text = section.header.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(TangemTheme.colors3.bg.secondary),
        ) {
            section.items.fastForEachIndexed { index, item ->
                PlanInfoRow(item = item)
                if (index != section.items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = TangemTheme.colors3.border.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanInfoRow(item: TangemPayCurrentPlanUM.InfoItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = item.label.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.value.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun ChangePlanFooter(state: TangemPayCurrentPlanUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_current_plan_change),
            onClick = state.onChangePlanClick,
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun TangemPayCurrentPlanScreenPreview() {
    TangemThemePreviewRedesign {
        TangemPayCurrentPlanScreen(state = previewState())
    }
}

private fun previewState() = TangemPayCurrentPlanUM(
    planName = stringReference("Plus"),
    notification = TangemPayCurrentPlanUM.Notification(
        text = stringReference(
            "Your Plus plan is active till Mar 23, then we will move you to Basic. $29.99 won't be charged.",
        ),
        button = TangemPayCurrentPlanUM.Notification.Button(
            text = stringReference("Stay on Plus"),
            onClick = {},
        ),
    ),
    sections = persistentListOf(
        TangemPayCurrentPlanUM.Section(
            header = stringReference("Card related"),
            items = persistentListOf(
                TangemPayCurrentPlanUM.InfoItem(stringReference("Visa Programme"), stringReference("Signature")),
                TangemPayCurrentPlanUM.InfoItem(
                    label = stringReference("Max daily spending limit"),
                    value = stringReference("$50.000"),
                ),
                TangemPayCurrentPlanUM.InfoItem(stringReference("FX fee"), stringReference("1%")),
            ),
        ),
        TangemPayCurrentPlanUM.Section(
            header = stringReference("Plan related"),
            items = persistentListOf(
                TangemPayCurrentPlanUM.InfoItem(stringReference("Plan fee"), stringReference("\$29.99/month")),
                TangemPayCurrentPlanUM.InfoItem(stringReference("Max cards issued"), stringReference("5")),
                TangemPayCurrentPlanUM.InfoItem(
                    label = stringReference("Additional benefits"),
                    value = stringReference("Benefit 1, Benefit 2, Benefit 3"),
                ),
            ),
        ),
    ),
    onBackClick = {},
    onChangePlanClick = {},
)