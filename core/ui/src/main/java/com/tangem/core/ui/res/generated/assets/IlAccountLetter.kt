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

private var _il_account_letter: ImageVector? = null

val Icons.il_account_letter: ImageVector
    get() {
        if (_il_account_letter != null) return _il_account_letter!!
        _il_account_letter = ImageVector.Builder(
            name = "il_account_letter",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M28.8284 25.5186C28.8282 26.206 28.2363 26.7242 27.4432 26.7244C26.8613 26.7244 26.4435 26.4335 26.4434 25.9364C26.4434 25.455 26.7868 25.1855 27.522 25.1379L28.8284 25.0477V25.5186Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.9642 21.4398L22.0009 24.6138H19.8267L20.874 21.4398H20.9642Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.0005 14C28.7143 14 31.0715 13.9996 32.536 15.464C34.0004 16.9285 34 19.2857 34 23.9995C34 28.7136 34.0004 31.0715 32.536 32.536C31.0715 34.0003 28.7142 34 24.0005 34C19.2864 34 16.9285 34.0004 15.464 32.536C13.9996 31.0715 14 28.7136 14 23.9995C14 19.2858 13.9997 16.9285 15.464 15.464C16.9285 13.9996 19.2864 14 24.0005 14ZM27.802 21.7465C26.427 21.7466 25.4697 22.3921 25.3639 23.4023H26.6019C26.7237 23.0057 27.1364 22.789 27.7394 22.789C28.4479 22.7891 28.8284 23.1116 28.8284 23.6881V24.1856L27.3368 24.2749C25.9298 24.3542 25.1417 24.9632 25.1417 26C25.1419 27.0578 25.9407 27.7346 27.0567 27.7346C27.8131 27.7346 28.4478 27.4066 28.7705 26.846H28.8607V27.6397H30.1244V23.6881C30.1244 22.4662 29.2885 21.7465 27.802 21.7465ZM20.1818 20.0062L17.4731 27.6397H18.8535L19.5096 25.6667H22.3181L22.9637 27.6397H24.4287L21.7152 20.0062H20.1818Z"),
            )
        }.build()
        return _il_account_letter!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountLetterPreview() {
    Icon(
        imageVector = Icons.il_account_letter,
        contentDescription = null,
    )
}