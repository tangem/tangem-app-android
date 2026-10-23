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

private var _ic_graduation_cap_24: ImageVector? = null

val Icons.ic_graduation_cap_24: ImageVector
    get() {
        if (_ic_graduation_cap_24 != null) return _ic_graduation_cap_24!!
        _ic_graduation_cap_24 = ImageVector.Builder(
            name = "ic_graduation_cap_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.32612 3.59217C10.9896 2.66923 13.0113 2.66907 14.6748 3.59217L20.5576 6.86072C22.48 7.92886 22.4801 10.694 20.5576 11.7621L18.7246 12.7797V15.9301C18.7244 17.0902 18.1811 18.1805 17.2626 18.8773L17.0742 19.0111L15.5429 20.0297C13.3969 21.4571 10.6027 21.4568 8.45601 20.0297L6.92378 19.0111C5.89374 18.3257 5.27355 17.1693 5.27339 15.9301V12.7787L3.44233 11.7621C1.51955 10.694 1.51955 7.92879 3.44233 6.86072L9.32612 3.59217ZM7.27339 15.9301C7.27355 16.4992 7.55876 17.0311 8.03218 17.3461L9.56343 18.3646C11.0394 19.3457 12.9608 19.3455 14.4355 18.3646L15.9667 17.3461C16.44 17.0316 16.7244 16.499 16.7246 15.9301V13.891L14.6748 15.0306C13.0114 15.9536 10.9879 15.9539 9.32515 15.0306L7.27339 13.89V15.9301ZM13.7031 5.34022C12.6433 4.75244 11.3555 4.75225 10.2958 5.34022L4.41304 8.60975C3.8624 8.91591 3.86238 9.70693 4.41304 10.0131L6.74507 11.308C6.74931 11.3103 6.75356 11.3125 6.75776 11.3148L10.2968 13.2816C11.3554 13.8693 12.6432 13.8696 13.7031 13.2816L17.2197 11.3275C17.2349 11.3186 17.2499 11.3093 17.2656 11.3012L19.5869 10.0131C20.1372 9.70689 20.1372 8.91597 19.5869 8.60975L13.7031 5.34022Z"),
            )
        }.build()
        return _ic_graduation_cap_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGraduationCap24Preview() {
    Icon(
        imageVector = Icons.ic_graduation_cap_24,
        contentDescription = null,
    )
}