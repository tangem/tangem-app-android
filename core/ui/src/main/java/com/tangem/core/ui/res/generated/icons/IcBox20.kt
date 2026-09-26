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

private var _ic_box_20: ImageVector? = null

val Icons.ic_box_20: ImageVector
    get() {
        if (_ic_box_20 != null) return _ic_box_20!!
        _ic_box_20 = ImageVector.Builder(
            name = "ic_box_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.8877 12.0337C5.09519 11.6753 5.55464 11.5528 5.91309 11.7602L8.17383 13.0698C8.53224 13.2773 8.65465 13.7358 8.44727 14.0942C8.23976 14.4525 7.78126 14.5749 7.42285 14.3676L5.16113 13.0591C4.80284 12.8515 4.68026 12.3921 4.8877 12.0337Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.88184 2.05124C9.5741 1.65206 10.4269 1.652 11.1191 2.05124L16.3242 5.05612C17.0169 5.4559 17.4443 6.19616 17.4443 6.9946V13.0063C17.4442 13.8057 17.0178 14.5452 16.3252 14.9448L16.3242 14.9438L11.1191 17.9497C10.427 18.3488 9.57403 18.3486 8.88184 17.9497L3.67578 14.9438C2.984 14.5437 2.55583 13.8049 2.55566 13.0053V6.9946C2.55566 6.24499 2.93113 5.54857 3.54883 5.13522L3.67578 5.05612L8.88184 2.05124ZM4.05664 13.0053C4.0568 13.2689 4.19726 13.5127 4.42578 13.645L9.25 16.4301V10.4321L4.05664 7.43405V13.0053ZM14.0898 8.50436V10.6391C14.0898 11.0532 13.7538 11.3889 13.3398 11.3891C12.9257 11.3891 12.5898 11.0533 12.5898 10.6391V9.37057L10.75 10.4331V16.4301L15.5762 13.645L15.6572 13.5903C15.8367 13.4522 15.9442 13.2371 15.9443 13.0063V7.43405L14.0898 8.50436ZM4.80664 6.13522L10 9.13327L11.8467 8.06686L6.6543 5.06882L4.80664 6.13522ZM10.3691 3.35007C10.1694 3.23511 9.92946 3.22087 9.71973 3.3071L9.63184 3.35007L8.15332 4.20261L13.3467 7.20065L15.1934 6.13522L10.3691 3.35007Z"),
            )
        }.build()
        return _ic_box_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBox20Preview() {
    Icon(
        imageVector = Icons.ic_box_20,
        contentDescription = null,
    )
}