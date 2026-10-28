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

private var _ic_mask_32: ImageVector? = null

val Icons.ic_mask_32: ImageVector
    get() {
        if (_ic_mask_32 != null) return _ic_mask_32!!
        _ic_mask_32 = ImageVector.Builder(
            name = "ic_mask_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M26 6.81586L25.9922 6.66547C25.9526 6.31734 25.768 5.99138 25.4678 5.76996C25.1227 5.5156 24.6689 5.4338 24.2481 5.55707L24.2461 5.55804C18.8649 7.12753 13.1352 7.12753 7.75394 5.55804L7.75199 5.55707C7.33116 5.43382 6.8774 5.51562 6.53226 5.76996C6.1892 6.023 5.99743 6.41277 6.00004 6.81586V16.7026C6.00019 22.0889 10.4522 26.5004 16 26.5004C21.5479 26.5004 25.9999 22.0889 26 16.7026V6.81586ZM18.5694 18.4311C19.1743 17.8655 20.1236 17.8976 20.6895 18.5024C21.2551 19.1073 21.223 20.0566 20.6182 20.6225C18.0291 23.0446 13.9709 23.0446 11.3819 20.6225C10.7771 20.0566 10.745 19.1073 11.3106 18.5024C11.8765 17.8976 12.8258 17.8655 13.4307 18.4311C14.8665 19.7745 17.1336 19.7745 18.5694 18.4311ZM13.125 10.9653C13.9534 10.9653 14.6249 11.6369 14.625 12.4653C14.625 13.2937 13.9535 13.9653 13.125 13.9653H10.25C9.42163 13.9652 8.75004 13.2937 8.75004 12.4653C8.75015 11.637 9.4217 10.9653 10.25 10.9653H13.125ZM21.75 10.9653C22.5784 10.9653 23.2499 11.6369 23.25 12.4653C23.25 13.2937 22.5785 13.9653 21.75 13.9653H18.875C18.0466 13.9653 17.375 13.2937 17.375 12.4653C17.3752 11.6369 18.0467 10.9653 18.875 10.9653H21.75ZM29 16.7026C28.9999 23.7956 23.1545 29.5004 16 29.5004C8.84561 29.5004 3.00019 23.7956 3.00004 16.7026V6.82562C2.99421 5.45302 3.64998 4.16785 4.75297 3.35492C5.85605 2.54201 7.27969 2.29372 8.59379 2.67816C13.4265 4.08766 18.5736 4.08668 23.4063 2.67718C24.7204 2.29272 26.144 2.54198 27.2471 3.35492C28.3501 4.16784 29.0059 5.45301 29 6.82562V16.7026Z"),
            )
        }.build()
        return _ic_mask_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMask32Preview() {
    Icon(
        imageVector = Icons.ic_mask_32,
        contentDescription = null,
    )
}