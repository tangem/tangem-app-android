package com.tangem.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.valentinilk.shimmer.shimmer

/**
 * Rectangle shimmer item with rounded shape from DS
 */
@Composable
fun RectangleShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shimmer()
            .background(
                color = TangemTheme.colors.button.secondary,
                shape = RoundedCornerShape(TangemTheme.dimens.radius6),
            ),
    )
}

/**
 * Circle shimmer item
 * Size should be set in modifier
 */
@Composable
fun CircleShimmer(modifier: Modifier = Modifier) {
    Box(modifier = modifier.shimmer()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = TangemTheme.colors.button.secondary,
                    shape = CircleShape,
                ),
        )
    }
}

// region preview

@Composable
private fun ShimmersPreview() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing18),
    ) {
        RectangleShimmer(
            modifier = Modifier.size(
                width = TangemTheme.dimens.size72,
                height = TangemTheme.dimens.size12,
            ),
        )
        CircleShimmer(modifier = Modifier.size(size = TangemTheme.dimens.size42))
    }
}

@Preview(showBackground = true)
@Composable
private fun Shimmers_InLightTheme() {
    TangemTheme(isDark = false) {
        ShimmersPreview()
    }
}

@Preview(showBackground = true)
@Composable
private fun Shimmers_InDarkTheme() {
    TangemTheme(isDark = true) {
        ShimmersPreview()
    }
}

// endregion preview