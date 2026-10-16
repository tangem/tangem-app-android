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

private var _ic_bone_16: ImageVector? = null

val Icons.ic_bone_16: ImageVector
    get() {
        if (_ic_bone_16 != null) return _ic_bone_16!!
        _ic_bone_16 = ImageVector.Builder(
            name = "ic_bone_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3 9.33301C3.00024 8.26894 3.86248 7.4055 4.92676 7.40527H5.91797L7.40723 5.91211V4.92773C7.40724 3.86334 8.27036 3 9.33496 3C10.337 3.00007 11.1592 3.76482 11.2529 4.74219C11.6983 4.78396 12.1177 4.97972 12.4365 5.29883C12.7979 5.66063 13.0003 6.1517 13 6.66309C12.9998 7.72719 12.1376 8.59064 11.0732 8.59082H10.083L8.59082 10.083V11.0723C8.59082 12.1358 7.72971 12.9987 6.66602 13C5.66226 12.9999 4.8375 12.2327 4.74609 11.2529C4.30116 11.2109 3.88198 11.0161 3.56348 10.6973C3.20208 10.3355 2.99969 9.84438 3 9.33301ZM8.65723 5.94043C8.65626 6.21542 8.56056 6.48053 8.3877 6.69141L8.30957 6.77832L6.7832 8.30762C6.56113 8.52906 6.25995 8.6538 5.94629 8.65527H4.92676C4.55337 8.6555 4.25024 8.95876 4.25 9.33301C4.24993 9.51286 4.32125 9.68634 4.44824 9.81348C4.57517 9.94054 4.74733 10.0116 4.92676 10.0117H5.36231C5.70738 10.0117 5.98715 10.2917 5.98731 10.6367V11.0713C5.98731 11.4459 6.29045 11.7484 6.66406 11.749C7.03746 11.7486 7.34082 11.4465 7.34082 11.0723V10.0557C7.34193 9.74158 7.46749 9.44005 7.68945 9.21777L9.21484 7.69141C9.43796 7.46727 9.74123 7.34141 10.0576 7.34082H11.0732C11.4467 7.34064 11.7498 7.03736 11.75 6.66309C11.7501 6.48323 11.6788 6.30975 11.5518 6.18262C11.4248 6.05555 11.2527 5.98446 11.0732 5.98438H10.6377C10.2927 5.98432 10.0128 5.70439 10.0127 5.35938V4.92871C10.0127 4.55373 9.70906 4.25007 9.33496 4.25C8.96125 4.25 8.65724 4.55316 8.65723 4.92773V5.94043Z"),
            )
        }.build()
        return _ic_bone_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBone16Preview() {
    Icon(
        imageVector = Icons.ic_bone_16,
        contentDescription = null,
    )
}