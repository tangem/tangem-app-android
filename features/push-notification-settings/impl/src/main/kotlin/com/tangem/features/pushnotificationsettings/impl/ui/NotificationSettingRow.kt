package com.tangem.features.pushnotificationsettings.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.pushnotificationsettings.impl.R
import com.tangem.features.pushnotificationsettings.impl.entity.ToggleUM

@Composable
internal fun NotificationSettingRow(
    toggle: ToggleUM,
    showInlineMoreInfoLink: Boolean,
    onMoreInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        BlockCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = TangemTheme.dimens.spacing16,
                        vertical = TangemTheme.dimens.spacing12,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResourceSafe(id = toggle.titleRes),
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                )
                TangemSwitch(
                    checked = toggle.isOn,
                    onCheckedChange = toggle.onCheckedChange,
                )
            }
        }

        SubtitleText(
            base = toggle.subtitle,
            showMoreInfo = showInlineMoreInfoLink,
            onMoreInfoClick = onMoreInfoClick,
        )
    }
}

@Composable
private fun SubtitleText(base: TextReference, showMoreInfo: Boolean, onMoreInfoClick: () -> Unit) {
    val reference: TextReference = if (showMoreInfo) {
        combinedReference(
            base,
            stringReference(" "),
            styledResourceReference(
                id = R.string.push_notifications_more_info,
                spanStyleReference = {
                    TangemTheme.typography.caption2
                        .copy(color = TangemTheme.colors.text.accent)
                        .toSpanStyle()
                },
                onClick = onMoreInfoClick,
            ),
        )
    } else {
        base
    }
    Text(
        modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
        text = reference.resolveAnnotatedReference(),
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.tertiary,
    )
}