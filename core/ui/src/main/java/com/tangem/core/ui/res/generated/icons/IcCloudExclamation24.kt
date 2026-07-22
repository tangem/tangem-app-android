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

private var _ic_cloud_exclamation_24: ImageVector? = null

val Icons.ic_cloud_exclamation_24: ImageVector
    get() {
        if (_ic_cloud_exclamation_24 != null) return _ic_cloud_exclamation_24!!
        _ic_cloud_exclamation_24 = ImageVector.Builder(
            name = "ic_cloud_exclamation_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.125 17.1061C12.5311 17.151 12.887 17.4096 13.0479 17.7926C13.2257 18.2168 13.1308 18.7075 12.8066 19.0338C12.4824 19.36 11.9926 19.4579 11.5674 19.2828C11.1422 19.1075 10.8646 18.6928 10.8643 18.233V18.2281C10.8662 17.6046 11.3708 17.0995 11.9932 17.0963L11.9971 17.0973L12.0029 17.0963C12.0442 17.0965 12.0851 17.1011 12.125 17.1061Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 4.63147C15.2272 4.63171 17.8952 7.02079 18.335 10.1266C20.4272 10.5585 22.0007 12.4114 22.001 14.6315C22.001 17.1719 19.9408 19.2318 17.4004 19.2321H15.6006C15.0483 19.2321 14.6006 18.7843 14.6006 18.2321C14.6007 17.6798 15.0483 17.2321 15.6006 17.2321H17.4004C18.8362 17.2318 20.001 16.0673 20.001 14.6315C20.0007 13.1958 18.836 12.0321 17.4004 12.0319C16.8483 12.0318 16.4005 11.584 16.4004 11.0319C16.4003 8.6019 14.4299 6.63173 12 6.63147C10.1174 6.63275 8.44473 7.8324 7.83887 9.61487C7.71672 9.9743 7.40059 10.2332 7.02441 10.2838C5.29538 10.5151 4.00318 11.9896 4 13.734C4.00119 15.666 5.56772 17.232 7.5 17.2321H8.40039C8.95233 17.2324 9.40032 17.68 9.40039 18.2321C9.40035 18.7841 8.95235 19.2317 8.40039 19.2321H7.5C4.4625 19.232 1.99916 16.7695 1.99902 13.7321V13.7301L2.00684 13.4742C2.12336 11.0444 3.82809 8.99404 6.16309 8.41565C7.18456 6.13176 9.46143 4.63271 12 4.63147Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.001 11.1764C12.5528 11.1769 13.001 11.6244 13.001 12.1764V14.8981C13.001 15.45 12.5528 15.8975 12.001 15.8981C11.4487 15.8981 11.001 15.4504 11.001 14.8981V12.1764C11.001 11.6241 11.4487 11.1764 12.001 11.1764Z"),
            )
        }.build()
        return _ic_cloud_exclamation_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloudExclamation24Preview() {
    Icon(
        imageVector = Icons.ic_cloud_exclamation_24,
        contentDescription = null,
    )
}