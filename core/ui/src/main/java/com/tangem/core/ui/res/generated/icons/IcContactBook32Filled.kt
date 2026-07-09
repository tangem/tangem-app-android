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

private var _ic_contact_book_32_filled: ImageVector? = null

val Icons.ic_contact_book_32_filled: ImageVector
    get() {
        if (_ic_contact_book_32_filled != null) return _ic_contact_book_32_filled!!
        _ic_contact_book_32_filled = ImageVector.Builder(
            name = "ic_contact_book_32_filled",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23.8203 3.00098C25.573 3.00111 26.9701 4.44317 26.9707 6.19434L27 25.7959V25.7979C26.9995 27.4941 25.6888 28.9007 24.0137 28.9873L23.8496 28.9922H8.17969C6.42723 28.9922 5.03037 27.5496 5.0293 25.7988L5.02637 24.042H3.375C2.6158 24.0418 2 23.4262 2 22.667C2.0003 21.908 2.61599 21.2922 3.375 21.292H5.02246L5.0166 17.4961H3.375C2.61585 17.4959 2.00008 16.8803 2 16.1211C2.00015 15.362 2.61589 14.7463 3.375 14.7461H5.0127L5.00684 10.9502H3.375C2.6158 10.95 2 10.3344 2 9.5752C2.00015 8.81607 2.61589 8.20042 3.375 8.2002H5.00293L5 6.19629C5.00006 4.4446 6.3973 3.00098 8.15039 3.00098H23.8203ZM14.3174 17.1738C12.671 17.1788 11.3273 18.4709 11.2402 20.0967L11.2354 20.2559V20.5908C11.2354 21.0637 11.4236 21.5181 11.7578 21.8525C12.0921 22.1867 12.5459 22.3748 13.0186 22.375H18.9805C19.9654 22.3749 20.7637 21.5757 20.7637 20.5908V20.2559C20.7589 18.6093 19.4668 17.2657 17.8408 17.1787L17.6816 17.1738H14.3174ZM16 9.625C14.3383 9.62502 12.9912 10.9721 12.9912 12.6338C12.9912 14.2955 14.3383 15.6426 16 15.6426C17.6617 15.6426 19.0088 14.2955 19.0088 12.6338C19.0088 10.9721 17.6617 9.625 16 9.625Z"),
            )
        }.build()
        return _ic_contact_book_32_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcContactBook32FilledPreview() {
    Icon(
        imageVector = Icons.ic_contact_book_32_filled,
        contentDescription = null,
    )
}