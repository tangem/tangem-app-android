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
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M32.0007 16.3545C29.3508 15.8245 26.6153 15.8894 23.9937 16.5449L23.5209 16.663C21.1868 17.2465 18.7521 17.3047 16.3929 16.8329C15.8052 16.7154 15.1949 16.8679 14.7316 17.2477C14.2684 17.6276 14 18.1954 14 18.7941V29.6331C14 30.5858 14.6726 31.4072 15.6077 31.5943L15.761 31.6246C18.5683 32.1861 21.4653 32.1169 24.2427 31.4225L24.4791 31.364C26.8133 30.7804 29.2488 30.7223 31.608 31.1941C32.1957 31.3116 32.8051 31.159 33.2684 30.7792C33.7317 30.3993 34 29.8317 34 29.2329V18.3938C33.9999 17.4411 33.3275 16.6196 32.3923 16.4326L32.0007 16.3545ZM19.5246 28.4997C19.5245 29.0515 19.0766 29.4992 18.5245 29.4994H17.5244C16.9721 29.4994 16.5244 29.0516 16.5242 28.4997C16.5242 27.9476 16.972 27.5001 17.5244 27.5001H18.5245C19.0767 27.5002 19.5246 27.9477 19.5246 28.4997ZM24.0005 20.5005C25.9339 20.5007 27.4999 22.0681 27.5 24.0003C27.5 25.9325 25.9339 27.4999 24.0005 27.5001C22.067 27.4999 20.5 25.9326 20.5 24.0003C20.5001 22.0681 22.0671 20.5007 24.0005 20.5005ZM22.5003 24.0003C22.5004 23.1712 23.1728 22.5 24.0005 22.4998C24.8281 22.5 25.4996 23.1713 25.4997 24.0003C25.4997 24.8294 24.8282 25.5006 24.0005 25.5008C23.1728 25.5006 22.5003 24.8294 22.5003 24.0003ZM31.5394 19.5009C31.5394 20.0529 31.0916 20.5005 30.5393 20.5005H29.5391C28.9869 20.5004 28.539 20.0528 28.539 19.5009C28.539 18.9489 28.9869 18.5014 29.5391 18.5012H30.5393C31.0916 18.5013 31.5394 18.9488 31.5394 19.5009Z"),
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