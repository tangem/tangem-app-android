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

private var _ic_rose_16: ImageVector? = null

val Icons.ic_rose_16: ImageVector
    get() {
        if (_ic_rose_16 != null) return _ic_rose_16!!
        _ic_rose_16 = ImageVector.Builder(
            name = "ic_rose_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.48242 3C9.44063 3.00011 10.2392 3.6529 10.4785 4.53418C11.358 4.77703 12 5.5864 12 6.54199C11.9999 7.28007 11.6157 7.92958 11.0361 8.2998V8.48633C11.0359 9.95387 10.0041 11.1821 8.625 11.4727V12.375C8.625 12.7201 8.34512 12.9999 8 13C7.65482 13 7.375 12.7202 7.375 12.375V11.4727C5.99599 11.182 4.96503 9.95381 4.96484 8.48633V7.93457C4.37428 7.4181 4 6.65878 4 5.8125C4 4.26417 5.24679 3 6.79492 3H8.48242ZM6.21777 8.56348C6.25795 9.52895 7.04515 10.291 8.00098 10.291C8.93601 10.2906 9.70905 9.56083 9.7793 8.625H6.79492C6.59684 8.625 6.40401 8.60281 6.21777 8.56348ZM6.79492 4.25C5.94677 4.25 5.25 4.94486 5.25 5.8125C5.25 6.68014 5.94677 7.375 6.79492 7.375H9.92871C10.3775 7.37496 10.7498 7.00695 10.75 6.54199C10.75 6.25875 10.6117 6.01143 10.4023 5.86133C10.0964 6.62432 9.3556 7.1669 8.48242 7.16699H7.07324C6.32408 7.16693 5.72559 6.55531 5.72559 5.8125C5.72559 5.06969 6.32408 4.45807 7.07324 4.45801H8.05176C8.39683 4.45801 8.67658 4.73798 8.67676 5.08301C8.67676 5.42819 8.39694 5.70801 8.05176 5.70801H7.07324C7.02405 5.70807 6.97559 5.75039 6.97559 5.8125C6.97559 5.87461 7.02405 5.91693 7.07324 5.91699H8.48242C8.90334 5.91687 9.25744 5.5928 9.2998 5.16895L9.30371 5.08301L9.2998 4.99707C9.25715 4.57353 8.90312 4.25012 8.48242 4.25H6.79492Z"),
            )
        }.build()
        return _ic_rose_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcRose16Preview() {
    Icon(
        imageVector = Icons.ic_rose_16,
        contentDescription = null,
    )
}