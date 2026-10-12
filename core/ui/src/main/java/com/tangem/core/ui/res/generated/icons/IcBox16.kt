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

private var _ic_box_16: ImageVector? = null

val Icons.ic_box_16: ImageVector
    get() {
        if (_ic_box_16 != null) return _ic_box_16!!
        _ic_box_16 = ImageVector.Builder(
            name = "ic_box_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.88867 9.61428C4.06164 9.31576 4.44453 9.21396 4.74316 9.38675L6.55176 10.4346C6.85029 10.6076 6.9522 10.9895 6.7793 11.2881C6.60633 11.5866 6.22442 11.6884 5.92578 11.5157L4.11621 10.4688C3.8178 10.2958 3.71591 9.91287 3.88867 9.61428Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.09277 1.61819C7.61893 1.31479 8.2593 1.29538 8.7998 1.56057L8.90723 1.61819L13.0723 4.02249L13.1748 4.08596C13.3994 4.23619 13.5821 4.43446 13.7178 4.66018C13.7238 4.66947 13.7317 4.67783 13.7373 4.68753C13.7404 4.69283 13.7422 4.69877 13.7451 4.70413C13.8958 4.97186 13.9794 5.27734 13.9795 5.59475V10.4043C13.9794 11.0123 13.6759 11.5769 13.1748 11.9121L13.0723 11.9756L8.90723 14.3799C8.34579 14.7037 7.65426 14.7045 7.09277 14.3809L7.0918 14.3799L2.92676 11.9756C2.36567 11.6509 2.01953 11.052 2.01953 10.4033V5.59475C2.01968 4.94648 2.36511 4.34662 2.92676 4.02249L7.09277 1.61819ZM3.26953 10.4033C3.26953 10.6053 3.37762 10.7921 3.55273 10.8936L7.375 13.0996V8.36038L3.26953 5.98928V10.4033ZM11.2969 6.81741V8.51077C11.2968 8.8558 11.0169 9.13559 10.6719 9.13577C10.3267 9.13577 10.0469 8.85591 10.0469 8.51077V7.53909L8.625 8.36038V13.0996L12.4473 10.8936C12.6218 10.7929 12.7294 10.6065 12.7295 10.4043V5.99026L11.2969 6.81741ZM3.89355 4.90725L8 7.27737L9.42773 6.45315L5.32227 4.08303L3.89355 4.90725ZM8.28223 2.70022C8.1073 2.59973 7.89159 2.60039 7.7168 2.7012L6.57227 3.36136L10.6777 5.73147L12.1055 4.90725L8.28223 2.70022Z"),
            )
        }.build()
        return _ic_box_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBox16Preview() {
    Icon(
        imageVector = Icons.ic_box_16,
        contentDescription = null,
    )
}