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

private var _ic_bone_28: ImageVector? = null

val Icons.ic_bone_28: ImageVector
    get() {
        if (_ic_bone_28 != null) return _ic_bone_28!!
        _ic_bone_28 = ImageVector.Builder(
            name = "ic_bone_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.8223 7.15332C12.8223 4.86009 14.681 3.00004 16.9746 3C19.1747 3 20.9727 4.71082 21.1172 6.87402C22.1207 6.93919 23.0701 7.36624 23.7852 8.08203C24.5636 8.86132 25.0005 9.91806 25 11.0195C25 13.3128 23.1413 15.1729 20.8477 15.1729H18.5898C18.5864 15.1729 18.5826 15.1742 18.5801 15.1768L18.5771 15.1807L15.1768 18.582C15.1749 18.584 15.1739 18.5869 15.1738 18.5898V20.8467C15.1738 23.1378 13.3188 24.997 11.0273 25L11.0254 23.75V25C8.82194 25 7.02052 23.2841 6.88086 21.1162C5.87811 21.0506 4.92938 20.6245 4.21484 19.9092C3.43621 19.1297 2.99933 18.0725 3 16.9707C3.00049 14.6779 4.85904 12.8184 7.15234 12.8184H9.41211C9.41533 12.8182 9.41841 12.8168 9.4209 12.8145L12.8193 9.40918L12.8223 9.40137V7.15332ZM15.3223 9.41016C15.3199 10.0723 15.0568 10.7071 14.5889 11.1758L11.1885 14.582L11.1865 14.585C10.7184 15.0517 10.085 15.3151 9.42383 15.3184H7.15234C6.24052 15.3184 5.5 16.0581 5.5 16.9717C5.49981 17.4104 5.67357 17.8324 5.9834 18.1426C6.29328 18.4528 6.71423 18.627 7.15234 18.627H8.12207C8.81224 18.6272 9.37207 19.1867 9.37207 19.877V20.8447C9.37207 21.759 10.1121 22.4985 11.0244 22.499C11.9352 22.4976 12.6738 21.7593 12.6738 20.8467V18.5811C12.6762 17.9188 12.9402 17.2841 13.4082 16.8154L16.8086 13.4131C17.2791 12.9404 17.9188 12.6741 18.5859 12.6729H20.8477C21.7595 12.6729 22.5 11.9332 22.5 11.0195C22.5002 10.5808 22.3264 10.1588 22.0166 9.84863C21.7067 9.53845 21.2858 9.36426 20.8477 9.36426H19.8789C19.1886 9.36426 18.6289 8.80461 18.6289 8.11426V7.15527C18.6289 6.24067 17.8874 5.5 16.9746 5.5C16.0628 5.50004 15.3223 6.23973 15.3223 7.15332V9.41016Z"),
            )
        }.build()
        return _ic_bone_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBone28Preview() {
    Icon(
        imageVector = Icons.ic_bone_28,
        contentDescription = null,
    )
}