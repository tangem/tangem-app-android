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

private var _ic_mask_16: ImageVector? = null

val Icons.ic_mask_16: ImageVector
    get() {
        if (_ic_mask_16 != null) return _ic_mask_16!!
        _ic_mask_16 = ImageVector.Builder(
            name = "ic_mask_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.25 3.88688C12.2513 3.67719 12.1564 3.48692 12.0069 3.37028C11.8589 3.25509 11.6722 3.22186 11.502 3.27458C9.21735 3.97857 6.78367 3.97857 4.49904 3.27458C4.32885 3.22186 4.14111 3.25509 3.99318 3.37028C3.84365 3.48692 3.74873 3.67718 3.75002 3.88688V8.31462C3.75005 10.7865 5.67467 12.7502 8.00002 12.7502C10.3254 12.7502 12.25 10.7865 12.25 8.31462V3.88688ZM9.084 9.13493C9.32939 8.89248 9.72513 8.89467 9.96779 9.13981C10.2105 9.38528 10.2084 9.7819 9.96291 10.0246C8.86636 11.1085 7.13368 11.1085 6.03713 10.0246C5.79166 9.7819 5.78956 9.38528 6.03224 9.13981C6.27491 8.89468 6.67066 8.89247 6.91603 9.13493C7.52566 9.73764 8.47437 9.73764 9.084 9.13493ZM6.78127 5.79216C7.12644 5.79216 7.40627 6.07198 7.40627 6.41716C7.40608 6.76217 7.12632 7.04215 6.78127 7.04216H5.56252C5.21746 7.04216 4.93771 6.76217 4.93752 6.41716C4.93752 6.07198 5.21734 5.79216 5.56252 5.79216H6.78127ZM10.4375 5.79216C10.7827 5.79218 11.0625 6.07199 11.0625 6.41716C11.0623 6.76216 10.7826 7.04213 10.4375 7.04216H9.21877C8.87371 7.04216 8.59396 6.76217 8.59377 6.41716C8.59377 6.07198 8.87359 5.79216 9.21877 5.79216H10.4375ZM13.5 8.31462C13.5 11.4324 11.0593 14.0002 8.00002 14.0002C4.94069 14.0002 2.50005 11.4324 2.50002 8.31462V3.89079C2.49751 3.30492 2.76211 2.74492 3.2256 2.38395C3.6918 2.02094 4.30174 1.90523 4.86721 2.08024L4.86623 2.08122C6.91097 2.71136 9.08806 2.71122 11.1328 2.08122C11.6983 1.90622 12.3082 2.02094 12.7744 2.38395C13.2379 2.74492 13.5025 3.30494 13.5 3.89079V8.31462Z"),
            )
        }.build()
        return _ic_mask_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcMask16Preview() {
    Icon(
        imageVector = Icons.ic_mask_16,
        contentDescription = null,
    )
}