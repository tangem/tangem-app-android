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

private var _ic_cards_star_12: ImageVector? = null

val Icons.ic_cards_star_12: ImageVector
    get() {
        if (_ic_cards_star_12 != null) return _ic_cards_star_12!!
        _ic_cards_star_12 = ImageVector.Builder(
            name = "ic_cards_star_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.58139 3.72192C7.8573 3.72209 8.08125 3.946 8.08139 4.22192C8.08139 4.43629 8.25476 4.61041 8.46908 4.6106C8.74514 4.6106 8.96895 4.83457 8.96908 5.1106C8.96891 5.38659 8.74512 5.6106 8.46908 5.6106C8.28173 5.61076 8.12557 5.74355 8.0892 5.92017L8.08139 5.99829L8.07064 6.09985C8.02385 6.32736 7.8227 6.49814 7.58139 6.49829C7.30524 6.49829 7.08139 6.27443 7.08139 5.99829C7.08123 5.89551 7.03982 5.79657 6.96713 5.72388C6.89432 5.65123 6.79557 5.6106 6.69271 5.6106C6.41668 5.6106 6.19288 5.38659 6.19271 5.1106C6.19283 4.86901 6.36427 4.6669 6.59213 4.62036L6.69271 4.6106L6.77084 4.60278C6.94789 4.56663 7.08139 4.40966 7.08139 4.22192C7.08152 3.94589 7.30532 3.72192 7.58139 3.72192Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.13607 1.50024C9.88885 1.50051 10.5001 2.11073 10.5003 2.86353V7.35767C10.5003 8.11064 9.88898 8.72068 9.13607 8.72095H8.43295C8.40622 8.8207 8.36744 8.91808 8.31478 9.00903C8.15657 9.28211 7.89617 9.48169 7.59115 9.56274L7.59017 9.56177L4.25521 10.4592L4.25424 10.4602C3.61911 10.6298 2.96644 10.2528 2.79428 9.61841L2.7933 9.61743L1.54037 4.94263V4.94067C1.373 4.30644 1.74758 3.65609 2.38021 3.48267L2.38314 3.48169L4.66146 2.87329V2.86353C4.66168 2.1107 5.27192 1.50046 6.02474 1.50024H9.13607ZM2.64389 4.44556L2.64486 4.44653C2.54126 4.47493 2.47973 4.58193 2.50717 4.68579L3.7601 9.35864C3.78509 9.44772 3.86828 9.50543 3.95736 9.50024L3.99642 9.49341L6.86752 8.72095H6.02474C5.27179 8.72073 4.66147 8.11067 4.66146 7.35767V3.90845L2.64389 4.44556ZM6.02474 2.50024C5.8242 2.50046 5.66168 2.66299 5.66146 2.86353V7.35767C5.66147 7.55838 5.82408 7.72073 6.02474 7.72095H9.13607C9.3367 7.72068 9.50033 7.55835 9.50033 7.35767V2.86353C9.50011 2.66302 9.33657 2.50051 9.13607 2.50024H6.02474Z"),
            )
        }.build()
        return _ic_cards_star_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCardsStar12Preview() {
    Icon(
        imageVector = Icons.ic_cards_star_12,
        contentDescription = null,
    )
}