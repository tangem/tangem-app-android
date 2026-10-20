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

private var _ic_hockey_28: ImageVector? = null

val Icons.ic_hockey_28: ImageVector
    get() {
        if (_ic_hockey_28 != null) return _ic_hockey_28!!
        _ic_hockey_28 = ImageVector.Builder(
            name = "ic_hockey_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.7481 2.96958C15.787 2.29018 18.0592 2.21235 19.5499 3.66001C19.5641 3.67298 19.5791 3.68533 19.5928 3.69907C19.6059 3.71213 19.6181 3.72594 19.6309 3.73911L24.2999 8.40806C25.7894 9.8978 25.716 12.1941 25.0303 14.2518C24.3181 16.3891 22.8274 18.7115 20.7696 20.7694C18.7117 22.8272 16.3893 24.3179 14.252 25.0301C12.1943 25.7158 9.898 25.7893 8.40826 24.2997L3.70025 19.5917C2.21064 18.1019 2.28409 15.8057 2.96979 13.7479C3.68203 11.6106 5.17368 9.28912 7.2315 7.2313C9.28933 5.17348 11.6108 3.68182 13.7481 2.96958ZM20.6504 8.2938C20.57 8.7139 20.4597 9.13336 20.3233 9.54282C19.6111 11.6801 18.1204 14.0026 16.0626 16.0604C14.0047 18.1182 11.6823 19.6089 9.54498 20.3211C9.13424 20.458 8.71347 20.5678 8.29205 20.6483L10.1758 22.5321C10.6353 22.9914 11.6859 23.2499 13.462 22.6581C15.1586 22.0926 17.1585 20.8442 19.001 19.0018C20.8437 17.1592 22.0928 15.1585 22.6583 13.4618C23.2501 11.6857 22.9916 10.6351 22.5323 10.1756L20.6504 8.2938ZM17.8243 5.46763C17.3649 5.00834 16.315 4.74997 14.5391 5.34165C12.8423 5.9071 10.8419 7.15609 8.99908 8.99888C7.1563 10.8417 5.9073 12.8421 5.34186 14.5389C4.75533 16.2993 5.00435 17.3458 5.45611 17.8114C5.46022 17.8154 5.46475 17.819 5.46881 17.8231C5.92822 18.2824 6.97875 18.5409 8.75494 17.9491C10.4517 17.3836 12.4514 16.1344 14.294 14.2918C16.1366 12.4493 17.3857 10.4495 17.9512 8.75278C18.5348 7.00158 18.291 5.95656 17.8438 5.48716L17.8243 5.46763Z"),
            )
        }.build()
        return _ic_hockey_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHockey28Preview() {
    Icon(
        imageVector = Icons.ic_hockey_28,
        contentDescription = null,
    )
}