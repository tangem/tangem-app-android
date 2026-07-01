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

private var _ic_eye_20: ImageVector? = null

val Icons.ic_eye_20: ImageVector
    get() {
        if (_ic_eye_20 != null) return _ic_eye_20!!
        _ic_eye_20 = ImageVector.Builder(
            name = "ic_eye_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99962 6.83555C10.839 6.83559 11.6444 7.16884 12.2379 7.76231C12.8314 8.35583 13.1646 9.16124 13.1647 10.0006C13.1645 10.8398 12.8313 11.6454 12.2379 12.2389C11.6444 12.8322 10.8388 13.1656 9.99962 13.1656C9.16037 13.1655 8.3548 12.8323 7.76134 12.2389C7.16794 11.6454 6.8347 10.8398 6.83458 10.0006C6.83465 9.16142 7.16808 8.35579 7.76134 7.76231C8.35479 7.16894 9.16043 6.83569 9.99962 6.83555ZM9.99962 8.33555C9.55825 8.33569 9.13403 8.51079 8.82189 8.82286C8.50993 9.13504 8.33465 9.55924 8.33458 10.0006C8.33469 10.442 8.50979 10.8662 8.82189 11.1783C9.13404 11.4905 9.5582 11.6655 9.99962 11.6656C10.441 11.6656 10.8652 11.4903 11.1774 11.1783C11.4894 10.8662 11.6645 10.442 11.6647 10.0006C11.6646 9.55907 11.4896 9.13507 11.1774 8.82286C10.8651 8.51069 10.4411 8.33559 9.99962 8.33555Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.98693 3.61192C13.0598 3.5574 15.9063 5.19236 17.4127 7.85313L17.5543 8.11387L17.5621 8.12852C18.1439 9.3082 18.144 10.692 17.5621 11.8717L17.5592 11.8766C16.8674 13.2538 15.7894 14.3975 14.4606 15.1695C13.1194 15.9649 11.5835 16.3884 10.0123 16.3863L10.0133 16.3873C6.84137 16.4437 3.91059 14.7004 2.44591 11.8863C2.44349 11.8817 2.44043 11.8764 2.4381 11.8717C1.85615 10.692 1.85635 9.30826 2.4381 8.12852L2.44005 8.12364C3.13185 6.74614 4.21051 5.60182 5.53966 4.82969C6.88056 4.03459 8.41612 3.60994 9.98693 3.61192ZM9.99767 5.11192C8.78945 5.10828 7.60604 5.41011 6.55333 5.97911C6.53749 5.98914 6.52132 6.00041 6.50451 6.00938C6.43203 6.0479 6.35943 6.08737 6.28868 6.12852C6.06076 6.26448 5.83868 6.41321 5.6256 6.57481C5.62053 6.5786 5.61412 6.58192 5.609 6.58555C4.84613 7.17119 4.21933 7.92355 3.78283 8.79063L3.7838 8.79161C3.4083 9.55302 3.40825 10.4462 3.7838 11.2076C4.95057 13.4369 7.23999 14.8435 9.74376 14.8873H10.0026C11.2033 14.8909 12.3795 14.5931 13.4274 14.0309C13.4489 14.0164 13.4723 14.0033 13.4957 13.9908C13.5615 13.9559 13.6267 13.9186 13.691 13.8814C13.9259 13.7423 14.1554 13.5907 14.3746 13.4244C14.3799 13.4205 14.3859 13.4165 14.3912 13.4127C15.154 12.827 15.7801 12.0747 16.2164 11.2076C16.5919 10.4462 16.5919 9.55296 16.2164 8.79161C15.0496 6.56274 12.7599 5.15574 10.2565 5.11192H9.99767Z"),
            )
        }.build()
        return _ic_eye_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcEye20Preview() {
    Icon(
        imageVector = Icons.ic_eye_20,
        contentDescription = null,
    )
}