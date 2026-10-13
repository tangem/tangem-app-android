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

private var _il_account_bookmark: ImageVector? = null

val Icons.il_account_bookmark: ImageVector
    get() {
        if (_il_account_bookmark != null) return _il_account_bookmark!!
        _il_account_bookmark = ImageVector.Builder(
            name = "il_account_bookmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.002 3.24707C18.0735 3.24723 19.7528 4.92651 19.7529 6.99805V19.5029L19.7412 19.6768C19.6855 20.0757 19.4387 20.4277 19.0742 20.6152C18.7095 20.8027 18.2799 20.7983 17.9229 20.6113L17.7754 20.5205L11.999 16.3936L6.22461 20.5195C5.84402 20.7917 5.34312 20.8287 4.92676 20.6152C4.51043 20.4015 4.24795 19.9728 4.24707 19.5049V6.99805C4.24722 4.92651 5.92651 3.24723 7.99805 3.24707H16.002Z"),
            )
        }.build()
        return _il_account_bookmark!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountBookmarkPreview() {
    Icon(
        imageVector = Icons.il_account_bookmark,
        contentDescription = null,
    )
}