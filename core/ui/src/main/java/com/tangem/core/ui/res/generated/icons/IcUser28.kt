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

private var _ic_user_28: ImageVector? = null

val Icons.ic_user_28: ImageVector
    get() {
        if (_ic_user_28 != null) return _ic_user_28!!
        _ic_user_28 = ImageVector.Builder(
            name = "ic_user_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.1074 16.387C20.8631 16.387 23.7204 19.4027 23.7207 22.9993C23.7207 23.6896 23.1609 24.2492 22.4707 24.2493C21.7803 24.2493 21.2207 23.6897 21.2207 22.9993C21.2204 20.7462 19.4454 18.887 17.1074 18.887H10.8916C8.63856 18.8872 6.77957 20.6616 6.7793 22.9993C6.7793 23.6897 6.21965 24.2493 5.5293 24.2493C4.83894 24.2493 4.2793 23.6897 4.2793 22.9993C4.27957 19.2439 7.29511 16.3872 10.8916 16.387H17.1074Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.54883 4.05207C12.1021 1.64946 16.0211 1.64916 18.5742 4.05207C18.5784 4.05601 18.5818 4.06079 18.5859 4.06476C18.5908 4.06949 18.5967 4.07361 18.6016 4.07843C21.146 6.62306 21.1853 10.7269 18.5742 13.1849C16.0728 15.539 11.9843 15.7759 9.49512 13.1312C7.14081 10.6297 6.90397 6.54135 9.54883 4.05207ZM16.8457 5.85871C15.2551 4.37576 12.8474 4.38042 11.2617 5.87238C9.76286 7.28325 9.76968 9.77592 11.3154 11.4183C12.7263 12.917 15.219 12.9103 16.8613 11.3646C18.3891 9.92615 18.4321 7.45804 16.8457 5.85871Z"),
            )
        }.build()
        return _ic_user_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser28Preview() {
    Icon(
        imageVector = Icons.ic_user_28,
        contentDescription = null,
    )
}