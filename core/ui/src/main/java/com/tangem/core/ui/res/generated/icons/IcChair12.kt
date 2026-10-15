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

private var _ic_chair_12: ImageVector? = null

val Icons.ic_chair_12: ImageVector
    get() {
        if (_ic_chair_12 != null) return _ic_chair_12!!
        _ic_chair_12 = ImageVector.Builder(
            name = "ic_chair_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.77816 1.5C8.79038 1.50025 9.61099 2.32074 9.61116 3.33301V5.14062C9.8452 5.2273 10.0544 5.37761 10.2117 5.5791C10.4698 5.90964 10.5604 6.34123 10.4588 6.74805L10.0067 8.55957C9.94039 8.82443 9.79958 9.05491 9.61116 9.23242V10C9.611 10.2758 9.3869 10.4996 9.11116 10.5C8.83512 10.5 8.61133 10.276 8.61116 10V9.61133H3.38851V10C3.38834 10.2757 3.16414 10.4995 2.88851 10.5C2.61247 10.5 2.38867 10.276 2.38851 10V9.2334C2.19964 9.0558 2.0594 8.82482 1.993 8.55957L1.54085 6.74805C1.43921 6.34113 1.53076 5.90969 1.7889 5.5791C1.94633 5.37778 2.15467 5.22731 2.38851 5.14062V3.33301C2.38868 2.32074 3.20929 1.50025 4.22152 1.5H7.77816ZM9.13753 6.05566C8.97125 6.05566 8.82636 6.1689 8.78597 6.33008L8.50472 7.45508C8.44899 7.67736 8.24852 7.83282 8.01937 7.83301H3.9803C3.75101 7.83301 3.55074 7.67744 3.49495 7.45508L3.2137 6.33008C3.17338 6.16899 3.02828 6.05585 2.86214 6.05566C2.75066 6.05571 2.64563 6.10753 2.57698 6.19531C2.50832 6.28325 2.48353 6.39764 2.51058 6.50586L2.9637 8.31641C3.007 8.48941 3.16221 8.61127 3.34066 8.61133H8.65902C8.83734 8.6111 8.99273 8.48934 9.03597 8.31641L9.48909 6.50586C9.51608 6.39775 9.49122 6.2832 9.42269 6.19531C9.35412 6.10752 9.24895 6.05582 9.13753 6.05566ZM4.22152 2.5C3.76147 2.50025 3.38868 2.87313 3.38851 3.33301V5.16211C3.77557 5.32458 4.07774 5.66126 4.18441 6.08789L4.37093 6.83301H7.62874L7.81527 6.08789C7.92202 5.66105 8.22379 5.32446 8.61116 5.16211V3.33301C8.61099 2.87313 8.2382 2.50025 7.77816 2.5H4.22152Z"),
            )
        }.build()
        return _ic_chair_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChair12Preview() {
    Icon(
        imageVector = Icons.ic_chair_12,
        contentDescription = null,
    )
}