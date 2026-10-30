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

private var _ic_bone_20: ImageVector? = null

val Icons.ic_bone_20: ImageVector
    get() {
        if (_ic_bone_20 != null) return _ic_bone_20!!
        _ic_bone_20 = ImageVector.Builder(
            name = "ic_bone_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.29688 5.61133C9.29688 4.19561 10.4232 3.04603 11.8291 3.00488C11.8547 3.00225 11.881 3 11.9072 3C13.3063 3.00024 14.4445 4.10107 14.5117 5.4834C15.1599 5.51375 15.7757 5.782 16.2363 6.24316C16.6645 6.67197 16.9284 7.23418 16.9873 7.83203L17 8.08984C16.9998 9.53139 15.8314 10.7002 14.3896 10.7002H12.9424L12.9189 10.7051C12.9118 10.7081 12.905 10.7131 12.8994 10.7188L10.7178 12.9014C10.7072 12.9122 10.7012 12.9271 10.7012 12.9424V14.3887C10.7012 15.8292 9.53441 16.9982 8.09375 17C6.69235 16.9998 5.55108 15.8953 5.48731 14.5098C4.83952 14.4792 4.22411 14.2128 3.76367 13.752C3.27415 13.2619 2.99958 12.597 3 11.9043C3.00027 10.4628 4.16861 9.29395 5.61035 9.29395H7.05859C7.07347 9.29388 7.08761 9.28743 7.09863 9.27734L9.2793 7.09375C9.29014 7.08289 9.29682 7.06725 9.29688 7.05176V5.61133ZM10.7969 7.05762C10.7954 7.4686 10.6313 7.86246 10.3408 8.15332L8.16016 10.3379C7.86962 10.6276 7.47674 10.7919 7.06641 10.7939H5.61035C4.99752 10.7939 4.5 11.2913 4.5 11.9053C4.49983 12.2002 4.61694 12.4829 4.8252 12.6914C5.03344 12.8998 5.31592 13.0166 5.61035 13.0166H6.23145C6.64553 13.0166 6.98124 13.3526 6.98145 13.7666V14.3877C6.98145 15.0018 7.47896 15.4983 8.0918 15.499C8.7041 15.4983 9.20117 15.0021 9.20117 14.3887V12.9365C9.20267 12.5256 9.36681 12.1317 9.65723 11.8408L11.834 9.66211C12.1262 9.36724 12.5242 9.20097 12.9395 9.2002H14.3896C15.0023 9.2002 15.4998 8.7036 15.5 8.08984C15.5001 7.79499 15.383 7.51119 15.1748 7.30273C14.9666 7.0943 14.6841 6.97754 14.3896 6.97754H13.7686C13.3544 6.97754 13.0187 6.64162 13.0186 6.22754V5.6123C13.0186 4.99789 12.5205 4.50025 11.9072 4.5C11.2944 4.5 10.7969 4.99738 10.7969 5.61133V7.05762Z"),
            )
        }.build()
        return _ic_bone_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBone20Preview() {
    Icon(
        imageVector = Icons.ic_bone_20,
        contentDescription = null,
    )
}