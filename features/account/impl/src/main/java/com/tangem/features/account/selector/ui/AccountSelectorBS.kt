package com.tangem.features.account.selector.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.account.impl.R
import com.tangem.features.account.selector.entity.AccountSelectorUM

@Composable
internal fun AccountSelectorBS(state: AccountSelectorUM, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        scrollableContent = false,
        containerColor = TangemTheme.colors.background.secondary,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.common_choose_account),
                startIconRes = R.drawable.ic_back_24,
                onStartClick = onDismiss,
            )
        },
        content = {
            AccountSelectorContent(
                state = state,
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = modifier.padding(horizontal = 16.dp),
            )
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(AccountSelectorPreviewStateProvider::class) params: AccountSelectorUM) {
    TangemThemePreview {
        AccountSelectorBS(
            state = params,
            onDismiss = {},
            modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        )
    }
}