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

private var _ic_graduation_cap_16: ImageVector? = null

val Icons.ic_graduation_cap_16: ImageVector
    get() {
        if (_ic_graduation_cap_16 != null) return _ic_graduation_cap_16!!
        _ic_graduation_cap_16 = ImageVector.Builder(
            name = "ic_graduation_cap_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.63676 3.84746C7.48528 3.3842 8.51482 3.38417 9.36332 3.84746L12.2237 5.41093C13.2583 5.97675 13.2584 7.45068 12.2237 8.0164L11.4073 8.46269V9.87968C11.4072 10.4446 11.1564 10.9744 10.7315 11.3348L10.5391 11.4783L9.79496 11.9656C8.70688 12.6778 7.29147 12.6778 6.20317 11.9656L5.45903 11.4783C4.92004 11.1252 4.59092 10.526 4.59086 9.87968V8.46172L3.77641 8.0164C2.74152 7.45067 2.74152 5.97667 3.77641 5.41093L6.63676 3.84746ZM5.84086 9.87968C5.84092 10.0983 5.95201 10.3069 6.1436 10.4324L6.88774 10.9197C7.56045 11.3599 8.43826 11.3597 9.11039 10.9197L9.85453 10.4324C10.046 10.3072 10.1572 10.0984 10.1573 9.87968V9.14629L9.3643 9.57988C8.51583 10.0432 7.48401 10.0433 6.63578 9.57988L5.84086 9.14433V9.87968ZM8.76469 4.94414C8.28946 4.6846 7.71063 4.68463 7.23539 4.94414L4.37602 6.50761C4.20835 6.59927 4.20835 6.82807 4.37602 6.91972L7.23539 8.4832C7.71 8.74248 8.28944 8.74275 8.76469 8.4832L11.6241 6.91972C11.7916 6.82808 11.7915 6.59934 11.6241 6.50761L8.76469 4.94414Z"),
            )
        }.build()
        return _ic_graduation_cap_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGraduationCap16Preview() {
    Icon(
        imageVector = Icons.ic_graduation_cap_16,
        contentDescription = null,
    )
}