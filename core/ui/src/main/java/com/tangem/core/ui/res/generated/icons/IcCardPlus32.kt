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

private var _ic_card_plus_32: ImageVector? = null

val Icons.ic_card_plus_32: ImageVector
    get() {
        if (_ic_card_plus_32 != null) return _ic_card_plus_32!!
        _ic_card_plus_32 = ImageVector.Builder(
            name = "ic_card_plus_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.3086 6.5C17.9988 6.5002 18.5586 7.05977 18.5586 7.75C18.5586 8.44023 17.9988 8.9998 17.3086 9H8.16699C6.65816 9.00047 5.50011 10.1774 5.5 11.5498V22.9502C5.50011 24.3226 6.65816 25.4995 8.16699 25.5H23.8369C25.3462 25.5 26.5048 24.3228 26.5049 22.9502V15.3496C26.5051 14.6596 27.0649 14.0998 27.7549 14.0996C28.4451 14.0997 29.0047 14.6595 29.0049 15.3496V22.9502C29.0048 25.7747 26.6546 28 23.8369 28H8.16699C5.34969 27.9995 3.00011 25.7744 3 22.9502V11.5498C3.00011 8.72556 5.34969 6.50048 8.16699 6.5H17.3086Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.3564 21C13.0465 21.0003 13.6062 21.56 13.6064 22.25C13.6063 22.94 13.0465 23.4997 12.3564 23.5H9C8.30975 23.5 7.75018 22.9402 7.75 22.25C7.7502 21.5598 8.30977 21 9 21H12.3564Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M25.3066 4C25.9969 4.00007 26.5566 4.55969 26.5566 5.25V6.5H27.7549C28.4452 6.5 29.0049 7.05964 29.0049 7.75C29.0049 8.44036 28.4452 9 27.7549 9H26.5566V10.25C26.5566 10.9403 25.9969 11.4999 25.3066 11.5C24.6165 11.4998 24.0566 10.9402 24.0566 10.25V9H22.8584C22.1681 8.99995 21.6084 8.44033 21.6084 7.75C21.6084 7.05967 22.1681 6.50005 22.8584 6.5H24.0566V5.25C24.0566 4.55977 24.6165 4.0002 25.3066 4Z"),
            )
        }.build()
        return _ic_card_plus_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCardPlus32Preview() {
    Icon(
        imageVector = Icons.ic_card_plus_32,
        contentDescription = null,
    )
}