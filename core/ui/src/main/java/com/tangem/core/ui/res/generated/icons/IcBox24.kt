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

private var _ic_box_24: ImageVector? = null

val Icons.ic_box_24: ImageVector
    get() {
        if (_ic_box_24 != null) return _ic_box_24!!
        _ic_box_24 = ImageVector.Builder(
            name = "ic_box_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.7793 14.391C6.05602 13.9131 6.66756 13.7501 7.14551 14.0268L9.8584 15.5971C10.3364 15.8738 10.5003 16.4863 10.2236 16.9643C9.94677 17.4418 9.33423 17.6051 8.85645 17.3285L6.14355 15.7573C5.66577 15.4806 5.50294 14.8689 5.7793 14.391Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.6084 2.37444C11.47 1.87758 12.531 1.87751 13.3926 2.37444L19.6406 5.98088L19.7979 6.07854C20.1482 6.31303 20.4345 6.62138 20.6436 6.97503C20.6487 6.98342 20.6552 6.99181 20.6602 7.00042C20.665 7.00889 20.6682 7.01823 20.6729 7.02678C20.9039 7.43735 21.0332 7.90612 21.0332 8.39299V15.6078C21.0329 16.6026 20.5027 17.5227 19.6406 18.0199L19.6396 18.019L13.3936 21.6264C12.586 22.0921 11.602 22.1206 10.7725 21.7133L10.6084 21.6264L4.36035 18.019C3.49943 17.521 2.96709 16.6019 2.9668 15.6069V8.39299C2.9668 7.39787 3.4981 6.47831 4.36035 5.98088L7.45117 4.19671C7.46567 4.18761 7.48021 4.17866 7.49512 4.17034L10.6084 2.37444ZM17.0068 10.2641V12.767C17.0066 13.3191 16.559 13.767 16.0068 13.767C15.4549 13.7668 15.0071 13.3189 15.0068 12.767V11.4184L13 12.5766V19.5434L18.6416 16.2865C18.8835 16.1467 19.0329 15.8884 19.0332 15.6078V9.09417L17.0068 10.2641ZM4.96777 15.6069C4.96807 15.8874 5.11787 16.1467 5.36133 16.2875L11 19.5424V12.5756L4.96777 9.09417V15.6069ZM5.96777 7.36174L12 10.8442L14.0166 9.6801L7.9834 6.19866L5.96777 7.36174ZM12.3936 4.10686C12.1504 3.96661 11.8506 3.96576 11.6074 4.10588L11.6084 4.10686L9.98438 5.04338L16.0156 8.52581L18.0322 7.36174L12.3936 4.10686Z"),
            )
        }.build()
        return _ic_box_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBox24Preview() {
    Icon(
        imageVector = Icons.ic_box_24,
        contentDescription = null,
    )
}