package com.tangem.feature.tester.presentation.news.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.news.state.NewsUM
import kotlinx.collections.immutable.ImmutableSet

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NewsScreen(state: NewsUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
    ) {
        stickyHeader { AppBar(onBackClick = state.onBackClick) }

        NewsButtons(buttons = state.buttons, onButtonClick = state.onButtonClick)
    }
}

@Composable
private fun AppBar(onBackClick: () -> Unit) {
    AppBarWithBackButton(
        onBackClick = onBackClick,
        text = stringResourceSafe(id = R.string.news),
        containerColor = TangemTheme.colors.background.primary,
    )
}

@Suppress("FunctionNaming")
private fun LazyListScope.NewsButtons(
    buttons: ImmutableSet<NewsUM.ButtonUM>,
    onButtonClick: (NewsUM.ButtonUM) -> Unit,
) {
    items(buttons.toList()) { button ->
        PrimaryButton(
            text = stringResourceSafe(button.textResId),
            onClick = { onButtonClick(button) },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
        )
    }
}