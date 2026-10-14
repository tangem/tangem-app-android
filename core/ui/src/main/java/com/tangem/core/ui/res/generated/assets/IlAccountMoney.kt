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

private var _il_account_money: ImageVector? = null

val Icons.il_account_money: ImageVector
    get() {
        if (_il_account_money != null) return _il_account_money!!
        _il_account_money = ImageVector.Builder(
            name = "il_account_money",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.999 4.35133C17.3496 3.82135 14.6144 3.88626 11.9932 4.54169L11.5205 4.65981C9.18676 5.24334 6.7524 5.30149 4.39355 4.82968C3.80593 4.71221 3.1957 4.8647 2.73242 5.24457C2.26935 5.62445 2.00101 6.19221 2.00098 6.79091V17.6299C2.00097 18.5826 2.67348 19.404 3.6084 19.5912L3.76172 19.6214C6.56861 20.1829 9.46518 20.1137 12.2422 19.4194L12.4785 19.3608C14.8124 18.7772 17.2475 18.7191 19.6064 19.1909C20.1941 19.3084 20.8033 19.1559 21.2666 18.776C21.7298 18.3961 21.9981 17.8285 21.998 17.2297V6.39066C21.9979 5.43794 21.3256 4.61647 20.3906 4.42943L19.999 4.35133ZM7.52481 16.4965C7.52462 17.0484 7.07683 17.496 6.52481 17.4962H5.52481C4.97265 17.4962 4.525 17.0485 4.52481 16.4965C4.52481 15.9444 4.97253 15.4969 5.52481 15.4969H6.52481C7.07695 15.4971 7.52481 15.9446 7.52481 16.4965ZM12 8.49736C13.9331 8.49755 15.4989 10.0649 15.499 11.9971C15.499 13.9294 13.9332 15.4967 12 15.4969C10.0668 15.4968 8.5 13.9294 8.5 11.9971C8.50009 10.0649 10.0669 8.49749 12 8.49736ZM10.5 11.9971C10.5001 11.1681 11.1725 10.4968 12 10.4967C12.8275 10.4969 13.4989 11.1681 13.499 11.9971C13.499 12.8262 12.8275 13.4974 12 13.4976C11.1724 13.4974 10.5 12.8263 10.5 11.9971ZM19.5378 7.4977C19.5378 8.04972 19.09 8.49731 18.5378 8.49736H17.5378C16.9857 8.49722 16.5379 8.04966 16.5378 7.4977C16.5378 6.94569 16.9857 6.49818 17.5378 6.49805H18.5378C19.0901 6.49809 19.5378 6.94563 19.5378 7.4977Z"),
            )
        }.build()
        return _il_account_money!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountMoneyPreview() {
    Icon(
        imageVector = Icons.il_account_money,
        contentDescription = null,
    )
}