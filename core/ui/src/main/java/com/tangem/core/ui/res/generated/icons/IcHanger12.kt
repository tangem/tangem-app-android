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

private var _ic_hanger_12: ImageVector? = null

val Icons.ic_hanger_12: ImageVector
    get() {
        if (_ic_hanger_12 != null) return _ic_hanger_12!!
        _ic_hanger_12 = ImageVector.Builder(
            name = "ic_hanger_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6 1.49976C6.76718 1.49976 7.38855 2.12127 7.38867 2.88843C7.38867 3.50653 6.98072 3.83037 6.72949 3.99976C6.63164 4.06551 6.57485 4.11468 6.54199 4.15894C6.53774 4.16467 6.53503 4.17136 6.53125 4.17749C6.88034 4.2152 7.22647 4.28982 7.56348 4.4021L9.45117 5.03101L9.56641 5.07495C10.1288 5.3135 10.4998 5.86664 10.5 6.48511C10.5 7.26374 9.91929 7.90631 9.16699 8.00464V9.08862C9.16699 9.86721 8.53534 10.4994 7.75684 10.4998H4.24512C3.46631 10.4998 2.83398 9.86743 2.83398 9.08862V8.00854C2.08334 7.92176 1.5 7.28531 1.5 6.51147V6.48999C1.50008 5.82702 1.9207 5.23999 2.54785 5.03101L4.43652 4.4021C4.78547 4.28586 5.14419 4.2099 5.50586 4.17358C5.55565 3.62697 5.93116 3.33077 6.1709 3.16968C6.26537 3.10592 6.31844 3.05916 6.34961 3.01733C6.3725 2.98647 6.38867 2.95188 6.38867 2.88843C6.38855 2.67356 6.2149 2.49976 6 2.49976C5.7852 2.49987 5.61144 2.67363 5.61133 2.88843C5.61133 3.16457 5.38747 3.38843 5.11133 3.38843C4.83519 3.38843 4.61133 3.16457 4.61133 2.88843C4.61145 2.12135 5.23292 1.49987 6 1.49976ZM3.83398 9.08862C3.83398 9.31515 4.01859 9.49976 4.24512 9.49976H7.75684C7.98305 9.49939 8.16699 9.31492 8.16699 9.08862V7.38843H3.83398V9.08862ZM7.24707 5.35034C6.43732 5.08063 5.56169 5.08057 4.75195 5.35034L2.86426 5.98022C2.64816 6.05219 2.50008 6.25457 2.5 6.48999V6.51147C2.5 6.7532 2.66931 6.95493 2.89551 7.00659C3.03195 6.64574 3.38051 6.38863 3.78906 6.38843H8.21191C8.61769 6.38863 8.96336 6.6427 9.10156 6.99976C9.33036 6.93967 9.5 6.73306 9.5 6.48511C9.49975 6.25575 9.35317 6.05268 9.13574 5.98022L7.24707 5.35034Z"),
            )
        }.build()
        return _ic_hanger_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHanger12Preview() {
    Icon(
        imageVector = Icons.ic_hanger_12,
        contentDescription = null,
    )
}