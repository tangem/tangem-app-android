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

private var _ic_book_opened_12: ImageVector? = null

val Icons.ic_book_opened_12: ImageVector
    get() {
        if (_ic_book_opened_12 != null) return _ic_book_opened_12!!
        _ic_book_opened_12 = ImageVector.Builder(
            name = "ic_book_opened_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.22168 1.49976C4.83511 1.49976 5.41726 1.75824 5.8418 2.20776C5.89816 2.26746 5.95046 2.33043 6 2.39526C6.41292 1.8536 7.0483 1.49976 7.77832 1.49976H9.33398C9.65281 1.49979 9.95276 1.63408 10.1689 1.86304C10.3841 2.09103 10.5 2.3951 10.5 2.70581V8.35327C10.4999 8.99049 10.0063 9.55654 9.33691 9.55835H7.43945C7.0353 9.55792 6.64788 9.80084 6.45312 10.2136C6.35316 10.425 6.11981 10.5392 5.8916 10.488C5.73685 10.4534 5.61392 10.3482 5.5498 10.2117L5.54688 10.2136C5.3521 9.8008 4.96474 9.55788 4.56055 9.55835H2.66602C2.34728 9.55828 2.04818 9.42393 1.83203 9.19507C1.61679 8.96711 1.50005 8.66398 1.5 8.35327V2.70581C1.5 2.39503 1.61674 2.09105 1.83203 1.86304C2.04817 1.63428 2.34737 1.49982 2.66602 1.49976H4.22168ZM2.66602 2.49976C2.63155 2.49982 2.59219 2.51399 2.55859 2.54956C2.52399 2.58621 2.5 2.64231 2.5 2.70581V8.35327C2.50005 8.4166 2.52411 8.47194 2.55859 8.50854C2.5922 8.54414 2.63154 8.55828 2.66602 8.55835H4.55957C4.89438 8.55799 5.21385 8.64152 5.49902 8.78979V3.88159C5.49893 3.50622 5.35806 3.1515 5.11523 2.89429C4.8732 2.63796 4.55085 2.49976 4.22168 2.49976H2.66602ZM7.77832 2.49976C7.0997 2.49976 6.50117 3.09098 6.50098 3.88159V8.78979C6.78614 8.6415 7.10562 8.55801 7.44043 8.55835H9.33398C9.3991 8.55818 9.4999 8.49385 9.5 8.35327V2.70581C9.5 2.64231 9.47601 2.58621 9.44141 2.54956C9.40778 2.51399 9.36846 2.49979 9.33398 2.49976H7.77832Z"),
            )
        }.build()
        return _ic_book_opened_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBookOpened12Preview() {
    Icon(
        imageVector = Icons.ic_book_opened_12,
        contentDescription = null,
    )
}