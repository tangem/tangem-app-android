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

private var _ic_sack_20: ImageVector? = null

val Icons.ic_sack_20: ImageVector
    get() {
        if (_ic_sack_20 != null) return _ic_sack_20!!
        _ic_sack_20 = ImageVector.Builder(
            name = "ic_sack_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7046 2C13.2469 2.00017 13.7505 2.28292 14.0327 2.74609C14.3147 3.20939 14.3352 3.78657 14.0864 4.26855L13.9096 4.60938C14.4255 4.88537 14.7768 5.42981 14.7768 6.05566V7.58301C14.7767 7.78217 14.6966 7.96144 14.5698 8.0957L15.7895 9.47461C17.2997 11.1809 17.6217 13.6345 16.603 15.6729L16.5376 15.8037C15.8651 17.1493 14.4899 17.9998 12.9858 18H7.02389C5.51973 17.9999 4.1447 17.1493 3.47213 15.8037L3.39694 15.6543C2.38345 13.6266 2.69655 11.1867 4.18893 9.48047L5.41452 8.07812C5.2972 7.94581 5.22421 7.77369 5.22409 7.58301V6.05566C5.22409 5.43012 5.57391 4.8855 6.08932 4.60938L5.91354 4.26855C5.66483 3.78653 5.68512 3.20936 5.96725 2.74609C6.24952 2.28274 6.75376 2 7.29635 2H12.7046ZM5.31784 10.4678C4.22642 11.7156 3.99753 13.5003 4.73873 14.9834L4.81393 15.1328C5.23255 15.9703 6.0882 16.4999 7.02389 16.5H12.9858C13.9214 16.4998 14.7772 15.9702 15.1958 15.1328L15.2612 15.002C16.0062 13.5111 15.7709 11.7166 14.6665 10.4688L12.7758 8.33301H7.186L5.31784 10.4678ZM6.86276 5.91699C6.78626 5.91699 6.72409 5.97916 6.72409 6.05566V6.83301H13.2768V6.05566C13.2768 5.97933 13.2144 5.91726 13.1381 5.91699H6.86276ZM7.29635 3.5C7.27711 3.5 7.25855 3.50993 7.2485 3.52637C7.23847 3.54284 7.2377 3.56392 7.24655 3.58105L7.67916 4.41699H12.3217L12.7534 3.58105C12.7621 3.56405 12.7612 3.54277 12.7514 3.52637C12.7415 3.51019 12.7235 3.50017 12.7046 3.5H7.29635Z"),
            )
        }.build()
        return _ic_sack_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSack20Preview() {
    Icon(
        imageVector = Icons.ic_sack_20,
        contentDescription = null,
    )
}