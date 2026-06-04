package com.tangem.features.tangempay.closure

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayCloseCardContent(state: TangemPayCloseCardUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismissRequest,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                        onClick = state.onDismissRequest,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        },
        content = {
            Content(state)
        },
    )
}

@Composable
private fun Content(state: TangemPayCloseCardUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens2.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(TangemTheme.dimens2.x6))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.status.warningSubtle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_warning_20),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.warning,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.height(TangemTheme.dimens2.x8))

        Text(
            text = stringResourceSafe(R.string.tangem_pay_close_card_popup_title),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens2.x2))

        Text(
            text = stringResourceSafe(R.string.tangem_pay_close_card_popup_description),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens2.x12))

        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangem_pay_close_card_popup_secondary_button_title),
            isEnabled = !state.isClosingInProgress,
            onClick = state.onDismissRequest,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens2.x2))

        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangem_pay_close_card_popup_primary_button_title),
            isLoading = state.isClosingInProgress,
            isEnabled = !state.isClosingInProgress,
            onClick = state.onCloseClick,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens2.x4))
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        Content(
            state = TangemPayCloseCardUM(
                isClosingInProgress = true,
                onCloseClick = {},
                onDismissRequest = {},
            ),
        )
    }
}