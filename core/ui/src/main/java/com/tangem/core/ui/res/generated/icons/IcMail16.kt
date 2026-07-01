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

private var _ic_mail_16: ImageVector? = null

val Icons.ic_mail_16: ImageVector
    get() {
        if (_ic_mail_16 != null) return _ic_mail_16!!
        _ic_mail_16 = ImageVector.Builder(
            name = "ic_mail_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.9355 5.4375C11.2286 5.25521 11.6135 5.34473 11.7959 5.6377C11.9782 5.9307 11.8886 6.31565 11.5957 6.49805L8.33105 8.5293C8.129 8.6549 7.87295 8.65492 7.6709 8.5293L4.40625 6.49805C4.11345 6.31566 4.02389 5.93067 4.20605 5.6377C4.38838 5.34478 4.77338 5.25535 5.06641 5.4375L8.00098 7.26172L10.9355 5.4375Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.5713 2.5C13.6766 2.50014 14.502 3.44503 14.502 4.52148V11.4775C14.5016 12.486 13.7765 13.3795 12.7754 13.4873L12.5713 13.498H3.43066C2.32546 13.498 1.50023 12.5529 1.5 11.4766V4.52148C1.5 3.445 2.32531 2.50009 3.43066 2.5H12.5713ZM3.43066 3.75C3.09322 3.7501 2.75 4.05515 2.75 4.52148V11.4766C2.75022 11.9426 3.09333 12.248 3.43066 12.248H12.5713C12.9087 12.2479 13.2516 11.9425 13.252 11.4775V4.52148C13.252 4.05519 12.9087 3.75015 12.5713 3.75H3.43066Z"),
            )
        }.build()
        return _ic_mail_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMail16Preview() {
    Icon(
        imageVector = Icons.ic_mail_16,
        contentDescription = null,
    )
}