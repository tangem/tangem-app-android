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
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.0006 2C14.0383 2.00003 15.6715 3.66688 15.6715 5.7002V6.5H17.3981C18.34 6.5 19.1171 7.18954 19.2643 8.09082L19.2848 8.27344L19.9938 19.0146C20.0996 20.6191 18.8376 22 17.2184 22H6.78091C5.16191 21.9998 3.89977 20.619 4.00552 19.0146L4.7145 8.27344L4.73501 8.09082C4.88214 7.18962 5.65946 6.50016 6.60122 6.5H8.32876V5.7002C8.32876 3.66653 9.96352 2.00018 12.0006 2ZM12.0006 4C11.0876 4.00018 10.3288 4.75149 10.3288 5.7002V6.5H13.6715V5.7002C13.6715 4.75096 12.9134 4.00003 12.0006 4Z"),
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