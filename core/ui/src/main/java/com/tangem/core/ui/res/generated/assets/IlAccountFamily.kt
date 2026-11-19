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

private var _il_account_family: ImageVector? = null

val Icons.il_account_family: ImageVector
    get() {
        if (_il_account_family != null) return _il_account_family!!
        _il_account_family = ImageVector.Builder(
            name = "il_account_family",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.3756 17C22.6201 17 24.504 18.7702 24.504 21.0313C24.5037 23.2922 22.6199 25.0625 20.3756 25.0625C18.1316 25.0622 16.2485 23.2919 16.2483 21.0313C16.2483 18.7704 18.1315 17.0003 20.3756 17Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M28.4428 18.7165C30.1971 18.7167 31.6834 20.1027 31.6834 21.8895C31.6831 23.676 30.197 25.0613 28.4428 25.0615C26.6885 25.0615 25.2025 23.6762 25.2023 21.8895C25.2023 20.1026 26.6884 18.7165 28.4428 18.7165Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.0001 32C14.4479 31.9998 14 31.5502 14 30.9956V30.1138C14 27.6166 16.0813 25.6552 18.5698 25.6549H22.1844C23.1561 25.6549 24.0658 25.954 24.8136 26.4673C25.5614 25.954 26.471 25.655 27.4427 25.6549H28.4428H29.4302C31.9188 25.655 34 27.6165 34 30.1138V30.9956C34 31.5502 33.5521 31.9998 32.9999 32H15.0001Z"),
            )
        }.build()
        return _il_account_family!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountFamilyPreview() {
    Icon(
        imageVector = Icons.il_account_family,
        contentDescription = null,
    )
}