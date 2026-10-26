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

private var _ic_box_32: ImageVector? = null

val Icons.ic_box_32: ImageVector
    get() {
        if (_ic_box_32 != null) return _ic_box_32!!
        _ic_box_32 = ImageVector.Builder(
            name = "ic_box_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.0723 3.02523C15.1894 2.368 16.5533 2.32702 17.7012 2.90218L17.9277 3.02523L26.0947 7.83577C27.284 8.53553 27.9997 9.8191 28 11.1893V20.8104C27.9999 22.182 27.2832 23.4642 26.0947 24.1639L17.9277 28.9735C16.7361 29.6746 15.2639 29.6756 14.0723 28.9744L5.90527 24.1639C4.71799 23.4633 4 22.1808 4 20.8084V11.1893C4.00026 9.81777 4.71677 8.53541 5.90527 7.83577L9.96973 5.44124C9.97421 5.43852 9.97889 5.4361 9.9834 5.43343L14.0723 3.02523ZM7 20.8084C7 21.1374 7.17156 21.4271 7.42773 21.5789L14.5 25.743V16.8563L7 12.4393V20.8084ZM17.5 16.8563V25.743L24.5723 21.5789L24.6641 21.5164C24.8679 21.3562 24.9999 21.099 25 20.8104V12.4393L17.5 16.8563ZM8.45508 9.81429L15.999 14.2577L18.292 12.9061L10.749 8.4637L8.45508 9.81429ZM16.4053 5.61019C16.153 5.46234 15.8459 5.46206 15.5938 5.61019L15.5947 5.61117L13.7051 6.72347L21.248 11.1659L23.543 9.81429L16.4053 5.61019Z"),
            )
        }.build()
        return _ic_box_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBox32Preview() {
    Icon(
        imageVector = Icons.ic_box_32,
        contentDescription = null,
    )
}