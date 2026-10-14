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

private var _il_account_shirt: ImageVector? = null

val Icons.il_account_shirt: ImageVector
    get() {
        if (_il_account_shirt != null) return _il_account_shirt!!
        _il_account_shirt = ImageVector.Builder(
            name = "il_account_shirt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.5896 3.33008C14.7217 3.33009 14.8533 3.3369 14.9842 3.34961C14.9891 3.35004 14.994 3.35007 14.9988 3.35059C15.4587 3.39691 15.9089 3.5211 16.3289 3.71875L17.827 4.42383C18.8931 4.9258 19.6906 5.86409 20.0145 6.99707L21.0262 10.5371C21.145 10.9532 21.0879 11.4002 20.8689 11.7734C20.6499 12.1467 20.2872 12.4143 19.866 12.5137L17.741 13.0137L17.742 17.8711C17.7503 18.9575 17.0259 19.9135 15.9773 20.2002C15.969 20.2025 15.9603 20.205 15.952 20.207C13.3461 20.8236 10.632 20.8237 8.02617 20.207C8.01804 20.2051 8.00982 20.2024 8.00176 20.2002C6.95075 19.9128 6.22524 18.9528 6.23711 17.8633V13.0078L4.13457 12.5137C3.71329 12.4144 3.35071 12.1467 3.13164 11.7734C2.91262 11.4001 2.85555 10.9533 2.97441 10.5371L3.98613 6.99707C4.31002 5.86402 5.10744 4.92578 6.17363 4.42383L7.67168 3.71875C8.21562 3.4628 8.80979 3.33013 9.41094 3.33008L9.99687 4.83008C10.0815 5.01125 10.1956 5.17914 10.3396 5.32324C10.6648 5.64844 11.1063 5.83096 11.5662 5.83105H12.4334C12.8932 5.83098 13.3348 5.64835 13.66 5.32324C13.804 5.17918 13.9181 5.01122 14.0027 4.83008L14.5896 3.33008Z"),
            )
        }.build()
        return _il_account_shirt!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountShirtPreview() {
    Icon(
        imageVector = Icons.il_account_shirt,
        contentDescription = null,
    )
}