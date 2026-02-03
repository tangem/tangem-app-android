package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.clore

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R as CoreR
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tokendetails.impl.R

@Composable
internal fun CloreMigrationBottomSheet(config: TangemBottomSheetConfig) {
    TangemModalBottomSheet<CloreMigrationBottomSheetConfig>(
        config = config,
        title = {
            TangemModalBottomSheetTitle(
                endIconRes = CoreR.drawable.ic_close_24,
                onEndClick = config.onDismissRequest,
            )
        },
    ) { content: CloreMigrationBottomSheetConfig ->
        CloreMigrationBottomSheetContent(content = content)
    }
}

@Composable
private fun CloreMigrationBottomSheetContent(content: CloreMigrationBottomSheetConfig) {
    Column(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.warning_clore_migration_sheet_title),
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.h3,
        )

        Text(
            text = stringResourceSafe(R.string.warning_clore_migration_sheet_description),
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.body2,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        MessageField(
            placeholder = stringResourceSafe(R.string.warning_clore_migration_message_label),
            value = content.message,
            onValueChange = content.onMessageChange,
            buttonText = stringResourceSafe(R.string.warning_clore_migration_sign_button),
            onButtonClick = content.onSignClick,
            isButtonEnabled = content.message.isNotBlank() && !content.isSigningInProgress,
            isLoading = content.isSigningInProgress,
        )

        SignatureField(
            placeholder = stringResourceSafe(R.string.warning_clore_migration_signature_label),
            value = content.signature,
            onCopyClick = content.onCopyClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        PrimaryButton(
            text = stringResourceSafe(R.string.warning_clore_migration_open_portal_button),
            onClick = content.onOpenPortalClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun MessageField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    buttonText: String,
    onButtonClick: () -> Unit,
    isButtonEnabled: Boolean,
    isLoading: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = TangemTheme.colors.stroke.primary,
                shape = TangemTheme.shapes.roundedCornersSmall,
            )
            .heightIn(min = 56.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SimpleTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = TextReference.Str(placeholder),
            modifier = Modifier.weight(1f),
            singleLine = false,
        )

        Spacer(modifier = Modifier.width(8.dp))

        SecondarySmallButton(
            config = SmallButtonConfig(
                text = TextReference.Str(buttonText),
                onClick = onButtonClick,
                isEnabled = isButtonEnabled,
                isLoading = isLoading,
            ),
        )
    }
}

@Suppress("FunctionSignature")
@Composable
private fun SignatureField(placeholder: String, value: String, onCopyClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = TangemTheme.colors.stroke.primary,
                shape = TangemTheme.shapes.roundedCornersSmall,
            )
            .heightIn(min = 56.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SimpleTextField(
            value = value,
            onValueChange = {},
            placeholder = TextReference.Str(placeholder),
            modifier = Modifier.weight(1f),
            readOnly = true,
            singleLine = false,
        )

        Spacer(modifier = Modifier.width(8.dp))

        SecondarySmallButton(
            config = SmallButtonConfig(
                text = TextReference.Res(R.string.warning_clore_migration_copy_button),
                onClick = onCopyClick,
                isEnabled = value.isNotBlank(),
            ),
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_CloreMigrationBottomSheet() {
    TangemThemePreview {
        val config = TangemBottomSheetConfig(
            isShown = true,
            content = CloreMigrationBottomSheetConfig(
                message = "Claim request for CLORE tokens to Ethereum address " +
                    "0x742d35Cc6634C0532925a3b844Bc9e7595f60126 from CEsMERNgBVPo9Dkc99pRDMPD3mxwPMxHZi",
                signature = "H4sIAAAAAAAEAGNgGAWjYBSMglEwCkYBHQEA",
                isSigningInProgress = false,
                onMessageChange = {},
                onSignClick = {},
                onCopyClick = {},
                onOpenPortalClick = {},
            ),
            onDismissRequest = {},
        )

        CloreMigrationBottomSheet(config)
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
private fun Preview_CloreMigrationBottomSheet_Empty() {
    TangemThemePreview {
        val config = TangemBottomSheetConfig(
            isShown = true,
            content = CloreMigrationBottomSheetConfig(
                message = "",
                signature = "",
                isSigningInProgress = false,
                onMessageChange = {},
                onSignClick = {},
                onCopyClick = {},
                onOpenPortalClick = {},
            ),
            onDismissRequest = {},
        )

        CloreMigrationBottomSheet(config)
    }
}
// endregion Preview