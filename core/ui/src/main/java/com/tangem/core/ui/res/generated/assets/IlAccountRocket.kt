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

private var _il_account_rocket: ImageVector? = null

val Icons.il_account_rocket: ImageVector
    get() {
        if (_il_account_rocket != null) return _il_account_rocket!!
        _il_account_rocket = ImageVector.Builder(
            name = "il_account_rocket",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.75261 15.0322C6.63944 14.899 7.53743 15.195 8.17156 15.8291C8.80552 16.4632 9.10067 17.3604 8.96746 18.2471C8.78295 19.4742 7.81962 20.4378 6.59246 20.6221L4.14812 20.9893C3.8346 21.0362 3.5169 20.9321 3.29265 20.708C3.06838 20.4837 2.9634 20.1652 3.01043 19.8516L3.37761 17.4072C3.56198 16.1803 4.52574 15.2169 5.75261 15.0322Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.3044 3.00977C20.2568 3.10645 21.0005 3.91074 21.0007 4.88867C21.0007 8.00147 19.5855 10.946 17.155 12.8906L16.5553 13.3701V16.7842C16.5552 17.4995 16.1512 18.1537 15.5114 18.4736L13.3639 19.5469C12.8743 19.7917 12.3023 19.812 11.7966 19.6025C11.2909 19.393 10.9004 18.9744 10.7272 18.4551L10.1979 16.8662C10.0711 16.7892 9.95058 16.6979 9.84148 16.5889L7.41179 14.1582C7.30261 14.049 7.21056 13.9286 7.13347 13.8018L5.54558 13.2725C5.02613 13.0993 4.6067 12.709 4.39715 12.2031C4.18781 11.6974 4.20801 11.1254 4.45281 10.6357L5.52703 8.48828L5.59051 8.37109C5.9286 7.79958 6.54576 7.44434 7.21648 7.44434H10.6315L11.11 6.8457C13.0547 4.41509 15.9992 3 19.112 3L19.3044 3.00977ZM12.3571 16.7285C12.3317 16.7489 12.3042 16.7663 12.278 16.7852L12.5837 17.7002L14.5553 16.7148V14.9697L12.3571 16.7285ZM6.29851 11.415L7.21453 11.7207C7.23333 11.6946 7.25184 11.668 7.27215 11.6426L9.03093 9.44434H7.28484L6.29851 11.415Z"),
            )
        }.build()
        return _il_account_rocket!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountRocketPreview() {
    Icon(
        imageVector = Icons.il_account_rocket,
        contentDescription = null,
    )
}