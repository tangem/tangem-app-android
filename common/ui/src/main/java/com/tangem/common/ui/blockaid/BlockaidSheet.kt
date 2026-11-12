package com.tangem.common.ui.blockaid

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.core.ui.res.generated.icons.ic_warning_28_filled
import com.tangem.core.ui.R as CoreUiR

data class BlockaidSheetUM(
    val title: TextReference,
    val body: TextReference,
    val secondaryButton: ButtonUM?,
    val primaryButton: ButtonUM,
    val onClose: () -> Unit,
) {

    data class ButtonUM(
        val text: TextReference,
        val hasTangemIcon: Boolean = false,
        val onClick: () -> Unit,
    )

    companion object {

        fun risky(primaryButton: ButtonUM, secondaryButton: ButtonUM, onClose: () -> Unit) = BlockaidSheetUM(
            title = resourceReference(CoreUiR.string.joint_blockaid_risky_title),
            body = resourceReference(CoreUiR.string.joint_blockaid_risky_body),
            secondaryButton = secondaryButton,
            primaryButton = primaryButton,
            onClose = onClose,
        )

        fun malicious(primaryButton: ButtonUM, onClose: () -> Unit) = BlockaidSheetUM(
            title = resourceReference(CoreUiR.string.joint_blockaid_malicious_title),
            body = resourceReference(CoreUiR.string.joint_blockaid_malicious_body),
            secondaryButton = null,
            primaryButton = primaryButton,
            onClose = onClose,
        )
    }
}

class BlockaidBottomSheetComponent(private val state: BlockaidSheetUM) : ComposableBottomSheetComponent {

    override fun dismiss() {
        state.onClose()
    }

    @Composable
    override fun BottomSheet() {
        BlockaidBottomSheet(state = state)
    }
}

@Composable
fun BlockaidBottomSheet(state: BlockaidSheetUM) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onClose,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        title = { BlockaidTopNavigation(onCloseClick = state.onClose) },
        content = { BlockaidSheetContent(state = state) },
    )
}

@Composable
fun BlockaidTopNavigation(onCloseClick: () -> Unit) {
    TangemModal.TopNavigation(endButton = { TangemButton.Close(onClick = onCloseClick) })
}

@Composable
fun BlockaidSheetContent(state: BlockaidSheetUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 32.dp)
                .size(72.dp)
                .background(color = TangemTheme.colors3.bg.status.errorSubtle, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = Icons.ic_warning_28_filled,
                tint = TangemTheme.colors3.icon.status.error,
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
                text = state.title.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = state.body.resolveReference(),
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
            state.secondaryButton?.let { button ->
                SheetButton(button = button, variant = TangemButton.Variant.Secondary)
            }
            SheetButton(button = state.primaryButton, variant = TangemButton.Variant.Primary)
        }
    }
}

@Composable
private fun SheetButton(button: BlockaidSheetUM.ButtonUM, variant: TangemButton.Variant) {
    TangemButton(
        modifier = Modifier.fillMaxWidth(),
        variant = variant,
        size = TangemButton.Size.X12,
        text = button.text,
        iconEnd = TangemIconUM.Icon(Icons.ic_logo_tangem_20).takeIf { button.hasTangemIcon },
        onClick = button.onClick,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_BlockaidSheetContent() {
    TangemThemePreviewRedesign {
        BlockaidSheetContent(
            state = BlockaidSheetUM(
                title = resourceReference(CoreUiR.string.joint_blockaid_risky_title),
                body = resourceReference(CoreUiR.string.joint_blockaid_risky_body),
                secondaryButton = BlockaidSheetUM.ButtonUM(
                    text = resourceReference(CoreUiR.string.joint_blockaid_decline_transaction),
                    onClick = {},
                ),
                primaryButton = BlockaidSheetUM.ButtonUM(
                    text = resourceReference(CoreUiR.string.joint_blockaid_sign_anyway),
                    hasTangemIcon = true,
                    onClick = {},
                ),
                onClose = {},
            ),
        )
    }
}