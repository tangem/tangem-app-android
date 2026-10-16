@file:Suppress("all")

package com.tangem.core.ui.res.generated.assets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.generated.icons.Icons

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _il_account_home: ImageVector? = null

val Icons.il_account_home: ImageVector
    get() {
        if (_il_account_home != null) return _il_account_home!!
        _il_account_home = ImageVector.Builder(
            name = "il_account_home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.6701 3.07328C11.9122 2.95483 12.204 2.97506 12.4299 3.13285L21.5988 9.55082C21.9381 9.78832 22.0208 10.2564 21.7834 10.5957C21.6473 10.7902 21.4355 10.9004 21.2155 10.9144L20.0851 11.0913V20.253C20.0848 20.6669 19.7491 21.0029 19.3351 21.003H4.66424C4.25041 21.0028 3.9146 20.6668 3.91424 20.253V11.0913L2.78764 10.9144C2.56641 10.9016 2.35297 10.7913 2.216 10.5957C1.97895 10.2565 2.06148 9.78825 2.40057 9.55082L11.5695 3.13285L11.6701 3.07328ZM9.9992 14.751V19.503H14.0012V14.751C14.0012 14.1521 13.516 13.6672 12.9172 13.667H11.0832C10.4842 13.667 9.9992 14.152 9.9992 14.751Z"),
            )
        }.build()
        return _il_account_home!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountHomePreview() {
    Icon(
        imageVector = Icons.il_account_home,
        contentDescription = null,
    )
}