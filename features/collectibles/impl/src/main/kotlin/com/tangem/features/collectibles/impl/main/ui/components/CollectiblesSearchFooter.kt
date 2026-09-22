package com.tangem.features.collectibles.impl.main.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.search.TangemSearch

@Composable
internal fun CollectiblesSearchFooter(
    state: TangemSearch.State,
    focusRequester: FocusRequester,
    isShown: Boolean,
    modifier: Modifier = Modifier,
) {
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
        if (isShown) {
            TangemFade(
                position = TangemFade.Position.Bottom,
                variant = TangemFade.Variant.Soft,
                blur = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp + navigationBarInset),
            )

            TangemSearch(
                state = state,
                focusRequester = focusRequester,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp + navigationBarInset),
            )
        }
    }
}

@Composable
internal fun rememberRestingFooterHeight(isSearchActive: Boolean, footerHeight: Int): Int {
    var restingFooterHeight by remember { mutableIntStateOf(0) }
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(isSearchActive, imeBottom, footerHeight) {
        if (!isSearchActive && imeBottom == 0.dp) restingFooterHeight = footerHeight
    }

    return restingFooterHeight
}