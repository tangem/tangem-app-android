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

private var _ic_gift_32: ImageVector? = null

val Icons.ic_gift_32: ImageVector
    get() {
        if (_ic_gift_32 != null) return _ic_gift_32!!
        _ic_gift_32 = ImageVector.Builder(
            name = "ic_gift_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.999 4.21972C17.6064 2.58275 20.2094 2.59351 21.8037 4.25194C23.1493 5.65178 23.3427 7.77866 22.3857 9.38866H26.2227C27.7561 9.38885 28.9997 10.6326 29 12.166V14.7217C28.9999 16.1805 27.8742 17.375 26.4443 17.4883V26.2226C26.4442 27.7563 25.2006 28.9999 23.667 29H8.33301C6.79948 28.9998 5.55576 27.7562 5.55566 26.2226V17.4883C4.12585 17.3749 3.00013 16.1805 3 14.7217V12.166C3.00025 10.6326 4.24391 9.3889 5.77734 9.38866H9.61426C8.65728 7.77866 8.8507 5.65179 10.1963 4.25194C11.7903 2.59387 14.3916 2.58369 15.999 4.21972ZM8.55566 17.5V26H23.4443V17.5H8.55566ZM6 14.5H26V12.3887H6V14.5ZM19.6406 6.33105C19.2163 5.88983 18.5542 5.88967 18.1299 6.33105C18.1281 6.33287 18.1258 6.33412 18.124 6.33593C17.9783 6.49018 17.8207 6.77404 17.6992 7.23241C17.5794 7.68486 17.52 8.21128 17.5039 8.7412C17.4991 8.90008 17.4989 9.05555 17.501 9.20507C18.2514 9.01835 19.0623 8.67193 19.6309 8.08593C19.6342 8.08241 19.6373 8.07868 19.6406 8.07519C20.0971 7.60034 20.0971 6.80589 19.6406 6.33105ZM13.8701 6.33105C13.4459 5.88968 12.7837 5.88989 12.3594 6.33105C11.9029 6.80589 11.9029 7.60034 12.3594 8.07519C12.3608 8.07668 12.3618 8.07857 12.3633 8.08007C12.9317 8.66941 13.7456 9.01654 14.498 9.20409C14.5 9.05487 14.4999 8.89974 14.4951 8.7412C14.4791 8.2113 14.4206 7.68484 14.3008 7.23241C14.1932 6.82672 14.0568 6.55788 13.9258 6.39452L13.8701 6.33105Z"),
            )
        }.build()
        return _ic_gift_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGift32Preview() {
    Icon(
        imageVector = Icons.ic_gift_32,
        contentDescription = null,
    )
}