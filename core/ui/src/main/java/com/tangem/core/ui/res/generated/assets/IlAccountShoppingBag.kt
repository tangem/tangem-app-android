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

private var _il_account_shopping_bag: ImageVector? = null

val Icons.il_account_shopping_bag: ImageVector
    get() {
        if (_il_account_shopping_bag != null) return _il_account_shopping_bag!!
        _il_account_shopping_bag = ImageVector.Builder(
            name = "il_account_shopping_bag",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.0013 14C26.039 14 27.6722 15.6669 27.6722 17.7002V18.5H29.3988C30.3407 18.5 31.1178 19.1895 31.265 20.0908L31.2855 20.2734L31.9945 31.0146C32.1002 32.6191 30.8383 34 29.2191 34H18.7816C17.1626 33.9998 15.9004 32.619 16.0062 31.0146L16.7152 20.2734L16.7357 20.0908C16.8828 19.1896 17.6601 18.5002 18.6019 18.5H20.3294V17.7002C20.3294 15.6665 21.9642 14.0002 24.0013 14ZM24.0013 16C23.0883 16.0002 22.3294 16.7515 22.3294 17.7002V18.5H25.6722V17.7002C25.6722 16.751 24.914 16 24.0013 16Z"),
            )
        }.build()
        return _il_account_shopping_bag!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountShoppingBagPreview() {
    Icon(
        imageVector = Icons.il_account_shopping_bag,
        contentDescription = null,
    )
}