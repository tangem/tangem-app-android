@file:Suppress("all")

package com.tangem.core.ui.res.generated.assets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.generated.icons.Icons

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _il_account_beach: ImageVector? = null

val Icons.il_account_beach: ImageVector
    get() {
        if (_il_account_beach != null) return _il_account_beach!!
        _il_account_beach = ImageVector.Builder(
            name = "il_account_beach",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23.7225 13.9133C24.2469 13.7634 24.7936 14.0671 24.9435 14.5916C25.0271 14.8845 25.0918 14.9427 25.3985 14.9069C28.3513 14.5621 31.1623 16.2232 32.4834 18.948C32.6104 19.2094 32.7436 19.4723 32.8203 19.7538C32.9139 20.0968 32.9323 20.5375 32.6551 20.9674C32.3565 21.4305 31.904 21.5753 31.5552 21.6151C31.2836 21.646 30.9615 21.6251 30.689 21.6074L29.2803 21.5168C29.1423 21.508 29.1398 21.5087 29.0269 21.5884L27.8012 22.4546C27.6926 22.5314 27.6383 22.5698 27.6171 22.628C27.596 22.6863 27.6132 22.7506 27.6474 22.8791L28.6837 26.7651C28.7644 27.0678 28.8048 27.2191 28.7215 27.2998C28.6382 27.3805 28.4882 27.3354 28.1882 27.2452L26.9603 26.8761C26.6507 26.783 26.6416 26.7736 26.5583 26.4613L25.8055 23.6381C25.7702 23.5057 25.7525 23.4395 25.7034 23.3992C25.6542 23.3588 25.5858 23.3544 25.4491 23.3456L23.3691 23.212C23.2311 23.2032 23.2287 23.2039 23.1158 23.2836L21.959 24.1011C21.726 24.266 21.4548 24.458 21.2024 24.5764C20.8831 24.7261 20.4018 24.8495 19.8909 24.5851C19.416 24.3393 19.2197 23.9223 19.1411 23.5712C19.0755 23.278 19.0706 22.9743 19.0604 22.6752C18.9603 19.811 20.4267 17.0677 22.8742 15.7179C23.1702 15.5547 23.1323 15.4429 23.0441 15.1342C22.8943 14.6098 23.198 14.0631 23.7225 13.9133Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.7728 26.9614C21.2121 26.9614 23.4127 27.6215 25.5856 28.2732C27.7348 28.9178 29.8849 29.561 32.3891 29.6995C32.8672 29.726 33.1063 29.7392 33.2414 29.882C33.3766 30.0248 33.3766 30.2547 33.3766 30.7145V32.8873C33.3766 33.3529 33.3766 33.5857 33.2319 33.7304C33.0873 33.875 32.8545 33.875 32.3889 33.875H14.6111C14.1455 33.875 13.9127 33.875 13.7681 33.7304C13.6234 33.5857 13.6234 33.3529 13.6234 32.8873V28.4103C13.6234 28.0653 13.6234 27.8928 13.7242 27.7603C13.8249 27.6278 13.9818 27.5843 14.2956 27.4972C14.5316 27.4318 14.7699 27.3739 15.0115 27.3234C15.8858 27.1406 17.1556 26.9614 18.7728 26.9614Z"),
            )
        }.build()
        return _il_account_beach!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountBeachPreview() {
    Icon(
        imageVector = Icons.il_account_beach,
        contentDescription = null,
    )
}