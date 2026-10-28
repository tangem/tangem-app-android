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

private var _ic_wallet_plus_20: ImageVector? = null

val Icons.ic_wallet_plus_20: ImageVector
    get() {
        if (_ic_wallet_plus_20 != null) return _ic_wallet_plus_20!!
        _ic_wallet_plus_20 = ImageVector.Builder(
            name = "ic_wallet_plus_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.2402 3.50732L13.2393 3.5083C13.3463 3.5083 13.4204 3.50791 13.4854 3.51025C15.2749 3.57524 16.7116 5.01066 16.7764 6.80029C16.7774 6.83163 16.7761 7.00045 16.7764 7.18506C17.5326 7.32223 18 8.04362 18 8.74756V11.2583C17.9998 11.962 17.5323 12.6816 16.7764 12.8188C16.7759 12.9984 16.7765 13.1711 16.7754 13.2046C16.7107 14.9943 15.275 16.4306 13.4854 16.4956C13.4204 16.498 13.3463 16.4976 13.2393 16.4976H10.6943C10.2801 16.4976 9.94434 16.1618 9.94434 15.7476C9.94453 15.3335 10.2802 14.9976 10.6943 14.9976H13.2393C13.3589 14.9976 13.3996 14.9977 13.4307 14.9966C14.4345 14.9601 15.24 14.1546 15.2764 13.1509C15.2764 13.1472 15.2771 13.1147 15.2773 13.0464C15.2775 12.9898 15.2774 12.9194 15.2773 12.8452H14.6777C13.1605 12.8451 12.0901 11.4774 12.0898 10.0024C12.0901 8.52743 13.1605 7.16078 14.6777 7.16064H15.2783C15.2781 7.09129 15.2775 7.02606 15.2773 6.97217C15.2771 6.89915 15.2773 6.86182 15.2773 6.85498C15.2411 5.85119 14.4345 5.04579 13.4307 5.00928C13.3996 5.00816 13.3588 5.00733 13.2393 5.00732L7.01562 5.00244C6.25725 5.00244 5.73299 5.00343 5.32617 5.03662C4.92808 5.06915 4.70771 5.12902 4.5459 5.21143C4.18561 5.39509 3.89265 5.68807 3.70898 6.04834C3.62659 6.21013 3.5667 6.43057 3.53418 6.82861C3.50099 7.23542 3.5 7.75971 3.5 8.51807C3.49974 8.9319 3.16384 9.26782 2.75 9.26807C2.33595 9.26807 2.00026 8.93206 2 8.51807C2 7.7844 1.99974 7.18855 2.03906 6.70654C2.07917 6.21569 2.16402 5.77611 2.37207 5.36768C2.6996 4.72488 3.22242 4.20203 3.86523 3.87451C4.2737 3.66644 4.71319 3.58161 5.2041 3.5415C5.68611 3.50217 6.28193 3.50244 7.01562 3.50244L13.2402 3.50732ZM14.6777 8.66064C14.1659 8.6608 13.5901 9.16706 13.5898 10.0024C13.5901 10.8378 14.1659 11.3451 14.6777 11.3452H16.4766C16.4863 11.3298 16.4999 11.301 16.5 11.2583V8.74756C16.5 8.70479 16.4863 8.67624 16.4766 8.66064H14.6777Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.35254 10.2446C5.76675 10.2446 6.10254 10.5804 6.10254 10.9946V12.5981H7.70508C8.11926 12.5981 8.45503 12.934 8.45508 13.3481C8.45503 13.7623 8.11926 14.0981 7.70508 14.0981H6.10254V15.7007C6.1025 16.1149 5.76673 16.4507 5.35254 16.4507C4.93844 16.4506 4.60258 16.1148 4.60254 15.7007V14.0981H3C2.58581 14.0981 2.25004 13.7623 2.25 13.3481C2.25005 12.934 2.58582 12.5981 3 12.5981H4.60254V10.9946C4.60254 10.5805 4.93842 10.2447 5.35254 10.2446Z"),
            )
        }.build()
        return _ic_wallet_plus_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletPlus20Preview() {
    Icon(
        imageVector = Icons.ic_wallet_plus_20,
        contentDescription = null,
    )
}