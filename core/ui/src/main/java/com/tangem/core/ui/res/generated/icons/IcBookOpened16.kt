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

private var _ic_book_opened_16: ImageVector? = null

val Icons.ic_book_opened_16: ImageVector
    get() {
        if (_ic_book_opened_16 != null) return _ic_book_opened_16!!
        _ic_book_opened_16 = ImageVector.Builder(
            name = "ic_book_opened_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.05566 2.99976C6.7486 2.99987 7.40529 3.29201 7.88379 3.79858C7.92476 3.84198 7.96222 3.88848 8 3.93433C8.46581 3.36729 9.15507 2.99976 9.94531 2.99976H11.6465C12.0174 2.99979 12.3655 3.1561 12.6162 3.42163C12.8657 3.6859 12.9999 4.03711 13 4.39624V10.574C12.9999 11.3098 12.4288 11.9685 11.6494 11.9705H9.5752C9.16509 11.9698 8.7674 12.2155 8.56641 12.6414C8.44159 12.9059 8.1497 13.0491 7.86426 12.9851C7.67067 12.9417 7.51662 12.8103 7.43652 12.6394L7.43359 12.6414C7.23264 12.2158 6.83567 11.97 6.42578 11.9705H4.35352C3.98261 11.9704 3.63448 11.8141 3.38379 11.5486C3.13438 11.2843 3.00007 10.933 3 10.574V4.39624C3.00013 4.03722 3.13436 3.68583 3.38379 3.42163C3.63448 3.1562 3.98268 2.99982 4.35352 2.99976H6.05566ZM4.35352 4.24976C4.33799 4.24981 4.31541 4.25633 4.29297 4.28003C4.26933 4.30508 4.25014 4.34636 4.25 4.39624V10.574C4.25007 10.6239 4.26935 10.6651 4.29297 10.6902C4.31545 10.714 4.33796 10.7204 4.35352 10.7205H6.4248C6.75975 10.7201 7.08128 10.7931 7.37402 10.9255V5.68335C7.37394 5.29214 7.22701 4.92337 6.97559 4.65698C6.72524 4.39191 6.3933 4.24987 6.05566 4.24976H4.35352ZM9.94531 4.24976C9.25026 4.24976 8.62602 4.85731 8.62598 5.68335V10.9255C8.91874 10.793 9.24022 10.7202 9.5752 10.7205H11.6465C11.6631 10.7204 11.6844 10.7142 11.707 10.6902C11.7303 10.6655 11.7499 10.6259 11.75 10.574V4.39624C11.7499 4.34623 11.7307 4.30511 11.707 4.28003C11.6845 4.25619 11.662 4.24979 11.6465 4.24976H9.94531Z"),
            )
        }.build()
        return _ic_book_opened_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBookOpened16Preview() {
    Icon(
        imageVector = Icons.ic_book_opened_16,
        contentDescription = null,
    )
}