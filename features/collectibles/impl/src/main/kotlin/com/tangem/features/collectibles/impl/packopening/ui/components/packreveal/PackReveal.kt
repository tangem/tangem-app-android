package com.tangem.features.collectibles.impl.packopening.ui.components.packreveal

import android.view.SurfaceView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun PackReveal(
    onSurfaceAttach: (SurfaceView) -> Unit,
    onSurfaceDetach: (SurfaceView) -> Unit,
    isTapEnabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> SurfaceView(context).also(onSurfaceAttach) },
            onRelease = onSurfaceDetach,
        )

        if (isTapEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(onTap) { detectTapGestures { onTap() } },
            )
        }
    }
}