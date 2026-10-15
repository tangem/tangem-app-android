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

private var _ic_at_sign_28: ImageVector? = null

val Icons.ic_at_sign_28: ImageVector
    get() {
        if (_ic_at_sign_28 != null) return _ic_at_sign_28!!
        _ic_at_sign_28 = ImageVector.Builder(
            name = "ic_at_sign_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.3057 14C17.3057 12.1746 15.8254 10.6943 14 10.6943C12.1746 10.6943 10.6943 12.1746 10.6943 14C10.6943 15.8254 12.1746 17.3057 14 17.3057C15.8254 17.3057 17.3057 15.8254 17.3057 14ZM19.8057 15.708C19.8057 16.5903 20.5201 17.3054 21.4023 17.3057C22.2848 17.3057 23 16.5905 23 15.708V14C23 9.02894 18.9711 5 14 5C9.02894 5 5 9.02894 5 14C5 18.9711 9.02894 23 14 23C15.1495 23 16.3165 22.7823 17.4434 22.3154C18.0456 22.0658 18.6075 21.7504 19.1318 21.3857C19.6986 20.9915 20.4779 21.1315 20.8721 21.6982C21.2662 22.265 21.1263 23.0443 20.5596 23.4385C19.9019 23.8959 19.1838 24.3007 18.4014 24.625C16.9615 25.2216 15.4674 25.5 14 25.5C7.64823 25.5 2.5 20.3518 2.5 14C2.5 7.64823 7.64823 2.5 14 2.5C20.3518 2.5 25.5 7.64823 25.5 14V15.708C25.5 17.9712 23.6655 19.8057 21.4023 19.8057C20.0407 19.8055 18.8347 19.1411 18.0898 18.1191C17.0404 19.1612 15.5956 19.8057 14 19.8057C10.7938 19.8057 8.19434 17.2062 8.19434 14C8.19434 10.7938 10.7938 8.19434 14 8.19434C17.2062 8.19434 19.8057 10.7938 19.8057 14V15.708Z"),
            )
        }.build()
        return _ic_at_sign_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtSign28Preview() {
    Icon(
        imageVector = Icons.ic_at_sign_28,
        contentDescription = null,
    )
}