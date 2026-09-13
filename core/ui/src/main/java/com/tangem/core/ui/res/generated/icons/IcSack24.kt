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

private var _ic_sack_24: ImageVector? = null

val Icons.ic_sack_24: ImageVector
    get() {
        if (_ic_sack_24 != null) return _ic_sack_24!!
        _ic_sack_24 = ImageVector.Builder(
            name = "ic_sack_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.3587 2C16.0563 2.00012 16.7038 2.36422 17.0667 2.95996C17.4294 3.55573 17.455 4.29718 17.1351 4.91699L16.9456 5.28418C17.5756 5.64928 18.0002 6.32985 18.0003 7.11035V9C18.0003 9.24321 17.9122 9.46532 17.7679 9.63867L19.2405 11.3018C21.135 13.441 21.539 16.5174 20.261 19.0732L20.18 19.2363C19.333 20.9301 17.6011 22 15.7073 22H8.30498C6.41129 21.9999 4.67927 20.93 3.83232 19.2363L3.73955 19.0498C2.46852 16.5076 2.86114 13.4486 4.73271 11.3096L6.21416 9.61621C6.08071 9.44625 6.00029 9.23285 6.00029 9V7.11035C6.0004 6.33016 6.42444 5.65037 7.054 5.28516L6.86455 4.91797C6.54483 4.29821 6.57122 3.55562 6.93389 2.95996C7.29681 2.36414 7.9442 2.00008 8.64189 2H15.3587ZM6.2376 12.627C4.9008 14.1548 4.62077 16.3394 5.52861 18.1553L5.62139 18.3418C6.12956 19.358 7.16885 19.9999 8.30498 20H15.7073C16.8436 20 17.8828 19.358 18.3909 18.3418L18.472 18.1787L18.6292 17.832C19.2969 16.1976 19.0518 14.3285 17.9847 12.9219L17.7435 12.6279L15.4163 10H8.53643L6.2376 12.627ZM8.11064 7C8.04977 7.00016 8.00045 7.04948 8.00029 7.11035V8H16.0003V7.11035C16.0001 7.04957 15.9507 7.00032 15.8899 7H8.11064ZM9.1585 5H14.8421L15.3587 4H8.64189L9.1585 5Z"),
            )
        }.build()
        return _ic_sack_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSack24Preview() {
    Icon(
        imageVector = Icons.ic_sack_24,
        contentDescription = null,
    )
}