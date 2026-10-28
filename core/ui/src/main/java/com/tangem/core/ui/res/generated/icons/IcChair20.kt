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

private var _ic_chair_20: ImageVector? = null

val Icons.ic_chair_20: ImageVector
    get() {
        if (_ic_chair_20 != null) return _ic_chair_20!!
        _ic_chair_20 = ImageVector.Builder(
            name = "ic_chair_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.7784 3C14.3429 3.00028 15.6123 4.26844 15.6124 5.83301V8.68066C15.9804 8.81315 16.3091 9.04649 16.5558 9.3623C16.9533 9.87137 17.0943 10.5355 16.9376 11.1621L16.2296 13.9912C16.1263 14.4041 15.9069 14.7634 15.6114 15.0381V16.25C15.6114 16.6641 15.2755 16.9998 14.8614 17C14.4473 16.9999 14.1114 16.6641 14.1114 16.25V15.6113H5.88878V16.25C5.88878 16.664 5.55269 16.9997 5.13878 17C4.72456 17 4.38878 16.6642 4.38878 16.25V15.0381C4.09347 14.7634 3.8739 14.4041 3.77061 13.9912L3.0626 11.1621C2.90595 10.5355 3.04696 9.87136 3.44444 9.3623C3.69136 9.04609 4.02023 8.81109 4.38878 8.67871V5.83301C4.38893 4.26827 5.65802 3 7.22276 3H12.7784ZM14.9024 10.0557C14.6283 10.0557 14.3889 10.2421 14.3224 10.5078L13.8829 12.2656C13.7993 12.5993 13.4994 12.833 13.1554 12.833H6.84385C6.50002 12.8327 6.19981 12.5992 6.11631 12.2656L5.67784 10.5078C5.61127 10.2421 5.37188 10.0557 5.09776 10.0557C4.91392 10.0557 4.74027 10.1404 4.62706 10.2852C4.5139 10.4301 4.47314 10.6195 4.51768 10.7979L5.22569 13.627C5.29674 13.9111 5.55179 14.1111 5.84483 14.1113H14.1544C14.4477 14.1113 14.7034 13.9113 14.7745 13.627L15.4825 10.7979C15.5271 10.6195 15.4863 10.4301 15.3732 10.2852C15.2599 10.1404 15.0863 10.0557 14.9024 10.0557ZM7.22276 4.5C6.48629 4.5 5.88893 5.09686 5.88878 5.83301V8.71191C6.494 8.95872 6.96689 9.48051 7.13292 10.1445L7.42979 11.333H12.5704L12.8673 10.1445C13.0334 9.48024 13.5069 8.9586 14.1124 8.71191V5.83301C14.1123 5.09703 13.5147 4.50028 12.7784 4.5H7.22276Z"),
            )
        }.build()
        return _ic_chair_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChair20Preview() {
    Icon(
        imageVector = Icons.ic_chair_20,
        contentDescription = null,
    )
}