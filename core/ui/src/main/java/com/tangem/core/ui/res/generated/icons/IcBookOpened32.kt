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

private var _ic_book_opened_32: ImageVector? = null

val Icons.ic_book_opened_32: ImageVector
    get() {
        if (_ic_book_opened_32 != null) return _ic_book_opened_32!!
        _ic_book_opened_32 = ImageVector.Builder(
            name = "ic_book_opened_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.8875 3.5C12.6466 3.5 14.3301 4.20799 15.5681 5.46191C15.7204 5.61619 15.8642 5.77734 15.9998 5.94434C17.2089 4.45469 19.0452 3.50006 21.1121 3.5H25.5847C26.4967 3.50013 27.3677 3.86748 28.0066 4.51465C28.6448 5.16128 29.0007 6.03431 29.0007 6.94043V22.4717C29.0006 24.3498 27.4942 25.907 25.5935 25.9121H20.1394C18.9666 25.9105 17.8823 26.5814 17.3484 27.6631C17.04 28.2882 16.3407 28.619 15.6619 28.4609C15.212 28.356 14.8495 28.0544 14.656 27.6602L14.6521 27.6631C14.1182 26.5811 13.0332 25.9104 11.8601 25.9121H6.41479C5.50281 25.912 4.63181 25.5447 3.99292 24.8975C3.35469 24.2509 2.99983 23.3778 2.99976 22.4717V6.94043C2.99976 6.0343 3.35471 5.16129 3.99292 4.51465C4.63181 3.86744 5.50282 3.50013 6.41479 3.5H10.8875ZM6.41479 6.5C6.31114 6.50013 6.20797 6.54184 6.12866 6.62207C6.04856 6.70321 5.99976 6.8174 5.99976 6.94043V22.4717C5.99983 22.5946 6.04862 22.709 6.12866 22.79C6.20796 22.8702 6.3112 22.912 6.41479 22.9121H11.8582C12.791 22.9111 13.6886 23.1267 14.4978 23.5166V10.1758C14.4977 9.19472 14.1124 8.25722 13.4333 7.56934C12.7549 6.8822 11.8383 6.5 10.8875 6.5H6.41479ZM21.1121 6.5C19.1369 6.5001 17.5028 8.1274 17.5027 10.1758V23.5166C18.3115 23.127 19.209 22.9112 20.1414 22.9121H25.5857C25.7964 22.9113 26.0006 22.7325 26.0007 22.4717V6.94043C26.0007 6.81752 25.9518 6.7032 25.8718 6.62207C25.7925 6.54172 25.6885 6.50013 25.5847 6.5H21.1121Z"),
            )
        }.build()
        return _ic_book_opened_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBookOpened32Preview() {
    Icon(
        imageVector = Icons.ic_book_opened_32,
        contentDescription = null,
    )
}