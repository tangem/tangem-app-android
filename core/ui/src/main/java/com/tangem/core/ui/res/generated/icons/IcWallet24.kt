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

private var _ic_wallet_24: ImageVector? = null

val Icons.ic_wallet_24: ImageVector
    get() {
        if (_ic_wallet_24 != null) return _ic_wallet_24!!
        _ic_wallet_24 = ImageVector.Builder(
            name = "ic_wallet_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.2246 3.87033C13.41 3.08017 15.0074 3.35376 15.8623 4.49337L17.623 6.84103C19.0662 7.10845 20.159 8.37319 20.1592 9.89376V10.8752C20.6657 11.2036 21.002 11.7735 21.002 12.4221V14.9485C21.0018 15.5968 20.6656 16.166 20.1592 16.4943V17.4748C20.1592 19.1901 18.769 20.5803 17.0537 20.5803H6.10352C4.38838 20.5801 2.99805 19.19 2.99805 17.4748V9.89376C2.99827 8.17879 4.38852 6.78846 6.10352 6.78829H7.84766L12.2246 3.87033ZM6.10352 8.78829C5.49309 8.78846 4.99827 9.28336 4.99805 9.89376V17.4748C4.99805 18.0854 5.49295 18.5801 6.10352 18.5803H17.0537C17.6644 18.5803 18.1592 18.0855 18.1592 17.4748V16.7912H17.0537C15.3387 16.7911 13.9484 15.3998 13.9482 13.6848C13.9484 11.9698 15.3387 10.5795 17.0537 10.5793H18.1592V9.89376C18.159 9.28325 17.6643 8.78829 17.0537 8.78829H6.10352ZM17.0537 12.5793C16.4433 12.5795 15.9484 13.0743 15.9482 13.6848C15.9484 14.2952 16.4433 14.7911 17.0537 14.7912H19.002V12.5793H17.0537ZM14.2617 5.69357C14.0437 5.40313 13.6362 5.33303 13.334 5.53439L11.4531 6.78829H15.083L14.2617 5.69357Z"),
            )
        }.build()
        return _ic_wallet_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWallet24Preview() {
    Icon(
        imageVector = Icons.ic_wallet_24,
        contentDescription = null,
    )
}