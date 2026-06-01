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

private var _ic_heart_broken_32: ImageVector? = null

val Icons.ic_heart_broken_32: ImageVector
    get() {
        if (_ic_heart_broken_32 != null) return _ic_heart_broken_32!!
        _ic_heart_broken_32 = ImageVector.Builder(
            name = "ic_heart_broken_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.8301 4.5C25.8213 4.5 28.9988 9.05222 28.999 13.1299C28.9989 15.2712 28.1439 17.2402 26.9707 18.9355C25.796 20.6328 24.2525 22.132 22.7441 23.3643C21.2306 24.6008 19.7152 25.5982 18.5664 26.2871C17.9914 26.6319 17.5025 26.9035 17.1445 27.0908C16.9668 27.1838 16.8146 27.2594 16.6973 27.3145C16.6403 27.3412 16.5795 27.3687 16.5234 27.3916C16.4968 27.4024 16.4553 27.4181 16.4082 27.4336C16.3849 27.4413 16.3459 27.4543 16.2979 27.4658C16.2678 27.473 16.1507 27.5009 16 27.501C15.8493 27.501 15.7322 27.473 15.7021 27.4658C15.6537 27.4542 15.6141 27.4413 15.5908 27.4336C15.5435 27.418 15.5021 27.4024 15.4756 27.3916C15.4195 27.3687 15.3597 27.3412 15.3027 27.3145C15.1853 27.2594 15.0334 27.184 14.8555 27.0908C14.4974 26.9034 14.0079 26.6321 13.4326 26.2871C12.2839 25.5982 10.7693 24.6006 9.25586 23.3643C7.74737 22.1319 6.20306 20.633 5.02832 18.9355C3.85509 17.2402 3.00013 15.2712 3 13.1299C3.00024 9.05237 6.17803 4.50034 11.1689 4.5C13.3037 4.5 14.8914 5.29752 15.999 6.20703C17.1066 5.29727 18.6949 4.50008 20.8301 4.5ZM11.1689 7C7.88378 7.00035 5.50024 10.0839 5.5 13.1299C5.50013 14.5951 6.08699 16.072 7.08398 17.5127C8.07956 18.9513 9.43439 20.2819 10.8369 21.4277C12.2345 22.5695 13.645 23.4996 14.7188 24.1436C14.8426 24.2178 14.9633 24.2854 15.0771 24.3516L16.5635 19.4912L13.7412 16.1689C13.3452 15.7025 13.3455 15.0174 13.7412 14.5508L16.4668 11.3408L15.2393 8.93164C15.1752 8.88254 15.1147 8.82814 15.0605 8.7666C14.3209 7.92498 13.0843 7 11.1689 7ZM20.8301 7C19.4162 7.00008 18.3723 7.50393 17.625 8.10645L19.0723 10.9482C19.3011 11.3975 19.2385 11.9408 18.9121 12.3252L16.334 15.3594L18.9121 18.3945C19.1876 18.7191 19.2788 19.1622 19.1543 19.5693L17.8652 23.7832C18.8409 23.1706 20.006 22.3722 21.1621 21.4277C22.5646 20.2819 23.9194 18.9512 24.915 17.5127C25.912 16.072 26.4989 14.5951 26.499 13.1299C26.4988 10.0837 24.1155 7 20.8301 7Z"),
            )
        }.build()
        return _ic_heart_broken_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeartBroken32Preview() {
    Icon(
        imageVector = Icons.ic_heart_broken_32,
        contentDescription = null,
    )
}