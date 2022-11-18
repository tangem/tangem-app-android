package com.tangem.core.ui.components

import androidx.annotation.DimenRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource

@Composable
fun VerticalSpacer(@DimenRes spaceResId: Int) {
    Spacer(modifier = Modifier.height(dimensionResource(id = spaceResId)))
}

@Composable
fun HorizontalSpacer(@DimenRes spaceResId: Int) {
    Spacer(modifier = Modifier.width(dimensionResource(id = spaceResId)))
}