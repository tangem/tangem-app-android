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

private var _ic_box_28: ImageVector? = null

val Icons.ic_box_28: ImageVector
    get() {
        if (_ic_box_28 != null) return _ic_box_28!!
        _ic_box_28 = ImageVector.Builder(
            name = "ic_box_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.66992 16.7475C7.01571 16.1503 7.78056 15.946 8.37793 16.2915L11.5439 18.1245C12.1413 18.4704 12.3449 19.2351 11.999 19.8325C11.6531 20.4298 10.8884 20.6334 10.291 20.2876L7.12598 18.4555C6.52882 18.1097 6.32441 17.3449 6.66992 16.7475Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.335 2.69773C13.3659 2.10322 14.635 2.10318 15.666 2.69773L22.9561 6.90574C23.988 7.50115 24.622 8.60229 24.6221 9.79148V18.2085C24.6219 19.3991 23.9868 20.5001 22.9551 21.0952L22.9541 21.0942L15.667 25.3022C14.6361 25.8967 13.3659 25.8965 12.335 25.3022L5.04492 21.0942C4.01469 20.4983 3.37814 19.3983 3.37793 18.2075V9.79148C3.37798 8.60098 4.01343 7.501 5.04492 6.90574L8.64648 4.82566C8.67183 4.8097 8.69828 4.79576 8.72461 4.78171L12.335 2.69773ZM19.9258 12.0229V14.895C19.9256 15.585 19.3657 16.1447 18.6758 16.145C17.9855 16.145 17.426 15.5852 17.4258 14.895V13.4653L15.25 14.7221V22.6557L21.7061 18.9292L21.7979 18.8676C22.0003 18.7118 22.1219 18.4692 22.1221 18.2085V10.7544L19.9258 12.0229ZM5.87891 18.2075C5.87911 18.5048 6.03717 18.7796 6.29492 18.9292L12.75 22.6548V14.7202L5.87891 10.7544V18.2075ZM7.12891 8.58835L14.001 12.5561L16.1865 11.2934L9.31445 7.32663L7.12891 8.58835ZM14.417 4.86277C14.1913 4.73273 13.9197 4.71631 13.6826 4.81394L13.584 4.86277L11.8154 5.88327L18.6865 9.85007L20.8711 8.58835L14.417 4.86277Z"),
            )
        }.build()
        return _ic_box_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBox28Preview() {
    Icon(
        imageVector = Icons.ic_box_28,
        contentDescription = null,
    )
}