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

private var _ic_star_20: ImageVector? = null

val Icons.ic_star_20: ImageVector
    get() {
        if (_ic_star_20 != null) return _ic_star_20!!
        _ic_star_20 = ImageVector.Builder(
            name = "ic_star_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.0001 2.37305C10.7193 2.3731 11.3762 2.78133 11.6964 3.4248L13.1486 6.32617L16.3702 6.78809C17.0898 6.88943 17.687 7.39336 17.9093 8.08496C18.1313 8.77628 17.9381 9.53342 17.4132 10.0352L17.4142 10.0361L15.1036 12.252L15.6525 15.4014C15.7776 16.1118 15.4891 16.8328 14.9073 17.2598C14.3258 17.6865 13.552 17.745 12.9122 17.4121L9.99915 15.9062L7.08801 17.4121L7.08704 17.4111C6.44739 17.7437 5.6743 17.6853 5.0929 17.2588C4.51147 16.832 4.22302 16.1114 4.34778 15.4014L4.89563 12.252L2.58606 10.0361L2.58704 10.0352C2.06209 9.53341 1.86894 8.7763 2.09094 8.08496C2.31313 7.39361 2.91084 6.88968 3.63001 6.78809L6.85071 6.32617L8.30383 3.4248C8.62404 2.78129 9.28092 2.37305 10.0001 2.37305ZM10.0001 3.87402C9.85049 3.87402 9.71341 3.95892 9.64661 4.09375L9.64465 4.09668L8.01575 7.35059C7.90556 7.57006 7.69442 7.72203 7.45129 7.75684L3.84192 8.27344H3.83997C3.69052 8.2945 3.56618 8.39938 3.51965 8.54395C3.47912 8.67053 3.5046 8.8078 3.58508 8.91016L3.62317 8.95215L3.62415 8.95312L6.22376 11.4463C6.40359 11.619 6.48618 11.8705 6.44348 12.1162L5.82532 15.6582V15.6611C5.79927 15.8098 5.85944 15.9609 5.98059 16.0498C6.1018 16.1385 6.26356 16.1505 6.39661 16.0811L6.39954 16.0791L9.6554 14.3965L9.7384 14.3594C9.93529 14.286 10.1558 14.2987 10.3448 14.3965L13.6007 16.0801L13.6036 16.082C13.7367 16.1514 13.8985 16.1387 14.0197 16.0498C14.1407 15.9608 14.2011 15.8097 14.1749 15.6611V15.6592L13.5568 12.1162C13.5141 11.8705 13.5966 11.619 13.7765 11.4463L16.3761 8.95312L16.3771 8.95215C16.4866 8.84757 16.5269 8.68861 16.4806 8.54395C16.4341 8.39944 16.3096 8.29456 16.1603 8.27344H16.1583L12.549 7.75684C12.3058 7.72205 12.0947 7.57002 11.9845 7.35059L10.3556 4.09668L10.3536 4.09375C10.2869 3.95896 10.1497 3.87407 10.0001 3.87402Z"),
            )
        }.build()
        return _ic_star_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcStar20Preview() {
    Icon(
        imageVector = Icons.ic_star_20,
        contentDescription = null,
    )
}