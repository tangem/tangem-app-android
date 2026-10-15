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

private var _ic_capsule_16: ImageVector? = null

val Icons.ic_capsule_16: ImageVector
    get() {
        if (_ic_capsule_16 != null) return _ic_capsule_16!!
        _ic_capsule_16 = ImageVector.Builder(
            name = "ic_capsule_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.42881 3.78426C9.4745 2.73858 11.1702 2.73858 12.2159 3.78426C13.2616 4.82995 13.2616 6.52568 12.2159 7.57137L7.57139 12.2159C6.5257 13.2616 4.82998 13.2615 3.78428 12.2159C2.73857 11.1702 2.73857 9.47448 3.78428 8.42879L8.42881 3.78426ZM4.66807 9.31258C4.11053 9.87011 4.11053 10.7746 4.66807 11.3321C5.22561 11.8896 6.13007 11.8896 6.6876 11.3321L8.56748 9.45125L6.54795 7.43172L4.66807 9.31258ZM11.3321 4.66805C10.7746 4.11053 9.87014 4.11052 9.3126 4.66805L7.43174 6.54793L9.45127 8.56746L11.3321 6.68758C11.8897 6.13005 11.8897 5.22558 11.3321 4.66805Z"),
            )
        }.build()
        return _ic_capsule_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCapsule16Preview() {
    Icon(
        imageVector = Icons.ic_capsule_16,
        contentDescription = null,
    )
}