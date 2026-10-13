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

private var _ic_hanger_16: ImageVector? = null

val Icons.ic_hanger_16: ImageVector
    get() {
        if (_ic_hanger_16 != null) return _ic_hanger_16!!
        _ic_hanger_16 = ImageVector.Builder(
            name = "ic_hanger_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.00098 3C8.88288 3.0005 9.59766 3.71563 9.59766 4.59766C9.59746 5.31409 9.12155 5.68828 8.8418 5.87695L8.84082 5.87793C8.8021 5.90395 8.77088 5.92783 8.74512 5.94824C9.07947 5.99409 9.41077 6.06994 9.73438 6.17773L11.7998 6.86621L11.9307 6.91602C12.575 7.18901 13 7.82283 13 8.53125C12.9999 9.3989 12.3699 10.1176 11.542 10.2588V11.3789C11.5419 12.2736 10.8156 12.9997 9.9209 13H6.08008C5.18519 13 4.45912 12.2738 4.45898 11.3789V10.2646C3.63265 10.1352 3 9.4222 3 8.55957V8.53613C3.00005 7.77744 3.4819 7.10539 4.2002 6.86621L6.26465 6.17773C6.63202 6.05534 7.00918 5.97319 7.38965 5.93066C7.46975 5.33767 7.8857 5.01311 8.14355 4.83984C8.24481 4.77148 8.29429 4.72552 8.32031 4.69043C8.33095 4.67601 8.33658 4.66483 8.33984 4.65527C8.34302 4.64592 8.34761 4.62837 8.34766 4.59766C8.34766 4.40599 8.19253 4.2505 8.00098 4.25C7.809 4.25 7.65332 4.40568 7.65332 4.59766C7.65308 4.94247 7.37313 5.22239 7.02832 5.22266C6.68329 5.22266 6.40356 4.94263 6.40332 4.59766C6.40332 3.71533 7.11865 3 8.00098 3ZM5.70898 11.3789C5.70912 11.5834 5.87554 11.75 6.08008 11.75H9.9209C10.1252 11.7497 10.2919 11.5832 10.292 11.3789V9.59766H5.70898V11.3789ZM9.33984 7.36426C8.47013 7.0745 7.52988 7.07451 6.66016 7.36426L4.59473 8.05176C4.39047 8.11978 4.25005 8.31202 4.25 8.53613V8.55957C4.25 8.76387 4.37919 8.93636 4.55957 9.00488C4.73657 8.61712 5.128 8.34774 5.58203 8.34766H10.4199C10.8706 8.348 11.2572 8.61464 11.4355 8.99805C11.6197 8.92285 11.7499 8.74284 11.75 8.53125C11.7499 8.31379 11.6113 8.12042 11.4053 8.05176L9.33984 7.36426Z"),
            )
        }.build()
        return _ic_hanger_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHanger16Preview() {
    Icon(
        imageVector = Icons.ic_hanger_16,
        contentDescription = null,
    )
}