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

private var _ic_graduation_cap_20: ImageVector? = null

val Icons.ic_graduation_cap_20: ImageVector
    get() {
        if (_ic_graduation_cap_20 != null) return _ic_graduation_cap_20!!
        _ic_graduation_cap_20 = ImageVector.Builder(
            name = "ic_graduation_cap_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.11035 4.00003C9.28535 3.33365 10.7147 3.33365 11.8896 4.00003L15.9766 6.32034C17.3413 7.09557 17.3412 9.08701 15.9766 9.86234L14.7256 10.5723V12.7891C14.7256 13.6738 14.2929 14.5083 13.5605 15.0059L12.498 15.7285C11.0805 16.6926 9.25842 16.7527 7.79004 15.9092L7.50195 15.7285L6.43848 15.0049C5.70761 14.5077 5.27344 13.675 5.27344 12.7891V10.5713L4.02344 9.86234C2.65888 9.08703 2.65883 7.09561 4.02344 6.32034L8.11035 4.00003ZM11.8906 12.1826L11.8896 12.1836C10.7147 12.8498 9.28383 12.8493 8.10937 12.1826L6.77344 11.4239V12.7891C6.77344 13.1868 6.96826 13.5515 7.28125 13.7647L8.34473 14.4883C9.34834 15.1705 10.6515 15.1702 11.6543 14.4883L12.7178 13.7647C13.0309 13.5519 13.2256 13.1869 13.2256 12.7891V11.4239L11.8906 12.1826ZM11.1494 5.30374C10.4335 4.89801 9.56543 4.89773 8.84961 5.30374L4.76465 7.62503C4.41205 7.82539 4.41196 8.35836 4.76465 8.55863L8.85058 10.8789C9.56551 11.2846 10.4335 11.2847 11.1494 10.8789L15.2354 8.55863C15.5881 8.35834 15.5881 7.82534 15.2354 7.62503L11.1494 5.30374Z"),
            )
        }.build()
        return _ic_graduation_cap_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGraduationCap20Preview() {
    Icon(
        imageVector = Icons.ic_graduation_cap_20,
        contentDescription = null,
    )
}