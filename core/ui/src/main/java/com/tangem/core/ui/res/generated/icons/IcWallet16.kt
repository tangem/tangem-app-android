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

private var _ic_wallet_16: ImageVector? = null

val Icons.ic_wallet_16: ImageVector
    get() {
        if (_ic_wallet_16 != null) return _ic_wallet_16!!
        _ic_wallet_16 = ImageVector.Builder(
            name = "ic_wallet_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.08008 3.49016C8.76265 3.0352 9.68145 3.19253 10.1738 3.84855L11.1182 5.10734C11.929 5.2718 12.5391 5.98906 12.5391 6.84855V7.34269C12.8175 7.53932 13 7.86356 13 8.23039V9.61125C13 9.97788 12.8173 10.3013 12.5391 10.498V10.9931C12.539 11.9739 11.7445 12.7692 10.7637 12.7695H4.77637C3.79536 12.7695 3.00004 11.9741 3 10.9931V6.84855C3 5.86752 3.79533 5.07219 4.77637 5.07219H5.70703L8.08008 3.49016ZM4.77637 6.32219C4.48569 6.32219 4.25 6.55787 4.25 6.84855V10.9931C4.25004 11.2837 4.48571 11.5195 4.77637 11.5195H10.7637C11.0541 11.5192 11.289 11.2836 11.2891 10.9931V10.6972H10.7637C9.78267 10.6972 8.98737 9.9018 8.9873 8.92082C8.9873 7.93978 9.78263 7.14445 10.7637 7.14445H11.2891V6.84855C11.2891 6.55802 11.0541 6.32243 10.7637 6.32219H4.77637ZM10.7637 8.39445C10.473 8.39445 10.2373 8.63014 10.2373 8.92082C10.2374 9.21145 10.473 9.44719 10.7637 9.44719H11.75V8.39445H10.7637ZM9.17383 4.59855C9.0795 4.47326 8.90401 4.44324 8.77344 4.53019L7.96094 5.07219H9.5293L9.17383 4.59855Z"),
            )
        }.build()
        return _ic_wallet_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWallet16Preview() {
    Icon(
        imageVector = Icons.ic_wallet_16,
        contentDescription = null,
    )
}