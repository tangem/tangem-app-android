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

private var _ic_eye_24: ImageVector? = null

val Icons.ic_eye_24: ImageVector
    get() {
        if (_ic_eye_24 != null) return _ic_eye_24!!
        _ic_eye_24 = ImageVector.Builder(
            name = "ic_eye_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.9997 8.00226C13.0602 8.00233 14.078 8.42341 14.8279 9.17316C15.5776 9.92282 15.9986 10.9401 15.9988 12.0003C15.9987 13.0605 15.5775 14.0787 14.8279 14.8284C14.078 15.578 13.0601 15.9993 11.9997 15.9993C10.9393 15.9993 9.92152 15.5782 9.17162 14.8284C8.42178 14.0787 8.0008 13.0607 8.00072 12.0003C8.00091 10.9403 8.42208 9.92279 9.17162 9.17316C9.9215 8.42357 10.9394 8.00234 11.9997 8.00226ZM11.9997 10.0023C11.4697 10.0023 10.9605 10.2127 10.5857 10.5872C10.2112 10.9618 10.0009 11.4708 10.0007 12.0003C10.0008 12.5299 10.2112 13.0387 10.5857 13.4134C10.9605 13.7881 11.4696 13.9993 11.9997 13.9993C12.5297 13.9993 13.038 13.7879 13.4128 13.4134C13.7874 13.0387 13.9987 12.53 13.9988 12.0003C13.9986 11.4706 13.7875 10.9618 13.4128 10.5872C13.0381 10.2128 12.5296 10.0023 11.9997 10.0023Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.9822 4.00129C15.9468 3.93095 19.6104 6.10934 21.4411 9.62629C21.4444 9.63251 21.4478 9.63952 21.4509 9.64582C22.1831 11.1298 22.1831 12.8708 21.4509 14.3548L21.448 14.3607C20.5832 16.0823 19.2352 17.5116 17.574 18.4769C15.8982 19.4707 13.9795 20.0007 12.0163 19.9984C8.05176 20.0684 4.3879 17.8904 2.55736 14.3734C2.55419 14.3673 2.55063 14.3609 2.54759 14.3548C1.81547 12.8709 1.81556 11.1298 2.54759 9.64582L2.5515 9.63996C3.4161 7.91867 4.76277 6.48806 6.42357 5.52277C8.09935 4.52875 10.0189 3.99905 11.9822 4.00129ZM11.9968 6.00129C10.5192 5.99685 9.07079 6.36129 7.78099 7.05207C7.75241 7.07129 7.72223 7.09022 7.69115 7.10676C7.60723 7.15136 7.52318 7.19698 7.44115 7.24445C7.15498 7.41436 6.87635 7.60041 6.60912 7.80305C6.60348 7.80724 6.59626 7.81069 6.59056 7.81476C5.64869 8.53728 4.87642 9.46675 4.33861 10.5374C3.88598 11.46 3.88585 12.5406 4.33861 13.4632C5.77474 16.2109 8.59601 17.945 11.6814 17.9993H12.0027C13.4879 18.0038 14.9422 17.6335 16.237 16.9359C16.2598 16.9213 16.2841 16.9068 16.3083 16.8939C16.3985 16.846 16.4879 16.7957 16.5759 16.7445C16.8556 16.5775 17.1279 16.3949 17.3894 16.1966C17.3987 16.1896 17.4092 16.1828 17.4187 16.1761C18.3538 15.4562 19.1214 14.533 19.657 13.4691C20.1127 12.545 20.1137 11.4621 19.6599 10.5374C18.1772 7.7008 15.219 5.94339 12.0173 6.00031C12.0106 6.00045 12.0035 6.0013 11.9968 6.00129Z"),
            )
        }.build()
        return _ic_eye_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcEye24Preview() {
    Icon(
        imageVector = Icons.ic_eye_24,
        contentDescription = null,
    )
}