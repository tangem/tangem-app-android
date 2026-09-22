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

private var _ic_star_24_filled: ImageVector? = null

val Icons.ic_star_24_filled: ImageVector
    get() {
        if (_ic_star_24_filled != null) return _ic_star_24_filled!!
        _ic_star_24_filled = ImageVector.Builder(
            name = "ic_star_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.0001 2.47266C12.9184 2.47277 13.7578 2.9936 14.167 3.81543L15.9542 7.38086L19.9131 7.94727C20.8323 8.07624 21.5976 8.71996 21.8819 9.60352C22.1657 10.4866 21.9198 11.4541 21.2491 12.0947L21.2501 12.0957L18.4083 14.8174L19.0821 18.6836C19.243 19.5918 18.8748 20.5131 18.1309 21.0586C17.387 21.6041 16.397 21.6796 15.5792 21.2529L12.0001 19.4023L8.41998 21.251L8.42096 21.252C7.6032 21.6786 6.61313 21.6029 5.8692 21.0576C5.12628 20.5127 4.7574 19.5928 4.91705 18.6855L5.59185 14.8174L2.75006 12.0957L2.75103 12.0947C2.08051 11.454 1.83434 10.4865 2.11822 9.60352C2.38477 8.77507 3.07356 8.15769 3.91607 7.97754L4.08697 7.94727L8.04498 7.38086L9.83307 3.81543C10.2423 2.99346 11.0816 2.47275 12.0001 2.47266Z"),
            )
        }.build()
        return _ic_star_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcStar24FilledPreview() {
    Icon(
        imageVector = Icons.ic_star_24_filled,
        contentDescription = null,
    )
}