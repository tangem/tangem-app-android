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

private var _ic_rocket_24: ImageVector? = null

val Icons.ic_rocket_24: ImageVector
    get() {
        if (_ic_rocket_24 != null) return _ic_rocket_24!!
        _ic_rocket_24 = ImageVector.Builder(
            name = "ic_rocket_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.75263 15.0322C6.63945 14.899 7.53744 15.195 8.17157 15.8291C8.80553 16.4632 9.10068 17.3604 8.96747 18.2471C8.78296 19.4742 7.81963 20.4378 6.59247 20.6221L4.14813 20.9893C3.83461 21.0362 3.51691 20.9321 3.29267 20.708C3.06839 20.4837 2.96341 20.1652 3.01044 19.8516L3.37763 17.4072C3.56199 16.1803 4.52576 15.2169 5.75263 15.0322ZM6.05048 17.0098C5.692 17.0638 5.41019 17.3456 5.35614 17.7041L5.18915 18.8105L6.29559 18.6445C6.65422 18.5907 6.93585 18.3088 6.98993 17.9502C7.02887 17.691 6.94275 17.4285 6.75751 17.2432C6.5722 17.0579 6.30963 16.9709 6.05048 17.0098Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.3044 3.00977C20.2568 3.10645 21.0006 3.91074 21.0007 4.88867C21.0007 8.00147 19.5855 10.946 17.155 12.8906L16.5554 13.3701V16.7842C16.5552 17.4995 16.1512 18.1537 15.5114 18.4736L13.364 19.5469C12.8743 19.7917 12.3024 19.812 11.7966 19.6025C11.2909 19.393 10.9004 18.9744 10.7272 18.4551L10.1979 16.8662C10.0712 16.7892 9.95059 16.6979 9.84149 16.5889L7.41181 14.1582C7.30262 14.049 7.21057 13.9286 7.13349 13.8018L5.54559 13.2725C5.02615 13.0993 4.60671 12.709 4.39716 12.2031C4.18782 11.6974 4.20802 11.1254 4.45282 10.6357L5.52704 8.48828L5.59052 8.37109C5.92862 7.79958 6.54577 7.44434 7.21649 7.44434H10.6315L11.11 6.8457C13.0547 4.41509 15.9992 3 19.112 3L19.3044 3.00977ZM12.3571 16.7285C12.3317 16.7489 12.3042 16.7663 12.278 16.7852L12.5837 17.7002L14.5554 16.7148V14.9697L12.3571 16.7285ZM18.9968 5.00293C16.534 5.03714 14.2128 6.16928 12.6716 8.0957L8.8952 12.8145L11.1852 15.1045L15.905 11.3291C17.8316 9.78777 18.9628 7.466 18.9968 5.00293ZM6.29852 11.415L7.21454 11.7207C7.23334 11.6946 7.25185 11.668 7.27216 11.6426L9.03095 9.44434H7.28485L6.29852 11.415Z"),
            )
        }.build()
        return _ic_rocket_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcRocket24Preview() {
    Icon(
        imageVector = Icons.ic_rocket_24,
        contentDescription = null,
    )
}