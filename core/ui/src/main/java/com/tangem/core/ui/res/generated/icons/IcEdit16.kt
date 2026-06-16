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

private var _ic_edit_16: ImageVector? = null

val Icons.ic_edit_16: ImageVector
    get() {
        if (_ic_edit_16 != null) return _ic_edit_16!!
        _ic_edit_16 = ImageVector.Builder(
            name = "ic_edit_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.00195 2.5C8.34699 2.5001 8.62687 2.77996 8.62695 3.125C8.62695 3.47012 8.34704 3.7499 8.00195 3.75H5.83398C4.68324 3.75027 3.75026 4.68324 3.75 5.83398V10.1689C3.75 11.3199 4.68307 12.2536 5.83398 12.2539H10.1689C11.3201 12.2539 12.2539 11.3201 12.2539 10.1689V8.00195C12.254 7.6569 12.5339 7.37704 12.8789 7.37695C13.2239 7.37716 13.5038 7.65697 13.5039 8.00195V10.1689C13.5039 12.0104 12.0104 13.5039 10.1689 13.5039H5.83398C3.99272 13.5036 2.5 12.0103 2.5 10.1689V5.83398C2.50026 3.99288 3.99288 2.50027 5.83398 2.5H8.00195Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.6387 2.5C12.1333 2.50057 12.6076 2.69777 12.957 3.04785L13.0811 3.18457C13.3531 3.51661 13.5038 3.93431 13.5039 4.36719C13.5039 4.86238 13.3062 5.33761 12.9561 5.6875C12.9493 5.69421 12.9406 5.69873 12.9336 5.70508C12.9357 5.70418 12.9385 5.7041 12.9385 5.7041L12.5615 6.08203C12.3193 6.32416 11.9871 6.65558 11.6289 7.01367C10.9124 7.73009 10.0905 8.55311 9.66797 8.97559C9.38516 9.25833 9.01513 9.43691 8.61816 9.48535L8.61914 9.48633L7.75488 9.59277C7.38243 9.63871 7.00893 9.50908 6.74609 9.24121C6.48341 8.97337 6.36014 8.59797 6.41309 8.22656L6.54102 7.33008C6.59502 6.94687 6.77234 6.59117 7.0459 6.31738C7.63731 5.72571 9.08258 4.28042 10.3174 3.0459C10.6675 2.69613 11.1438 2.49962 11.6387 2.5ZM11.6367 3.75C11.4736 3.75001 11.3166 3.81439 11.2012 3.92969C9.9665 5.16407 8.52205 6.60952 7.93066 7.20117C7.84865 7.28325 7.79543 7.38999 7.7793 7.50488V7.50684L7.65918 8.34473L8.46582 8.24609L8.4668 8.24512C8.58663 8.23049 8.6988 8.17617 8.78418 8.09082C9.20679 7.66823 10.0288 6.8461 10.7451 6.12988C11.1033 5.77181 11.4354 5.44044 11.6777 5.19824C11.7989 5.07717 11.8973 4.97768 11.9658 4.90918C12.0001 4.87497 12.0276 4.8483 12.0459 4.83008L12.0732 4.80273C12.1887 4.6872 12.2539 4.53036 12.2539 4.36719C12.2538 4.20384 12.1886 4.04705 12.0732 3.93164L12.0723 3.93066C11.957 3.81521 11.7999 3.75015 11.6367 3.75Z"),
            )
        }.build()
        return _ic_edit_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcEdit16Preview() {
    Icon(
        imageVector = Icons.ic_edit_16,
        contentDescription = null,
    )
}