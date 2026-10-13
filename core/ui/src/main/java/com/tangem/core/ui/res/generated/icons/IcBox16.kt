@file:Suppress("all")

package com.tangem.core.ui.res.generated.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _ic_box_16: ImageVector? = null

val Icons.ic_box_16: ImageVector
    get() {
        if (_ic_box_16 != null) return _ic_box_16!!
        _ic_box_16 = ImageVector.Builder(
            name = "ic_box_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.14941 2.22457C7.67605 1.92563 8.32395 1.92563 8.85059 2.22457L12.6416 4.37887C12.9056 4.52876 13.12 4.74305 13.2686 4.99605C13.27 4.99845 13.2721 5.00046 13.2734 5.00289C13.2769 5.00895 13.278 5.0163 13.2812 5.02242C13.4207 5.26967 13.5 5.55141 13.5 5.84566V10.1543C13.4999 10.7633 13.1697 11.3213 12.6416 11.6211L8.85059 13.7753C8.32404 14.0742 7.67596 14.0742 7.14941 13.7753L3.3584 11.6211C2.83117 11.3209 2.50025 10.7633 2.5 10.1543V5.84566C2.5 5.2365 2.83033 4.67869 3.3584 4.37887L5.27734 3.28804C5.28007 3.28665 5.28242 3.28451 5.28516 3.28316L7.14941 2.22457ZM3.75 10.1543C3.75024 10.3066 3.83241 10.4527 3.97461 10.5341L7.375 12.4658V8.36129L3.75 6.30172V10.1543ZM8.625 8.36226V12.4658L12.0244 10.5341C12.1667 10.4534 12.2499 10.3072 12.25 10.1543V6.30269L8.625 8.36226ZM4.39062 5.22945L8 7.27926L9.17188 6.61324L5.56348 4.56246L4.39062 5.22945ZM8.2334 3.31148C8.08945 3.22978 7.91055 3.22978 7.7666 3.31148L6.82812 3.84371L10.4375 5.89449L11.6084 5.22945L8.2334 3.31148Z"),
            )
        }.build()
        return _ic_box_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBox16Preview() {
    Icon(
        imageVector = Icons.ic_box_16,
        contentDescription = null,
    )
}