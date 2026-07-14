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

private var _ic_arrow_split_top_28: ImageVector? = null

val Icons.ic_arrow_split_top_28: ImageVector
    get() {
        if (_ic_arrow_split_top_28 != null) return _ic_arrow_split_top_28!!
        _ic_arrow_split_top_28 = ImageVector.Builder(
            name = "ic_arrow_split_top_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.32449 10.3058C4.69291 11.115 3.85876 10.8175 3.56085 9.78216L2.05937 4.62944C1.82104 3.80833 2.32153 3.16573 3.17952 3.20143L8.55386 3.37993C9.62634 3.41563 10.103 4.16533 9.48334 4.96264L8.41086 6.33114L8.45852 6.35494C10.6512 7.80675 13.3324 10.8294 13.9759 12.8286H14.0235C14.6551 10.8413 17.3482 7.80675 19.5409 6.35494L19.6004 6.31924L18.5041 4.80794C17.9083 3.98683 18.3969 3.24903 19.4813 3.26093L24.8556 3.23713C25.7136 3.23713 26.1903 3.90353 25.9281 4.72464L24.2598 9.81786C23.938 10.8413 23.0801 11.115 22.4723 10.2701L21.3164 8.67545L21.0543 8.84205C18.659 10.3653 15.4416 14.1138 15.4416 16.8389V23.2054C15.4416 24.2407 14.9292 24.8 13.9997 24.8C13.0702 24.8 12.5697 24.2407 12.5697 23.2054V16.8389C12.5697 14.1138 9.32843 10.3653 6.94513 8.84205L6.62338 8.63975L5.32449 10.3058Z"),
            )
        }.build()
        return _ic_arrow_split_top_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowSplitTop28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_split_top_28,
        contentDescription = null,
    )
}