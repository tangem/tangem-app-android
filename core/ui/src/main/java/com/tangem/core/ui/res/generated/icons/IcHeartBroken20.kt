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

private var _ic_heart_broken_20: ImageVector? = null

val Icons.ic_heart_broken_20: ImageVector
    get() {
        if (_ic_heart_broken_20 != null) return _ic_heart_broken_20!!
        _ic_heart_broken_20 = ImageVector.Builder(
            name = "ic_heart_broken_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7783 3.5C15.6524 3.50026 17.5049 6.06504 17.5049 8.39062C17.5048 9.61013 17.0055 10.7261 16.3281 11.6816C15.6496 12.6388 14.7588 13.4824 13.8906 14.1748C13.0193 14.8697 12.1472 15.4304 11.4863 15.8174C11.1556 16.011 10.8741 16.1632 10.668 16.2686C10.5656 16.3208 10.478 16.3635 10.4102 16.3945C10.3773 16.4096 10.3423 16.4255 10.3096 16.4385C10.2941 16.4446 10.2699 16.454 10.2422 16.4629C10.2286 16.4673 10.2051 16.4738 10.1768 16.4805C10.1593 16.4845 10.0905 16.5009 10.002 16.501C9.91386 16.5009 9.84496 16.4846 9.82715 16.4805C9.79927 16.4739 9.77634 16.4673 9.7627 16.4629C9.73516 16.454 9.71083 16.4446 9.69531 16.4385C9.66266 16.4255 9.62759 16.4096 9.59473 16.3945C9.52687 16.3635 9.4384 16.3209 9.33594 16.2686C9.12976 16.1632 8.84831 16.011 8.51758 15.8174C7.85662 15.4304 6.98469 14.8698 6.11328 14.1748C5.2452 13.4825 4.35526 12.6387 3.67676 11.6816C2.99938 10.7261 2.50007 9.61017 2.5 8.39062C2.5 6.06492 4.35227 3.5 7.22656 3.5C8.44824 3.50012 9.36128 3.94253 10.002 4.45215C10.6427 3.9424 11.5563 3.5 12.7783 3.5ZM7.22656 5C5.34429 5 4 6.71969 4 8.39062C4.00007 9.19437 4.33059 10.0106 4.90039 10.8145C5.46908 11.6165 6.24419 12.3602 7.04883 13.002C7.85025 13.6411 8.65918 14.1617 9.27539 14.5225C9.33735 14.5587 9.39842 14.5912 9.45605 14.624L10.2861 11.9736L8.68652 10.1328C8.44108 9.85059 8.44106 9.43065 8.68652 9.14844L10.2256 7.37793L9.54492 6.07324C9.50908 6.04571 9.47503 6.01552 9.44434 5.98145C9.02594 5.51667 8.32236 5.00016 7.22656 5ZM12.7783 5C11.9908 5 11.4067 5.26828 10.9844 5.59082L11.792 7.1377C11.9347 7.41113 11.8957 7.74382 11.6934 7.97656L10.2451 9.64062L11.6934 11.3057C11.8641 11.5021 11.9214 11.773 11.8438 12.0215L11.1377 14.2744C11.6821 13.9377 12.3221 13.5076 12.9561 13.002C13.7607 12.3602 14.5359 11.6166 15.1045 10.8145C15.6742 10.0107 16.0048 9.19434 16.0049 8.39062C16.0049 6.71982 14.6604 5.00027 12.7783 5Z"),
            )
        }.build()
        return _ic_heart_broken_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeartBroken20Preview() {
    Icon(
        imageVector = Icons.ic_heart_broken_20,
        contentDescription = null,
    )
}