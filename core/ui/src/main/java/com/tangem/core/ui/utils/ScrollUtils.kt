package com.tangem.core.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

fun Modifier.disableNestedScroll(): Modifier = nestedScroll(DisableParentConnection)

private object DisableParentConnection : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return available.copy(x = 0f)
    }
}