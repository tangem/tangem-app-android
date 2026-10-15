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

private var _ic_cards_star_32: ImageVector? = null

val Icons.ic_cards_star_32: ImageVector
    get() {
        if (_ic_cards_star_32 != null) return _ic_cards_star_32!!
        _ic_cards_star_32 = ImageVector.Builder(
            name = "ic_cards_star_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.1501 9.83301C20.9783 9.83327 21.6501 10.5047 21.6501 11.333C21.6501 11.7924 22.0226 12.165 22.4822 12.165C23.3104 12.1653 23.9822 12.8368 23.9822 13.665C23.9821 14.4933 23.3103 15.1648 22.4822 15.165C22.0512 15.1651 21.6965 15.4925 21.654 15.9121L21.6501 15.9971L21.6423 16.1504C21.5654 16.9065 20.9265 17.4968 20.1501 17.4971C19.3218 17.4971 18.6503 16.8254 18.6501 15.9971C18.6501 15.7766 18.5628 15.5642 18.407 15.4082C18.2511 15.2523 18.0386 15.1652 17.8181 15.165C16.9897 15.165 16.3182 14.4934 16.3181 13.665C16.3181 12.8884 16.9084 12.2497 17.6648 12.1729L17.8181 12.165L17.9031 12.1602C18.3225 12.1174 18.6501 11.7635 18.6501 11.333C18.6501 10.5046 19.3217 9.83302 20.1501 9.83301Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.196 4C26.2963 4 27.9995 5.7025 27.9998 7.80273V19.5273C27.9996 21.6276 26.2964 23.3301 24.196 23.3301H22.53C22.4578 23.5586 22.361 23.7806 22.239 23.9912C21.7987 24.7515 21.0735 25.3057 20.2244 25.5312L20.2234 25.5303L11.4685 27.8857L11.4665 27.8867C9.69901 28.359 7.88173 27.3128 7.40307 25.5469L7.40209 25.543L4.11303 13.2715L4.11108 13.2666C3.64497 11.5021 4.68806 9.69158 6.44799 9.20898L6.45873 9.20605L12.3005 7.64746C12.3822 5.61936 14.0522 4.00022 16.1003 4H24.196ZM7.23998 12.1016L7.24096 12.1025C7.06846 12.1501 6.96684 12.3271 7.01245 12.5L10.2996 24.7617L10.322 24.8223C10.3793 24.9364 10.5001 25.0065 10.6277 24.999L10.6921 24.9883L16.8572 23.3301H16.1003C14.0002 23.3299 12.2977 21.6275 12.2976 19.5273V10.7529L7.23998 12.1016ZM16.1003 7C15.6571 7.00022 15.2978 7.3595 15.2976 7.80273V19.5273C15.2977 19.9707 15.657 20.3299 16.1003 20.3301H24.196C24.6395 20.3301 24.9996 19.9708 24.9998 19.5273V7.80273C24.9995 7.35936 24.6395 7 24.196 7H16.1003Z"),
            )
        }.build()
        return _ic_cards_star_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCardsStar32Preview() {
    Icon(
        imageVector = Icons.ic_cards_star_32,
        contentDescription = null,
    )
}