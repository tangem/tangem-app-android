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

private var _ic_coin_token_16: ImageVector? = null

val Icons.ic_coin_token_16: ImageVector
    get() {
        if (_ic_coin_token_16 != null) return _ic_coin_token_16!!
        _ic_coin_token_16 = ImageVector.Builder(
            name = "ic_coin_token_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.94549 5.94531C6.18952 5.70158 6.58528 5.70154 6.82928 5.94531L8.44159 7.55859C8.68548 7.80269 8.6856 8.19837 8.44159 8.44238L6.82928 10.0547C6.58526 10.2986 6.18955 10.2985 5.94549 10.0547L4.33319 8.44238C4.08915 8.19835 4.08923 7.80268 4.33319 7.55859L5.94549 5.94531ZM5.65838 8L6.3869 8.72852L7.11542 8L6.3869 7.27148L5.65838 8Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.61249 3.6123C12.0357 3.61234 14.0002 5.57682 14.0002 8C14.0001 10.4231 12.0356 12.3877 9.61249 12.3877C9.04359 12.3877 8.50286 12.2746 8.00702 12.0781C6.4291 12.7037 4.56192 12.379 3.28534 11.1025C1.57188 9.38908 1.57188 6.61092 3.28534 4.89746C4.56183 3.621 6.42915 3.29548 8.00702 3.9209C8.50277 3.7245 9.04369 3.61233 9.61249 3.6123ZM7.68866 5.14551C6.5317 4.61845 5.12102 4.82938 4.16913 5.78125C2.94382 7.00655 2.94382 8.99345 4.16913 10.2188C5.11771 11.1672 6.52222 11.3804 7.67694 10.8604C7.70638 10.8426 7.73789 10.8291 7.76971 10.8164C8.0717 10.6682 8.35551 10.4699 8.60663 10.2188C9.79353 9.03181 9.83055 7.1301 8.71795 5.89844L8.60663 5.78125L8.48944 5.66992C8.26661 5.46859 8.02137 5.30574 7.76288 5.17969C7.73764 5.16929 7.71239 5.15914 7.68866 5.14551ZM9.45721 4.86621C9.46804 4.87682 9.47967 4.88671 9.49042 4.89746L9.64569 5.06152C11.2017 6.78387 11.1502 9.44268 9.49042 11.1025C9.47983 11.1131 9.46788 11.1224 9.45721 11.1328C9.50865 11.1354 9.56043 11.1377 9.61249 11.1377C11.3453 11.1377 12.7501 9.73277 12.7502 8C12.7502 6.26718 11.3453 4.86234 9.61249 4.8623C9.56044 4.86231 9.50863 4.86362 9.45721 4.86621Z"),
            )
        }.build()
        return _ic_coin_token_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCoinToken16Preview() {
    Icon(
        imageVector = Icons.ic_coin_token_16,
        contentDescription = null,
    )
}