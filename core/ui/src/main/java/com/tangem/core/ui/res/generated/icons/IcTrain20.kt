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

private var _ic_train_20: ImageVector? = null

val Icons.ic_train_20: ImageVector
    get() {
        if (_ic_train_20 != null) return _ic_train_20!!
        _ic_train_20 = ImageVector.Builder(
            name = "ic_train_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.36914 11.8584C6.57726 11.5003 7.0364 11.3788 7.39453 11.5869L8.14453 12.0225C8.50236 12.2307 8.62406 12.6898 8.41602 13.0479C8.20787 13.4057 7.74862 13.5272 7.39062 13.3193L6.64062 12.8838C6.28261 12.6757 6.16129 12.2165 6.36914 11.8584Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.6055 11.5869C12.9636 11.3788 13.4227 11.5003 13.6309 11.8584C13.8388 12.2165 13.7174 12.6757 13.3594 12.8838L12.6094 13.3193C12.2514 13.527 11.7921 13.4057 11.584 13.0479C11.376 12.6899 11.4978 12.2308 11.8555 12.0225L12.6055 11.5869Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.25 2C12.6641 2.00008 13 2.33583 13 2.75C12.9999 3.16408 12.6641 3.49992 12.25 3.5H11.875V4.29004H12.25C14.3332 4.29006 16 6.00511 16 8.09277V12.2363C15.9998 13.207 15.7377 13.975 15.2305 14.6172C14.9584 14.9616 14.6256 15.2531 14.2607 15.5176L15.127 16.8389C15.354 17.1853 15.2576 17.6509 14.9111 17.8779C14.5647 18.1048 14.099 18.0075 13.8721 17.6611L12.9883 16.3115L12.6094 16.5322C11.0937 17.4131 9.25341 17.4682 7.69727 16.6973L7.39062 16.5322L7.01074 16.3115L6.12695 17.6611C5.89994 18.0075 5.4353 18.1049 5.08887 17.8779C4.74256 17.6509 4.64612 17.1853 4.87305 16.8389L5.73828 15.5166C5.37382 15.2523 5.04132 14.9612 4.76953 14.6172C4.26227 13.975 4.00017 13.207 4 12.2363V8.09277C4 6.0051 5.6668 4.29004 7.75 4.29004H8.125V3.5H7.75C7.33585 3.5 7.0001 3.16413 7 2.75C7 2.33579 7.33579 2 7.75 2H12.25ZM5.5 12.2363C5.50016 12.9077 5.67074 13.3386 5.94629 13.6875C6.24524 14.0658 6.70745 14.4003 7.39453 14.7998L8.14355 15.2354C9.29403 15.9039 10.705 15.9039 11.8555 15.2354L12.6055 14.7998C13.2926 14.4003 13.7548 14.0658 14.0537 13.6875C14.3292 13.3386 14.4998 12.9076 14.5 12.2363V10.3691H5.5V12.2363ZM7.75 5.79004C6.51972 5.79004 5.5 6.80881 5.5 8.09277V8.86914H14.5V8.09277C14.5 6.80882 13.4803 5.79006 12.25 5.79004H7.75ZM9.625 4.29004H10.375V3.5H9.625V4.29004Z"),
            )
        }.build()
        return _ic_train_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTrain20Preview() {
    Icon(
        imageVector = Icons.ic_train_20,
        contentDescription = null,
    )
}