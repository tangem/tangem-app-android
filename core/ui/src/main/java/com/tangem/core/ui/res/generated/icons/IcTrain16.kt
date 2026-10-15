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

private var _ic_train_16: ImageVector? = null

val Icons.ic_train_16: ImageVector
    get() {
        if (_ic_train_16 != null) return _ic_train_16!!
        _ic_train_16 = ImageVector.Builder(
            name = "ic_train_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.25879 9.3418C5.43289 9.04401 5.81628 8.94328 6.11426 9.11719L6.66699 9.43945C6.96491 9.61338 7.0662 9.99689 6.89258 10.2949C6.71859 10.593 6.3352 10.6934 6.03711 10.5195L5.4834 10.1963C5.18563 10.0223 5.08513 9.63977 5.25879 9.3418Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.88672 9.11719C10.1847 8.94346 10.5682 9.04394 10.7422 9.3418C10.9158 9.63977 10.8144 10.0223 10.5166 10.1963L9.96387 10.5195C9.66578 10.6935 9.28242 10.593 9.1084 10.2949C8.93462 9.99688 9.03507 9.61344 9.33301 9.43945L9.88672 9.11719Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.66113 2C10.0061 2.00026 10.2861 2.27998 10.2861 2.625C10.286 2.96988 10.006 3.24974 9.66113 3.25H9.45508V3.69727H9.66113C11.2417 3.69748 12.4999 5.00408 12.5 6.58594V9.65723C12.5 10.3902 12.303 10.9751 11.918 11.4648C11.7255 11.7096 11.4933 11.9186 11.2402 12.1074L11.8447 13.0342C12.0333 13.3233 11.9522 13.7108 11.6631 13.8994C11.374 14.0878 10.9864 14.0057 10.7979 13.7168L10.1816 12.7725L9.96289 12.9014C8.74688 13.6113 7.25311 13.6113 6.03711 12.9014L5.81738 12.7725L5.20215 13.7168C5.0135 14.0057 4.62595 14.088 4.33691 13.8994C4.04806 13.7108 3.96688 13.3232 4.15527 13.0342L4.75879 12.1074C4.506 11.9187 4.27437 11.7094 4.08203 11.4648C3.69698 10.9751 3.5 10.3902 3.5 9.65723V6.58594C3.50013 5.00409 4.75832 3.6975 6.33887 3.69727H6.54492V3.25H6.33984C5.99477 3.25 5.71501 2.97004 5.71484 2.625C5.71484 2.27982 5.99467 2 6.33984 2H9.66113ZM4.75 9.65723C4.75 10.1422 4.87261 10.4461 5.06543 10.6914C5.27741 10.961 5.60766 11.2037 6.11328 11.499L6.66699 11.8223C7.49362 12.3049 8.50638 12.3049 9.33301 11.8223L9.88672 11.499L10.2334 11.2861C10.5498 11.0807 10.7755 10.8937 10.9346 10.6914C11.1274 10.4461 11.25 10.1422 11.25 9.65723V8.34277H4.75V9.65723ZM6.33887 4.94727C5.47427 4.9475 4.75013 5.66857 4.75 6.58594V7.09277H11.25V6.58594C11.2499 5.66855 10.5257 4.94748 9.66113 4.94727H6.33887ZM7.79492 3.69727H8.20508V3.25H7.79492V3.69727Z"),
            )
        }.build()
        return _ic_train_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTrain16Preview() {
    Icon(
        imageVector = Icons.ic_train_16,
        contentDescription = null,
    )
}