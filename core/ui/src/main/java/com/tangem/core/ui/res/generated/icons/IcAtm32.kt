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

private var _ic_atm_32: ImageVector? = null

val Icons.ic_atm_32: ImageVector
    get() {
        if (_ic_atm_32 != null) return _ic_atm_32!!
        _ic_atm_32 = ImageVector.Builder(
            name = "ic_atm_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.915 19.2366C18.7435 19.2366 19.415 19.9081 19.415 20.7366V23.1174C19.4149 23.9457 18.7434 24.6174 17.915 24.6174C17.087 24.6171 16.4152 23.9456 16.415 23.1174V20.7366C16.415 19.9083 17.0869 19.2369 17.915 19.2366Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.9443 3.50024C27.2025 3.50024 28.9998 5.34789 29 7.58813V12.7649C29 15.0053 27.2026 16.8528 24.9443 16.8528H23.8877V24.4124C23.8875 26.6524 22.091 28.5 19.833 28.5002H12.166C9.90797 28.5001 8.11054 26.6525 8.11035 24.4124V16.8528H7.05566C4.79738 16.8528 3 15.0053 3 12.7649V7.58813C3.00018 5.34789 4.7975 3.50024 7.05566 3.50024H24.9443ZM14.9453 25.5002H19.833C20.3983 25.5 20.8875 25.0316 20.8877 24.4124V11.677H14.9453V25.5002ZM11.1104 15.3333C11.1104 15.3398 11.1113 15.3463 11.1113 15.3528C11.1113 15.359 11.1104 15.3652 11.1104 15.3713V24.4124C11.1105 24.9492 11.479 25.3715 11.9453 25.4749V11.677H11.1104V15.3333ZM7.05566 6.50024C6.49016 6.50024 6.00018 6.96871 6 7.58813V12.7649C6 13.3845 6.49006 13.8528 7.05566 13.8528H8.11035V10.177C8.11035 9.34866 8.78204 8.67713 9.61035 8.677H13.4258C13.4322 8.67692 13.4389 8.67603 13.4453 8.67603C13.4518 8.67603 13.4583 8.67692 13.4648 8.677H22.3877C23.2161 8.677 23.8877 9.34857 23.8877 10.177V13.8528H24.9443C25.5099 13.8528 26 13.3845 26 12.7649V7.58813C25.9998 6.96871 25.5098 6.50024 24.9443 6.50024H7.05566Z"),
            )
        }.build()
        return _ic_atm_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtm32Preview() {
    Icon(
        imageVector = Icons.ic_atm_32,
        contentDescription = null,
    )
}