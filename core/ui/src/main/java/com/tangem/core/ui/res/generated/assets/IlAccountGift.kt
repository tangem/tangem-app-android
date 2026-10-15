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

private var _il_account_gift: ImageVector? = null

val Icons.il_account_gift: ImageVector
    get() {
        if (_il_account_gift != null) return _il_account_gift!!
        _il_account_gift = ImageVector.Builder(
            name = "il_account_gift",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.3369 3.04649C13.4301 1.9695 15.1663 1.98608 16.2373 3.1002C17.31 4.21635 17.3098 6.01517 16.2373 7.13145C16.1975 7.17286 16.1543 7.21118 16.1133 7.25059H20C20.9662 7.25059 21.75 8.03438 21.75 9.00059V11.0006C21.7497 11.9666 20.9661 12.7506 20 12.7506H19.75H18.25H12.75H11.25H5.75H4.25H4C3.03395 12.7506 2.25026 11.9666 2.25 11.0006V9.00059C2.25 8.03438 3.03379 7.25059 4 7.25059H7.88672C7.84565 7.21113 7.80254 7.17292 7.7627 7.13145C7.74409 7.11206 7.72705 7.09168 7.71094 7.0709C6.69304 5.95109 6.70921 4.19715 7.76367 3.1002C8.8342 1.98682 10.568 1.96997 11.6611 3.04551C11.6816 3.06266 11.7018 3.08054 11.7207 3.1002C11.824 3.20772 11.917 3.32291 12 3.44297C12.083 3.32291 12.176 3.20772 12.2793 3.1002C12.2977 3.08106 12.317 3.06325 12.3369 3.04649ZM8.84473 4.14024C8.33028 4.6759 8.33013 5.55682 8.84473 6.09239C9.49524 6.76936 10.4683 7.07979 11.2266 7.19395C11.243 6.93785 11.2559 6.63124 11.2461 6.30625C11.2329 5.8723 11.1849 5.4256 11.0801 5.02989C10.9745 4.63187 10.8247 4.33489 10.6436 4.14414C10.6421 4.14267 10.6401 4.14173 10.6387 4.14024C10.1399 3.62132 9.34364 3.62149 8.84473 4.14024ZM13.3623 4.14024C13.3607 4.14192 13.3587 4.14335 13.3568 4.14477C13.355 4.14612 13.3531 4.14747 13.3516 4.14903C13.1725 4.33994 13.0246 4.63534 12.9199 5.02989C12.8151 5.4256 12.7671 5.8723 12.7539 6.30625C12.7441 6.63122 12.756 6.93787 12.7725 7.19395C13.5311 7.07996 14.5045 6.7697 15.1553 6.09239C15.67 5.55674 15.67 4.67589 15.1553 4.14024C14.6564 3.62129 13.8612 3.62139 13.3623 4.14024Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.75 14.2906V20.0006C19.7497 20.9666 18.9661 21.7506 18 21.7506H12.75V20.2506V14.2906H18.25H19.75Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6 21.7506C5.03395 21.7506 4.25026 20.9666 4.25 20.0006V14.2906H5.75H11.25V20.2506V21.7506H6Z"),
            )
        }.build()
        return _il_account_gift!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountGiftPreview() {
    Icon(
        imageVector = Icons.il_account_gift,
        contentDescription = null,
    )
}