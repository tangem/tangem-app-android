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

private var _ic_bell_28: ImageVector? = null

val Icons.ic_bell_28: ImageVector
    get() {
        if (_ic_bell_28 != null) return _ic_bell_28!!
        _ic_bell_28 = ImageVector.Builder(
            name = "ic_bell_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.0107 1.93945C18.7349 1.9396 22.6109 5.69589 22.6113 10.3848V14.7139L23.9062 15.9805L23.9072 15.9824C24.6419 16.7033 25.0606 17.6859 25.0615 18.7158C25.0614 20.7906 23.3499 22.4286 21.2959 22.4287H17.668C17.5583 23.2581 17.2355 24.0537 16.709 24.6953C16.0428 25.507 15.08 26.0269 14.0098 26.0264C12.9404 26.0267 11.9784 25.5073 11.3125 24.6963C10.7856 24.0545 10.463 23.2585 10.3535 22.4287H6.72559C5.73563 22.4286 4.78117 22.0436 4.07324 21.3506C3.36442 20.6566 2.96101 19.7099 2.96094 18.7168C2.96187 17.687 3.37971 16.7032 4.11426 15.9824L4.11621 15.9805L5.41113 14.7139V10.3848C5.41153 5.69589 9.28657 1.9396 14.0107 1.93945ZM12.9004 22.4287C12.9764 22.6953 13.0954 22.9292 13.2441 23.1104C13.4968 23.418 13.7808 23.5265 14.0098 23.5264C14.239 23.5265 14.5236 23.4173 14.7764 23.1094C14.925 22.9283 15.0448 22.695 15.1211 22.4287H12.9004ZM14.0107 4.43945C10.617 4.43959 7.91153 7.12635 7.91113 10.3848V15.2402C7.91102 15.5762 7.77524 15.8988 7.53516 16.1338L5.86426 17.7676C5.60306 18.0244 5.46132 18.367 5.46094 18.7178C5.46127 19.0301 5.58822 19.3353 5.82227 19.5645C6.05746 19.7946 6.38185 19.9286 6.72559 19.9287H21.2959C22.0191 19.9286 22.5608 19.3617 22.5615 18.7178L22.5547 18.5859C22.5235 18.2817 22.3853 17.9913 22.1562 17.7666L20.4873 16.1338C20.247 15.8987 20.1114 15.5763 20.1113 15.2402V10.3848C20.1109 7.12635 17.4045 4.4396 14.0107 4.43945Z"),
            )
        }.build()
        return _ic_bell_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBell28Preview() {
    Icon(
        imageVector = Icons.ic_bell_28,
        contentDescription = null,
    )
}