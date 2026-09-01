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

private var _ic_wallet_28: ImageVector? = null

val Icons.ic_wallet_28: ImageVector
    get() {
        if (_ic_wallet_28 != null) return _ic_wallet_28!!
        _ic_wallet_28 = ImageVector.Builder(
            name = "ic_wallet_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.2568 3.55542C15.7156 2.58227 17.6799 2.919 18.7314 4.32202L20.8691 7.17456C22.637 7.50794 23.9734 9.06016 23.9736 10.9246V12.1033C24.5919 12.5106 25 13.2113 25 14.0066V17.0876C24.9998 17.8824 24.5914 18.5809 23.9736 18.988V20.1677C23.9736 22.2753 22.2664 23.9851 20.1582 23.9851H6.81543C4.70739 23.9849 3 22.2752 3 20.1677V10.9246C3.00023 8.8173 4.70753 7.10739 6.81543 7.10718H8.93262L14.2568 3.55542ZM6.81543 9.60718C6.08984 9.60739 5.50023 10.1964 5.5 10.9246V20.1677C5.5 20.8961 6.0897 21.4849 6.81543 21.4851H20.1582C20.8841 21.4851 21.4736 20.8962 21.4736 20.1677V19.364H20.1572C18.0493 19.3638 16.3419 17.654 16.3418 15.5466C16.3419 13.4393 18.0492 11.7295 20.1572 11.7292H21.4736V10.9246C21.4734 10.1963 20.8839 9.60723 20.1582 9.60718H9.32324C9.31086 9.6073 9.2985 9.60742 9.28613 9.60718H6.81543ZM20.1572 14.2292C19.4316 14.2295 18.8419 14.8184 18.8418 15.5466C18.8419 16.2749 19.4316 16.8638 20.1572 16.864H22.5V14.2292H20.1572ZM16.7305 5.82104C16.4747 5.48001 15.9976 5.39834 15.6436 5.63452L13.4365 7.10718H17.6943L16.7305 5.82104Z"),
            )
        }.build()
        return _ic_wallet_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWallet28Preview() {
    Icon(
        imageVector = Icons.ic_wallet_28,
        contentDescription = null,
    )
}