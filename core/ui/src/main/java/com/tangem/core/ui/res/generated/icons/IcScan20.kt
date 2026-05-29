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

private var _ic_scan_20: ImageVector? = null

val Icons.ic_scan_20: ImageVector
    get() {
        if (_ic_scan_20 != null) return _ic_scan_20!!
        _ic_scan_20 = ImageVector.Builder(
            name = "ic_scan_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.4238 9.6377C17.8378 9.6377 18.1735 9.97376 18.1738 10.3877C18.1738 10.8019 17.838 11.1377 17.4238 11.1377H17.002V13.123C17.002 15.2636 15.2665 16.9989 13.126 16.999H12.7354C12.3213 16.9988 11.9854 16.6631 11.9854 16.249C11.9857 15.8352 12.3215 15.4992 12.7354 15.499H13.126C14.4381 15.4989 15.502 14.4352 15.502 13.123V11.1377H4.49707V13.123C4.49707 14.4352 5.56192 15.4988 6.87402 15.499H7.26465C7.67849 15.4992 8.0143 15.8352 8.01465 16.249C8.01465 16.6631 7.6787 16.9988 7.26465 16.999H6.87402C4.7335 16.9988 2.99707 15.2636 2.99707 13.123V11.1377H2.5752C2.16115 11.1375 1.8252 10.8018 1.8252 10.3877C1.82552 9.97388 2.16135 9.63789 2.5752 9.6377H17.4238Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.26465 2.99512C7.67861 2.99531 8.0145 3.33115 8.01465 3.74512C8.01465 4.15921 7.6787 4.49493 7.26465 4.49512H6.87402C5.56205 4.49531 4.49727 5.55912 4.49707 6.87109V7.26172C4.49707 7.67593 4.16128 8.01172 3.74707 8.01172C3.33321 8.01131 2.99707 7.67568 2.99707 7.26172V6.87109C2.99727 4.73069 4.73362 2.99531 6.87402 2.99512H7.26465Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.126 2.99512C15.2664 2.99528 17.0018 4.73067 17.002 6.87109V7.26172C17.002 7.67586 16.6661 8.0116 16.252 8.01172C15.8379 8.01152 15.502 7.67581 15.502 7.26172V6.87109C15.5018 5.5591 14.438 4.49528 13.126 4.49512H12.7354C12.3213 4.49492 11.9854 4.15921 11.9854 3.74512C11.9855 3.33115 12.3214 2.99531 12.7354 2.99512H13.126Z"),
            )
        }.build()
        return _ic_scan_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcScan20Preview() {
    Icon(
        imageVector = Icons.ic_scan_20,
        contentDescription = null,
    )
}