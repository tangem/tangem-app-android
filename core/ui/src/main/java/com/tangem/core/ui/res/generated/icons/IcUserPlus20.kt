@file:Suppress("all")

package com.tangem.core.ui.res.generated.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _ic_user_plus_20: ImageVector? = null

val Icons.ic_user_plus_20: ImageVector
    get() {
        if (_ic_user_plus_20 != null) return _ic_user_plus_20!!
        _ic_user_plus_20 = ImageVector.Builder(
            name = "ic_user_plus_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.9751 11.2271C10.3888 11.2275 10.7249 11.5633 10.7251 11.9771C10.7249 12.3908 10.3888 12.7266 9.9751 12.7271H6.57959C5.43038 12.7271 4.49875 13.6589 4.49854 14.8081V15.5161C4.49827 15.93 4.1624 16.2659 3.74854 16.2661C3.33448 16.2661 2.9988 15.9301 2.99854 15.5161V14.8081C2.99875 12.8305 4.60195 11.2271 6.57959 11.2271H9.9751Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.1274 10.519C14.5416 10.519 14.8773 10.855 14.8774 11.269V12.6421H16.2515C16.6651 12.6424 17.001 12.9784 17.0015 13.3921C17.0015 13.8061 16.6654 14.1417 16.2515 14.1421H14.8774V15.5161C14.8771 15.93 14.5414 16.2661 14.1274 16.2661C13.7135 16.266 13.3778 15.93 13.3774 15.5161V14.1421H12.0044C11.5902 14.1421 11.2544 13.8063 11.2544 13.3921C11.2548 12.9782 11.5904 12.6421 12.0044 12.6421H13.3774V11.269C13.3776 10.855 13.7134 10.5191 14.1274 10.519Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99951 2.73389C11.9773 2.73392 13.5805 4.33719 13.5806 6.31494C13.5805 8.29268 11.9773 9.89596 9.99951 9.896C8.02178 9.89594 6.41852 8.29267 6.41846 6.31494C6.41851 4.3372 8.02177 2.73394 9.99951 2.73389ZM9.99951 4.23389C8.8502 4.23394 7.91851 5.16563 7.91846 6.31494C7.91852 7.46424 8.85021 8.39594 9.99951 8.396C11.1488 8.39596 12.0805 7.46425 12.0806 6.31494C12.0805 5.16562 11.1488 4.23392 9.99951 4.23389Z"),
            )
        }.build()
        return _ic_user_plus_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUserPlus20Preview() {
    Icon(
        imageVector = Icons.ic_user_plus_20,
        contentDescription = null,
    )
}