package com.tangem.tap.common.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
[REDACTED_AUTHOR]
 */
// ***************************** Horizontal
@Composable
fun SpacerH(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}

@Composable
fun SpacerH4() {
    SpacerH(4.dp)
}

@Composable
fun SpacerH8() {
    SpacerH(8.dp)
}

@Composable
fun SpacerH16() {
    SpacerH(16.dp)
}

@Composable
fun SpacerH24() {
    SpacerH(24.dp)
}

// ***************************** Vertical
@Composable
fun SpacerV(width: Dp) {
    Spacer(modifier = Modifier.width(width))
}

@Composable
fun SpacerV4() {
    SpacerV(4.dp)
}

@Composable
fun SpacerV8() {
    SpacerV(8.dp)
}

@Composable
fun SpacerV16() {
    SpacerV(16.dp)
}

@Composable
fun SpacerV24() {
    SpacerV(24.dp)
}

// ***************************** Size
@Composable
fun SpacerS(size: Dp) {
    Spacer(modifier = Modifier.size(size))
}

@Composable
fun SpacerS4() {
    SpacerS(4.dp)
}

@Composable
fun SpacerS8() {
    SpacerS(8.dp)
}

@Composable
fun SpacerS16() {
    SpacerS(16.dp)
}

@Composable
fun SpacerS24() {
    SpacerS(24.dp)
}