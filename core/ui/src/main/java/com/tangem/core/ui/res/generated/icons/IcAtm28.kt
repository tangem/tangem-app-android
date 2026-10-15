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

private var _ic_atm_28: ImageVector? = null

val Icons.ic_atm_28: ImageVector
    get() {
        if (_ic_atm_28 != null) return _ic_atm_28!!
        _ic_atm_28 = ImageVector.Builder(
            name = "ic_atm_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.624 16.7333C16.3141 16.7336 16.874 17.2931 16.874 17.9833V19.9852C16.8738 20.6752 16.314 21.2349 15.624 21.2352C14.9338 21.2352 14.3742 20.6754 14.374 19.9852V17.9833C14.374 17.2929 14.9337 16.7333 15.624 16.7333Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.583 3.49988C23.4757 3.49988 25 5.0391 25 6.92664V11.2792C25 13.1667 23.4757 14.7059 21.583 14.7059H20.666V21.0741C20.6659 22.9614 19.1425 24.4997 17.25 24.4999H10.75C8.85737 24.4999 7.33317 22.9615 7.33301 21.0741V14.7059H6.41699C4.52426 14.7059 3 13.1667 3 11.2792V6.92664C3 5.0391 4.52426 3.49988 6.41699 3.49988H21.583ZM13.084 21.9999H17.25C17.7512 21.9997 18.1659 21.5913 18.166 21.0741V10.3534H13.084V21.9999ZM9.83301 21.0741C9.83315 21.5329 10.1603 21.9047 10.584 21.9833V10.3534H9.83301V21.0741ZM6.41699 5.99988C5.91556 5.99988 5.5 6.40921 5.5 6.92664V11.2792C5.5 11.7966 5.91556 12.2059 6.41699 12.2059H7.33301V9.10339C7.33307 8.41309 7.89269 7.85339 8.58301 7.85339H19.416C20.1063 7.85339 20.6659 8.41309 20.666 9.10339V12.2059H21.583C22.0844 12.2059 22.5 11.7966 22.5 11.2792V6.92664C22.5 6.40921 22.0844 5.99988 21.583 5.99988H6.41699Z"),
            )
        }.build()
        return _ic_atm_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtm28Preview() {
    Icon(
        imageVector = Icons.ic_atm_28,
        contentDescription = null,
    )
}