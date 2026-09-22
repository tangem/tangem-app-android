package com.tangem.features.tangempay.eligibility.country

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun ResidencySearchFooter(state: TangemSearch.State, modifier: Modifier = Modifier) {
    val navigationBarInset = WindowInsets.navigationBars
        .exclude(WindowInsets.ime)
        .asPaddingValues()
        .calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime),
        contentAlignment = Alignment.BottomCenter,
    ) {
        TangemFade(
            position = TangemFade.Position.Bottom,
            variant = TangemFade.Variant.Hard,
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp + navigationBarInset),
        )
        TangemSearch(
            state = state,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp + navigationBarInset),
        )
    }
}

@Preview(widthDp = 402, heightDp = 180, showBackground = true)
@Preview(widthDp = 402, heightDp = 180, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ResidencySearchFooter() {
    TangemThemePreviewRedesign {
        PreviewContainer {
            ResidencySearchFooter(state = previewSearchState(query = ""))
        }
    }
}

@Preview(widthDp = 402, heightDp = 180, showBackground = true)
@Preview(widthDp = 402, heightDp = 180, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ResidencySearchFooter_WithQuery() {
    TangemThemePreviewRedesign {
        PreviewContainer {
            ResidencySearchFooter(state = previewSearchState(query = "United"))
        }
    }
}

@Composable
private fun PreviewContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary),
        contentAlignment = Alignment.BottomCenter,
    ) {
        content()
    }
}

private fun previewSearchState(query: String) = TangemSearch.State(
    placeholderText = resourceReference(R.string.common_search),
    query = query,
    onQueryChange = {},
    isActive = query.isNotEmpty(),
    onActiveChange = {},
)