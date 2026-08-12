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

private var _ic_filter_16: ImageVector? = null

val Icons.ic_filter_16: ImageVector
    get() {
        if (_ic_filter_16 != null) return _ic_filter_16!!
        _ic_filter_16 = ImageVector.Builder(
            name = "ic_filter_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.25944 8.53687C7.53806 8.53695 8.60253 9.42929 8.88346 10.6228H13.3337C13.6787 10.623 13.9587 10.9027 13.9587 11.2478C13.9586 11.5929 13.6787 11.8726 13.3337 11.8728H8.88346C8.60252 13.0663 7.53805 13.9587 6.25944 13.9587C4.98078 13.9587 3.9164 13.0663 3.63542 11.8728H2.66667C2.32149 11.8728 2.04168 11.593 2.04167 11.2478C2.04167 10.9026 2.32149 10.6228 2.66667 10.6228H3.63542C3.91639 9.42927 4.98077 8.53687 6.25944 8.53687ZM6.25944 9.78687C5.46241 9.78687 4.81022 10.4378 4.81022 11.2478C4.81023 12.0578 5.46242 12.7087 6.25944 12.7087C7.05637 12.7086 7.70865 12.0577 7.70866 11.2478C7.70866 10.4379 7.05638 9.78696 6.25944 9.78687ZM9.59245 2.04175C10.8712 2.04175 11.9356 2.93403 12.2165 4.12769H13.3337C13.6787 4.12786 13.9587 4.40761 13.9587 4.75269C13.9585 5.09764 13.6786 5.37751 13.3337 5.37769H12.2165C11.9354 6.57107 10.871 7.46265 9.59245 7.46265C8.31392 7.46258 7.24946 6.57106 6.96842 5.37769H2.66667C2.32157 5.37769 2.0418 5.09775 2.04167 4.75269C2.04166 4.40751 2.32149 4.12769 2.66667 4.12769H6.96842C7.24927 2.93404 8.31373 2.04182 9.59245 2.04175ZM9.59245 3.29175C8.79549 3.29183 8.14323 3.94275 8.14323 4.75269C8.14338 5.56249 8.79558 6.21257 9.59245 6.21265C10.3894 6.21265 11.0415 5.56254 11.0417 4.75269C11.0417 3.9427 10.3895 3.29175 9.59245 3.29175Z"),
            )
        }.build()
        return _ic_filter_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcFilter16Preview() {
    Icon(
        imageVector = Icons.ic_filter_16,
        contentDescription = null,
    )
}