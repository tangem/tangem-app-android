@file:Suppress("TooManyFunctions")

package com.tangem.core.ui.components

import androidx.annotation.DimenRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VerticalSpacer(@DimenRes spaceResId: Int) {
    Spacer(modifier = Modifier.height(dimensionResource(id = spaceResId)))
}

@Composable
fun HorizontalSpacer(@DimenRes spaceResId: Int) {
    Spacer(modifier = Modifier.width(dimensionResource(id = spaceResId)))
}

// region Horizontal
@Composable
fun SpacerH(height: Dp, modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(height))
}

@Composable
fun SpacerH2(modifier: Modifier = Modifier) {
    SpacerH(2.dp, modifier)
}

@Composable
fun SpacerH4(modifier: Modifier = Modifier) {
    SpacerH(4.dp, modifier)
}

@Composable
fun SpacerH8(modifier: Modifier = Modifier) {
    SpacerH(8.dp, modifier)
}

@Composable
fun SpacerH10(modifier: Modifier = Modifier) {
    SpacerH(10.dp, modifier)
}

@Composable
fun SpacerH12(modifier: Modifier = Modifier) {
    SpacerH(12.dp, modifier)
}

@Composable
fun SpacerH16(modifier: Modifier = Modifier) {
    SpacerH(16.dp, modifier)
}

@Composable
fun SpacerH24(modifier: Modifier = Modifier) {
    SpacerH(24.dp, modifier)
}

@Composable
fun SpacerH28(modifier: Modifier = Modifier) {
    SpacerH(28.dp, modifier)
}

@Composable
fun SpacerH32(modifier: Modifier = Modifier) {
    SpacerH(32.dp, modifier)
}

@Composable
fun SpacerH50(modifier: Modifier = Modifier) {
    SpacerH(50.dp, modifier)
}

// TODO: Refactor with context receivers
@Composable
fun ColumnScope.SpacerHMax(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.weight(1f))
}

// TODO: Refactor with context receivers
@Suppress("MagicNumber")
@Composable
fun ColumnScope.SpacerHHalf(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.weight(.5f))
}
// endregion Horizontal

// region Vertical
@Composable
fun SpacerW(width: Dp, modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.width(width))
}

@Composable
fun SpacerW2(modifier: Modifier = Modifier) {
    SpacerW(2.dp, modifier)
}

@Composable
fun SpacerW4(modifier: Modifier = Modifier) {
    SpacerW(4.dp, modifier)
}

@Composable
fun SpacerW6(modifier: Modifier = Modifier) {
    SpacerW(6.dp, modifier)
}

@Composable
fun SpacerW8(modifier: Modifier = Modifier) {
    SpacerW(8.dp, modifier)
}

@Composable
fun SpacerW12(modifier: Modifier = Modifier) {
    SpacerW(12.dp, modifier)
}

@Composable
fun SpacerW16(modifier: Modifier = Modifier) {
    SpacerW(16.dp, modifier)
}

@Composable
fun SpacerW24(modifier: Modifier = Modifier) {
    SpacerW(24.dp, modifier)
}

@Composable
fun SpacerW32(modifier: Modifier = Modifier) {
    SpacerW(32.dp, modifier)
}

// TODO: Refactor with context receivers
@Composable
fun RowScope.SpacerWMax(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.weight(1f))
}
// endregion Vertical

// region Size
@Composable
fun SpacerS(size: Dp, modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.size(size))
}

@Composable
fun SpacerS4(modifier: Modifier = Modifier) {
    SpacerS(4.dp, modifier)
}

@Composable
fun SpacerS8(modifier: Modifier = Modifier) {
    SpacerS(8.dp, modifier)
}

@Composable
fun SpacerS16(modifier: Modifier = Modifier) {
    SpacerS(16.dp, modifier)
}

@Composable
fun SpacerS24(modifier: Modifier = Modifier) {
    SpacerS(24.dp, modifier)
}

@Composable
fun SpacerS32(modifier: Modifier = Modifier) {
    SpacerS(32.dp, modifier)
}
// endregion Size
