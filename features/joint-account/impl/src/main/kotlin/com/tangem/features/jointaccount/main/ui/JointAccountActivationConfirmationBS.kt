package com.tangem.features.jointaccount.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_28_filled
import com.tangem.features.jointaccount.main.JointAccountMembersUM.ActivationUM
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointAccountActivationConfirmationBS(state: ActivationUM.ConfirmationUM, modifier: Modifier = Modifier) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onCancelClick,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopNavigation(
                blurBackground = false,
                endButton = { TangemButton.Close(onClick = state.onCancelClick) },
            )
        },
        content = {
            Content(
                state = state,
                modifier = modifier,
            )
        },
    )
}

@Composable
private fun Content(state: ActivationUM.ConfirmationUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 32.dp)
                .size(72.dp)
                .background(color = TangemTheme.colors3.bg.status.infoSubtle, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = Icons.ic_shield_checkmark_28_filled,
                tint = TangemTheme.colors3.icon.status.info,
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier
                .padding(top = 32.dp)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResourceSafe(CoreUiR.string.joint_account_activation_title),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResourceSafe(CoreUiR.string.joint_account_activation_subtitle),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(CoreUiR.string.common_activate),
                onClick = state.onConfirmClick,
            )

            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                text = resourceReference(CoreUiR.string.common_cancel),
                onClick = state.onCancelClick,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_JointAccountActivationConfirmation() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.secondary)) {
            Content(
                state = ActivationUM.ConfirmationUM(
                    onConfirmClick = {},
                    onCancelClick = {},
                ),
            )
        }
    }
}
// endregion