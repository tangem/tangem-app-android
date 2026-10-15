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

private var _ic_rose_32: ImageVector? = null

val Icons.ic_rose_32: ImageVector
    get() {
        if (_ic_rose_32 != null) return _ic_rose_32!!
        _ic_rose_32 = ImageVector.Builder(
            name = "ic_rose_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.3574 2.5C19.9479 2.50015 22.1536 4.25633 22.7617 6.66016C25.1808 7.24797 27 9.3977 27 12C27 14.0186 25.9053 15.7652 24.2852 16.7217V17.334C24.285 21.3583 21.3427 24.6687 17.5 25.3633V28C17.5 28.8283 16.8282 29.4998 16 29.5C15.1716 29.5 14.5 28.8284 14.5 28V25.3633C10.6571 24.6689 7.71405 21.3585 7.71387 17.334V15.7393C6.05872 14.3675 5 12.3129 5 10C5 5.83333 8.43164 2.5 12.6074 2.5H17.3574ZM10.7139 17.334C10.7141 20.1617 13.0565 22.4999 16 22.5C18.8856 22.4998 21.1915 20.2526 21.2812 17.5H12.6074C11.9546 17.5 11.3201 17.418 10.7139 17.2646V17.334ZM12.6074 5.5C10.0382 5.5 8 7.54001 8 10C8 11.537 8.79586 12.9097 10.0195 13.7236C10.0329 13.7321 10.0465 13.7401 10.0596 13.749C10.7867 14.222 11.6607 14.5 12.6074 14.5H21.4287C22.8741 14.4999 24 13.3559 24 12C24 11.0529 23.4498 10.2108 22.626 9.78809C21.8713 11.9586 19.7838 13.4999 17.3574 13.5H13.3916C11.4646 13.5 9.85547 11.9573 9.85547 10C9.85547 8.04266 11.4646 6.5 13.3916 6.5H16.1465C16.9749 6.5 17.6465 7.17157 17.6465 8C17.6465 8.82843 16.9749 9.5 16.1465 9.5H13.3916C13.0712 9.5 12.8555 9.74934 12.8555 10C12.8555 10.2507 13.0712 10.5 13.3916 10.5H17.3574C18.7122 10.4998 19.7858 9.49463 19.915 8.25195L19.9287 8L19.915 7.74805C19.7858 6.50537 18.7122 5.50016 17.3574 5.5H12.6074Z"),
            )
        }.build()
        return _ic_rose_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcRose32Preview() {
    Icon(
        imageVector = Icons.ic_rose_32,
        contentDescription = null,
    )
}