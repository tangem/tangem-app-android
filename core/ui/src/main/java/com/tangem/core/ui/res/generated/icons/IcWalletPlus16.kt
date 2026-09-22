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

private var _ic_wallet_plus_16: ImageVector? = null

val Icons.ic_wallet_plus_16: ImageVector
    get() {
        if (_ic_wallet_plus_16 != null) return _ic_wallet_plus_16!!
        _ic_wallet_plus_16 = ImageVector.Builder(
            name = "ic_wallet_plus_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.7646 4.14941C11.7459 4.14955 12.542 4.94549 12.542 5.92676V6.42188C12.8203 6.61852 13.002 6.94298 13.002 7.30957V8.69141C13.0018 9.05759 12.8199 9.38055 12.542 9.57715V10.0732C12.5419 11.0544 11.7458 11.8495 10.7646 11.8496H4.77539C3.79409 11.8496 2.99812 11.0545 2.99805 10.0732V5.92676C2.99805 4.94541 3.79404 4.14941 4.77539 4.14941H10.7646ZM4.77539 5.39941C4.4844 5.39941 4.24805 5.63576 4.24805 5.92676V10.0732C4.24812 10.3642 4.48444 10.5996 4.77539 10.5996H10.7646C11.0555 10.5995 11.2919 10.3641 11.292 10.0732V9.77734H10.7646C9.78373 9.77721 8.98781 8.98181 8.9873 8.00098C8.9873 7.01971 9.78341 6.22376 10.7646 6.22363H11.292V5.92676C11.292 5.63585 11.0555 5.39955 10.7646 5.39941H4.77539ZM10.7646 7.47363C10.4738 7.47377 10.2373 7.71006 10.2373 8.00098C10.2378 8.29146 10.4741 8.52721 10.7646 8.52734H11.752V7.47363H10.7646Z"),
            )
        }.build()
        return _ic_wallet_plus_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletPlus16Preview() {
    Icon(
        imageVector = Icons.ic_wallet_plus_16,
        contentDescription = null,
    )
}