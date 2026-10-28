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

private var _ic_hanger_32: ImageVector? = null

val Icons.ic_hanger_32: ImageVector
    get() {
        if (_ic_hanger_32 != null) return _ic_hanger_32!!
        _ic_hanger_32 = ImageVector.Builder(
            name = "ic_hanger_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16 3C18.2403 3.00006 20.0557 4.81533 20.0557 7.05566C20.0556 8.86618 18.856 9.81226 18.1299 10.3018L18.1279 10.3027C17.8757 10.4722 17.728 10.6005 17.6367 10.7109C18.6099 10.8233 19.5746 11.0335 20.5146 11.3467L25.9424 13.1562C27.7696 13.764 28.9999 15.472 29 17.3965C29 19.6486 27.3348 21.5111 25.167 21.8203V24.8818C25.167 27.1553 23.3222 28.9997 21.0488 29H10.9521C8.67856 29 6.83398 27.1554 6.83398 24.8818V21.833C4.67163 21.5553 3 19.7095 3 17.4717V17.4092C3.00014 15.4763 4.22679 13.7656 6.05566 13.1562L11.4844 11.3467L12.0361 11.1748C12.8524 10.9391 13.6846 10.7794 14.5225 10.6924C14.6863 9.12448 15.7693 8.27328 16.4551 7.8125C16.7265 7.62942 16.8719 7.49882 16.9551 7.38672C17.0134 7.30805 17.0556 7.22372 17.0557 7.05566C17.0557 6.47218 16.5835 6.00006 16 6C15.4165 6 14.9443 6.47215 14.9443 7.05566C14.9443 7.88404 14.2727 8.55566 13.4443 8.55566C12.6159 8.55566 11.9444 7.88404 11.9443 7.05566C11.9443 4.81529 13.7596 3 16 3ZM9.83398 24.8818C9.83398 25.4986 10.3354 26 10.9521 26H21.0488C21.6653 25.9997 22.167 25.4984 22.167 24.8818V20.0557H9.83398V24.8818ZM19.5664 14.1924C17.3957 13.4692 15.0604 13.4249 12.8691 14.0576L12.4326 14.1924L7.00488 16.002C6.40935 16.2004 6.00014 16.7591 6 17.4092V17.4717C6 18.1185 6.43981 18.661 7.03613 18.8213C7.45077 17.787 8.46122 17.0557 9.64355 17.0557H22.3584C23.5334 17.0559 24.5372 17.7793 24.9561 18.8037C25.5598 18.6211 26 18.0611 26 17.3965C25.9999 16.7631 25.5955 16.2017 24.9951 16.002L19.5664 14.1924Z"),
            )
        }.build()
        return _ic_hanger_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHanger32Preview() {
    Icon(
        imageVector = Icons.ic_hanger_32,
        contentDescription = null,
    )
}