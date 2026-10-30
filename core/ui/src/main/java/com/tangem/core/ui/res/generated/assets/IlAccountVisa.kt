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

private var _il_account_visa: ImageVector? = null

val Icons.il_account_visa: ImageVector
    get() {
        if (_il_account_visa != null) return _il_account_visa!!
        _il_account_visa = ImageVector.Builder(
            name = "il_account_visa",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M6.00007 15.9384H3.92318L2.36576 9.73226C2.29184 9.44678 2.13488 9.1944 1.90401 9.07544C1.32782 8.77651 0.692909 8.53861 0.000274658 8.41863V8.17969H3.34598C3.80774 8.17969 4.15406 8.53861 4.21177 8.95546L5.01985 13.4322L7.09573 8.17969H9.1149L6.00007 15.9384ZM10.2684 15.9384H8.30692L9.92205 8.17969H11.8835L10.2684 15.9384ZM14.4223 10.3283C14.48 9.91047 14.8264 9.67153 15.2304 9.67153C15.8653 9.61154 16.5569 9.73152 17.1341 10.0294L17.4804 8.35892C16.9033 8.11999 16.2683 8 15.6921 8C13.7884 8 12.4031 9.0747 12.4031 10.5663C12.4031 11.7009 13.3844 12.2967 14.077 12.6557C14.8264 13.0136 15.115 13.2525 15.0572 13.6104C15.0572 14.1472 14.48 14.3862 13.9039 14.3862C13.2112 14.3862 12.5186 14.2072 11.8847 13.9083L11.5384 15.5798C12.231 15.8777 12.9803 15.9977 13.673 15.9977C15.8076 16.0566 17.1341 14.983 17.1341 13.3714C17.1341 11.342 14.4223 11.2231 14.4223 10.3283V10.3283ZM23.9997 15.9384L22.4423 8.17969H20.7695C20.4231 8.17969 20.0768 8.41863 19.9614 8.77651L17.0774 15.9384H19.0966L19.4996 14.8048H21.9806L22.2114 15.9384H23.9997ZM21.0568 10.2686L21.633 13.1927H20.0179L21.0568 10.2686Z"),
            )
        }.build()
        return _il_account_visa!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountVisaPreview() {
    Icon(
        imageVector = Icons.il_account_visa,
        contentDescription = null,
    )
}