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

private var _ic_shield_checkmark_20: ImageVector? = null

val Icons.ic_shield_checkmark_20: ImageVector
    get() {
        if (_ic_shield_checkmark_20 != null) return _ic_shield_checkmark_20!!
        _ic_shield_checkmark_20 = ImageVector.Builder(
            name = "ic_shield_checkmark_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.5312 8.41471C11.8547 8.15596 12.3272 8.20845 12.5859 8.5319C12.8445 8.85534 12.7921 9.32789 12.4688 9.58659L9.96875 11.5866C9.69491 11.8055 9.30509 11.8055 9.03125 11.5866L7.53125 10.3864C7.20797 10.1277 7.15555 9.65511 7.41406 9.3317C7.67281 9.0084 8.14535 8.95588 8.46875 9.21452L9.5 10.0387L11.5312 8.41471Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.34961 3.24479C9.40583 2.78691 10.5952 2.78688 11.6514 3.24479L11.6504 3.24577L14.6504 4.5446C15.6833 4.99286 16.2997 6.02086 16.2998 7.09635V9.36295C16.2994 12.9046 13.8113 16.2357 10.3477 17.0602C10.1196 17.1145 9.88142 17.1144 9.65332 17.0602C6.18954 16.2358 3.70057 12.9047 3.7002 9.36295V7.09635C3.70034 6.02198 4.31493 4.99145 5.35059 4.54362L8.34961 3.24479ZM11.0547 4.62077C10.3792 4.3279 9.62183 4.32796 8.94629 4.62077L5.94629 5.92057C5.49527 6.11561 5.20034 6.58064 5.2002 7.09635V9.36295C5.20058 12.2478 7.24813 14.9462 10 15.6012C12.7519 14.9462 14.7994 12.2478 14.7998 9.36295V7.09635C14.7997 6.58084 14.5054 6.11639 14.0547 5.92057L11.0547 4.62077Z"),
            )
        }.build()
        return _ic_shield_checkmark_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShieldCheckmark20Preview() {
    Icon(
        imageVector = Icons.ic_shield_checkmark_20,
        contentDescription = null,
    )
}