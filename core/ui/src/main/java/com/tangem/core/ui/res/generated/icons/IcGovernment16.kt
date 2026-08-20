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

private var _ic_government_16: ImageVector? = null

val Icons.ic_government_16: ImageVector
    get() {
        if (_ic_government_16 != null) return _ic_government_16!!
        _ic_government_16 = ImageVector.Builder(
            name = "ic_government_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.89398 2.75879C7.99846 2.74091 8.10642 2.75045 8.20746 2.78617L12.3666 4.25751C12.6164 4.34589 12.7836 4.58284 12.7836 4.84801V6.00358C12.7835 6.34896 12.5036 6.62917 12.1586 6.62927H12.1274L12.1274 9.5238C12.2616 9.56703 12.38 9.65486 12.4595 9.77603L12.897 10.4428C12.9636 10.5446 12.9995 10.6651 12.9995 10.7869V12.3743C12.999 12.7193 12.7191 12.9997 12.3745 13H3.62549C3.28064 13 3.00102 12.7194 3.00049 12.3743V10.7869C3.00049 10.665 3.03621 10.5447 3.10303 10.4428L3.54053 9.77603C3.6202 9.65451 3.73787 9.56592 3.87256 9.52282L3.8725 6.62927H3.84027C3.49526 6.62918 3.21541 6.34896 3.21527 6.00358V4.84801C3.21527 4.5829 3.38262 4.34593 3.63226 4.25751L7.79144 2.78617L7.89398 2.75879ZM4.25049 10.9726V11.7486H11.7495V10.9726L11.6001 10.7449H4.3999L4.25049 10.9726ZM5.12256 9.49349H6.29639L6.29633 6.62927H5.1225L5.12256 9.49349ZM7.54639 9.49349H8.59326L8.5932 6.62927H7.54633L7.54639 9.49349ZM9.84326 9.49349H10.8774L10.8774 6.62927H9.8432L9.84326 9.49349ZM4.46527 5.2899V5.37789H11.5336V5.2899L7.99945 4.03852L4.46527 5.2899Z"),
            )
        }.build()
        return _ic_government_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGovernment16Preview() {
    Icon(
        imageVector = Icons.ic_government_16,
        contentDescription = null,
    )
}