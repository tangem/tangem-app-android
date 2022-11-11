package com.tangem.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SystemBarsEffect(
    block: SystemUiController.() -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    SideEffect { block(systemUiController) }
}
