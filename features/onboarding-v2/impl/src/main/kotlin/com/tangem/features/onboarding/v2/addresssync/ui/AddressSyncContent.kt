package com.tangem.features.onboarding.v2.addresssync.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.addresssync.entity.AddressSyncUM

@Composable
internal fun AddressSyncContent(state: AddressSyncUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBarWithBackButton(
            onBackClick = state.onBackClick,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .weight(1f),
        ) {}
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddressSyncContentPreview(@PreviewParameter(PreviewStateProvider::class) state: AddressSyncUM) {
    TangemThemePreview {
        AddressSyncContent(state = state)
    }
}

private class PreviewStateProvider : CollectionPreviewParameterProvider<AddressSyncUM>(
    buildList {
        TODO("Will be implemented during [REDACTED_TASK_KEY]")
    },
)