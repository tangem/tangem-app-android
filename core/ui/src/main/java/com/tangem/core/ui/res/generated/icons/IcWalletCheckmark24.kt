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

private var _ic_wallet_checkmark_24: ImageVector? = null

val Icons.ic_wallet_checkmark_24: ImageVector
    get() {
        if (_ic_wallet_checkmark_24 != null) return _ic_wallet_checkmark_24!!
        _ic_wallet_checkmark_24 = ImageVector.Builder(
            name = "ic_wallet_checkmark_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.4218 4.00244C18.6832 4.08413 20.498 5.89828 20.58 8.15967C20.581 8.1878 20.5795 8.31134 20.58 8.46729C21.4638 8.7106 21.9999 9.58778 21.9999 10.4399V13.563C21.9997 14.4147 21.464 15.2909 20.5809 15.5347C20.5807 15.6849 20.582 15.8067 20.5809 15.8364C20.4992 18.0979 18.6841 19.9117 16.4227 19.9937C16.3403 19.9966 16.2469 19.9966 16.1141 19.9966H12.9481C12.3959 19.9966 11.9482 19.5488 11.9481 18.9966C11.9482 18.4444 12.3959 17.9966 12.9481 17.9966H16.1141C16.2643 17.9966 16.3132 17.997 16.3505 17.9956C17.5644 17.9516 18.538 16.9774 18.5819 15.7642C18.5817 15.7699 18.5825 15.7329 18.5829 15.6372V15.604H17.7997C15.8674 15.6037 14.5136 13.865 14.5135 12.0015C14.5135 10.1379 15.8673 8.39921 17.7997 8.39893H18.5819V8.23193C18.5378 7.01877 17.5635 6.04531 16.3495 6.00147C16.3123 6.00014 16.2633 5.99951 16.1141 5.99951H8.30652C7.36159 5.99951 6.71266 6.00043 6.20984 6.0415C5.71871 6.08164 5.45481 6.15551 5.26453 6.25244C4.82894 6.47445 4.47485 6.82876 4.25281 7.26416C4.15589 7.45432 4.08201 7.71852 4.04187 8.20947C4.00083 8.71208 3.99988 9.36074 3.99988 10.3052C3.99988 10.8575 3.55216 11.3052 2.99988 11.3052C2.44771 11.305 1.99988 10.8574 1.99988 10.3052C1.99988 9.39352 1.99945 8.64934 2.04871 8.04639C2.09896 7.43165 2.20603 6.87505 2.47058 6.35596C2.88449 5.54406 3.54525 4.88391 4.3573 4.47021C4.87633 4.20586 5.43209 4.09858 6.04675 4.04834C6.64986 3.99907 7.39451 3.99951 8.30652 3.99951H16.1141C16.2465 3.99951 16.3395 3.99949 16.4218 4.00244ZM17.7997 10.3989C17.2076 10.3992 16.5135 10.991 16.5135 12.0015C16.5136 13.0118 17.2076 13.6037 17.7997 13.604H19.993C19.9958 13.5928 19.9999 13.5794 19.9999 13.563V10.4399C19.9999 10.4236 19.9957 10.4102 19.993 10.3989H17.7997Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.16003 13.9653C9.55059 13.5752 10.1837 13.5749 10.5741 13.9653C10.9642 14.3558 10.9642 14.989 10.5741 15.3794L6.39636 19.5571C6.00595 19.9475 5.37285 19.9473 4.9823 19.5571L2.47546 17.0503C2.08502 16.6598 2.08497 16.0267 2.47546 15.6362C2.86598 15.246 3.49908 15.2459 3.88953 15.6362L5.68933 17.436L9.16003 13.9653Z"),
            )
        }.build()
        return _ic_wallet_checkmark_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletCheckmark24Preview() {
    Icon(
        imageVector = Icons.ic_wallet_checkmark_24,
        contentDescription = null,
    )
}