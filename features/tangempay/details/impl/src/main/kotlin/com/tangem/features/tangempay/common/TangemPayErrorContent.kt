package com.tangem.features.tangempay.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class TangemPayErrorAction(
    val text: TextReference,
    val variant: TangemButton.Variant,
    val onClick: () -> Unit,
    val isLoading: Boolean = false,
    val isEnabled: Boolean = true,
)

@Composable
internal fun TangemPayErrorContent(
    title: TextReference,
    subtitle: TextReference,
    actions: ImmutableList<TangemPayErrorAction>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(bottom = TangemTheme.dimens2.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WarningIcon(modifier = Modifier.padding(top = TangemTheme.dimens2.x4))
        TitleText(
            text = title,
            modifier = Modifier.padding(top = TangemTheme.dimens2.x8),
        )
        SubtitleText(
            text = subtitle,
            modifier = Modifier.padding(top = TangemTheme.dimens2.x2),
        )
        SpacerH24()
        actions.forEach { action ->
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                text = action.text,
                variant = action.variant,
                size = TangemButton.Size.X12,
                isLoading = action.isLoading,
                isEnabled = action.isEnabled,
                onClick = action.onClick,
            )
        }
    }
}

@Composable
private fun WarningIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens2.x20)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.status.warningSubtle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens2.x7),
            imageVector = Icons.ic_error_28,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.status.warning,
        )
    }
}

@Composable
private fun TitleText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text.resolveReference(),
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SubtitleText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text.resolveReference(),
        style = TangemTheme.typography3.subheading.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayErrorContentPreview() {
    TangemThemePreviewRedesign {
        TangemPayErrorContent(
            title = resourceReference(R.string.tangempay_va_banking_details_error_title),
            subtitle = resourceReference(R.string.tangempay_va_banking_details_error_description),
            actions = persistentListOf(
                TangemPayErrorAction(
                    text = resourceReference(R.string.common_contact_support),
                    variant = TangemButton.Variant.Secondary,
                    onClick = {},
                ),
                TangemPayErrorAction(
                    text = resourceReference(R.string.common_retry),
                    variant = TangemButton.Variant.Primary,
                    onClick = {},
                ),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayErrorContentSingleActionPreview() {
    TangemThemePreviewRedesign {
        TangemPayErrorContent(
            title = resourceReference(R.string.common_something_went_wrong),
            subtitle = resourceReference(R.string.tangempay_va_banking_details_error_description),
            actions = persistentListOf(
                TangemPayErrorAction(
                    text = resourceReference(R.string.common_retry),
                    variant = TangemButton.Variant.Primary,
                    onClick = {},
                    isLoading = true,
                    isEnabled = false,
                ),
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}