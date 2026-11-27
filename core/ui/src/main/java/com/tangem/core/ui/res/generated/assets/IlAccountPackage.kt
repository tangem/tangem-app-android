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

private var _il_account_package: ImageVector? = null

val Icons.il_account_package: ImageVector
    get() {
        if (_il_account_package != null) return _il_account_package!!
        _il_account_package = ImageVector.Builder(
            name = "il_account_package",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.4241 16.939L27.0772 21.3569L28.5749 20.493L20.9218 16.0751L19.4241 16.939Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M22.6096 13.3723C23.4704 12.876 24.5306 12.8758 25.3914 13.3723L32.5092 17.4811C33.3707 17.978 33.8995 18.8975 33.8996 19.8905V28.1091C33.8996 29.1034 33.3696 30.0225 32.5082 30.5194L32.5072 30.5184L25.3914 34.6273C24.5306 35.1237 23.4704 35.1245 22.6096 34.6282L22.6086 34.6273L15.4918 30.5184C14.6313 30.0207 14.1004 29.1026 14.1004 28.1081V19.8905C14.1005 18.8963 14.6304 17.9779 15.4918 17.4811L17.9566 16.0583L19.4241 15.2111L22.6096 13.3723ZM23.999 23.1354L27.0772 21.3569L28.5749 20.493L31.6531 18.7155L24.6425 14.6691C24.2449 14.44 23.7551 14.4399 23.3574 14.6691L20.9218 16.0751L19.4241 16.939L17.885 17.8278L16.3459 18.7165L23.999 23.1354ZM18.2745 26.6456C17.9166 26.4384 17.4579 26.5607 17.2507 26.9186C17.0436 27.2764 17.166 27.7351 17.5238 27.9424L20.6146 29.7316C20.9725 29.9388 21.4312 29.8164 21.6384 29.4586C21.8455 29.1007 21.7231 28.643 21.3654 28.4357L18.2745 26.6456Z"),
            )
        }.build()
        return _il_account_package!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountPackagePreview() {
    Icon(
        imageVector = Icons.il_account_package,
        contentDescription = null,
    )
}