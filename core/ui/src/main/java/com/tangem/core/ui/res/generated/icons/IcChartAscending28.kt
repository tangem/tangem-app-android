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

private var _ic_chart_ascending_28: ImageVector? = null

val Icons.ic_chart_ascending_28: ImageVector
    get() {
        if (_ic_chart_ascending_28 != null) return _ic_chart_ascending_28!!
        _ic_chart_ascending_28 = ImageVector.Builder(
            name = "ic_chart_ascending_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.75 4C5.44036 4 6 4.55964 6 5.25V21.5H23.249C23.9392 21.5002 24.499 22.0598 24.499 22.75C24.499 23.4402 23.9392 23.9998 23.249 24H4.75C4.05964 24 3.5 23.4404 3.5 22.75V5.25C3.5 4.55964 4.05964 4 4.75 4Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23.29 7.73633C23.3161 7.73717 23.3422 7.73777 23.3682 7.74023C23.3846 7.74178 23.4008 7.7449 23.417 7.74707C23.4411 7.75032 23.4653 7.75316 23.4893 7.75781C23.505 7.76088 23.5206 7.7649 23.5361 7.76855C23.5623 7.7747 23.5884 7.78121 23.6143 7.78906C23.6241 7.79207 23.6338 7.79559 23.6436 7.79883C23.6718 7.80817 23.6999 7.8177 23.7275 7.8291C23.7402 7.83434 23.7522 7.84104 23.7646 7.84668C23.7886 7.85752 23.8126 7.86843 23.8359 7.88086C23.8486 7.88759 23.8607 7.8952 23.873 7.90234C23.896 7.91558 23.9192 7.92855 23.9414 7.94336C23.9562 7.95318 23.9701 7.96418 23.9844 7.97461C24.0016 7.98718 24.0194 7.99913 24.0361 8.0127C24.0559 8.02867 24.0741 8.04635 24.0928 8.06348C24.1033 8.0731 24.1147 8.08174 24.125 8.0918L24.1338 8.10059C24.183 8.14981 24.2271 8.20389 24.2676 8.26074C24.3154 8.32785 24.3557 8.39832 24.3887 8.47168C24.4593 8.62828 24.5 8.80143 24.5 8.98438V11.6201C24.5 12.3104 23.9403 12.87 23.25 12.8701C22.6864 12.8701 22.2107 12.4969 22.0547 11.9844L18.3613 15.7471L18.3623 15.748C17.4933 16.6355 16.0745 16.6337 15.2061 15.749L13.335 13.8428L8.625 18.6445C8.14146 19.1371 7.34914 19.1446 6.85645 18.6611C6.36438 18.1777 6.35687 17.3861 6.83984 16.8936L11.7578 11.8818C12.6259 10.9972 14.0449 10.9953 14.9141 11.8818L16.7832 13.7871L20.3184 10.1846C19.7959 10.0348 19.4131 9.555 19.4131 8.98438C19.4131 8.29402 19.9727 7.73438 20.6631 7.73438H23.25C23.2634 7.73438 23.2767 7.73591 23.29 7.73633Z"),
            )
        }.build()
        return _ic_chart_ascending_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartAscending28Preview() {
    Icon(
        imageVector = Icons.ic_chart_ascending_28,
        contentDescription = null,
    )
}