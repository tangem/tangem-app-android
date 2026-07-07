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

private var _ic_lightning_28: ImageVector? = null

val Icons.ic_lightning_28: ImageVector
    get() {
        if (_ic_lightning_28 != null) return _ic_lightning_28!!
        _ic_lightning_28 = ImageVector.Builder(
            name = "ic_lightning_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.9993 3.97256C12.7106 3.04738 13.8362 2.85973 14.7053 3.10439C15.5766 3.34992 16.4787 4.12782 16.4788 5.36514V10.5399H21.4856C22.4181 10.5401 23.2279 11.0216 23.6545 11.7558C24.0898 12.5052 24.0992 13.4922 23.4875 14.2899L16.0022 24.039C15.2909 24.9643 14.1643 25.1517 13.2952 24.9071C12.4239 24.6616 11.5228 23.8838 11.5227 22.6464V17.4716H6.51586C5.58328 17.4716 4.77304 16.9906 4.34594 16.2558C3.90989 15.5053 3.90181 14.5181 4.51488 13.7206L11.9993 3.97256ZM6.70629 14.9716H12.7727C13.4625 14.9721 14.0226 15.5316 14.0227 16.2216V22.5038C14.0247 22.5037 14.0269 22.504 14.0286 22.5038L21.2952 13.0399H15.2288C14.5385 13.0399 13.9788 12.4802 13.9788 11.7899V5.50674C13.9764 5.50691 13.9738 5.50657 13.9719 5.50674L6.70629 14.9716Z"),
            )
        }.build()
        return _ic_lightning_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLightning28Preview() {
    Icon(
        imageVector = Icons.ic_lightning_28,
        contentDescription = null,
    )
}