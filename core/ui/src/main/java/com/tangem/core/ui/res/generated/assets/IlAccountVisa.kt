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
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.4999 28.9233H13.9037L11.9569 21.1654C11.8645 20.8085 11.6683 20.493 11.3797 20.3443C10.6595 19.9707 9.86581 19.6733 9 19.5233V19.2246H13.1822C13.7594 19.2246 14.1923 19.6733 14.2645 20.1944L15.2746 25.7904L17.8695 19.2246H20.3935L16.4999 28.9233ZM21.8354 28.9233H19.3835L21.4025 19.2246H23.8544L21.8354 28.9233ZM27.028 21.9105C27.1001 21.3881 27.533 21.0895 28.0381 21.0895C28.8317 21.0145 29.6963 21.1645 30.4178 21.5368L30.8507 19.4487C30.1292 19.15 29.3355 19 28.6153 19C26.2356 19 24.5039 20.3434 24.5039 22.2079C24.5039 23.6263 25.7305 24.371 26.5963 24.8197C27.533 25.2671 27.8938 25.5658 27.8216 26.0131C27.8216 26.6842 27.1001 26.9829 26.3799 26.9829C25.5141 26.9829 24.6482 26.7592 23.8558 26.3855L23.4229 28.475C24.2888 28.8474 25.2255 28.9973 26.0913 28.9973C28.7596 29.071 30.4178 27.7289 30.4178 25.7145C30.4178 23.1776 27.028 23.0289 27.028 21.9105ZM39 28.9233L37.0532 19.2246H34.9621C34.5292 19.2246 34.0963 19.5233 33.952 19.9707L30.3469 28.9233H32.8709L33.3747 27.5062H36.476L36.7646 28.9233H39ZM35.3213 21.8358L36.0415 25.491H34.0225L35.3213 21.8358Z"),
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