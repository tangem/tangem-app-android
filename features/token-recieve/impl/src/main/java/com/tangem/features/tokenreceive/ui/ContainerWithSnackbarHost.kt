package com.tangem.features.tokenreceive.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.snackbar.CopiedTextSnackbarHost

@Composable
internal fun ContainerWithSnackbarHost(snackbarHostState: SnackbarHostState, content: @Composable () -> Unit) {
    Box {
        content()
        CopiedTextSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        )
    }
}