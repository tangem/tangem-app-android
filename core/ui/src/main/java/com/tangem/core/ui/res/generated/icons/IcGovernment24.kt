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

private var _ic_government_24: ImageVector? = null

val Icons.ic_government_24: ImageVector
    get() {
        if (_ic_government_24 != null) return _ic_government_24!!
        _ic_government_24 = ImageVector.Builder(
            name = "ic_government_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.6661 3.05713C11.8818 2.98096 12.1173 2.98096 12.3331 3.05713L19.9405 5.74189C20.3398 5.88294 20.6064 6.26056 20.6065 6.68326V8.79209C20.6064 9.34308 20.1587 9.78998 19.6065 9.78998H19.4063V14.4471C19.637 14.4955 19.8455 14.6247 19.9922 14.8145L20.793 15.8495C20.9278 16.024 21.001 16.2382 21.001 16.4585L21.001 19.2521C21.001 19.8032 20.5533 20.25 20.001 20.25H3.99902C3.44674 20.25 2.99902 19.8032 2.99902 19.2521L2.99908 16.4585C2.99912 16.2382 3.0723 16.024 3.20709 15.8495L4.00787 14.8145L4.08307 14.7268C4.22228 14.5844 4.40084 14.4875 4.59478 14.4471V9.78998H4.39264C3.84044 9.78998 3.39279 9.34308 3.39264 8.79209V6.68326C3.39269 6.26059 3.65939 5.88296 4.05865 5.74189L11.6661 3.05713ZM4.99908 16.7986L4.99902 18.2542H19.001L19.001 16.7986L18.709 16.4215H5.29107L4.99908 16.7986ZM6.59478 14.4257H9.0274V9.78998H6.59478V14.4257ZM11.0274 14.4257H13.2295V9.78998H11.0274V14.4257ZM15.2295 14.4257H17.4063V9.78998H15.2295V14.4257ZM5.39264 7.38783V7.7942H18.6065V7.38783L11.9991 5.05486L5.39264 7.38783Z"),
            )
        }.build()
        return _ic_government_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGovernment24Preview() {
    Icon(
        imageVector = Icons.ic_government_24,
        contentDescription = null,
    )
}