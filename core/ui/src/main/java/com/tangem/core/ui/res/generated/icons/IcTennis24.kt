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

private var _ic_tennis_24: ImageVector? = null

val Icons.ic_tennis_24: ImageVector
    get() {
        if (_ic_tennis_24 != null) return _ic_tennis_24!!
        _ic_tennis_24 = ImageVector.Builder(
            name = "ic_tennis_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.1067 16.2119C20.7054 16.2121 22.0011 17.5087 22.0012 19.1074C22.001 20.7061 20.7054 22.0018 19.1067 22.002C17.5079 22.0018 16.2114 20.7062 16.2112 19.1074C16.2113 17.5087 17.5079 16.212 19.1067 16.2119ZM19.1067 18.2119C18.6125 18.212 18.2113 18.6132 18.2112 19.1074C18.2114 19.6016 18.6125 20.0018 19.1067 20.002C19.6008 20.0018 20.001 19.6016 20.0012 19.1074C20.0011 18.6132 19.6009 18.2121 19.1067 18.2119Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.7502 1.99805C15.6875 1.99805 17.5454 2.76785 18.9153 4.1377C20.2851 5.50754 21.0549 7.36548 21.0549 9.30273C21.0548 13.3367 17.7843 16.6064 13.7502 16.6064H12.511C10.7754 16.6064 9.11032 17.2962 7.88306 18.5234L5.92896 20.4775C5.55954 20.8469 5.05878 21.0547 4.53638 21.0547C4.01397 21.0547 3.51322 20.8469 3.1438 20.4775L2.57544 19.9092C2.20607 19.5398 1.99829 19.039 1.99829 18.5166C1.99829 17.9942 2.20607 17.4934 2.57544 17.124L4.52954 15.1699C5.75679 13.9427 6.44654 12.2776 6.44653 10.542V9.30273C6.44653 7.36561 7.21553 5.50751 8.58521 4.1377C9.95499 2.76792 11.8131 1.99811 13.7502 1.99805ZM6.5354 15.9229C6.43824 16.0426 6.33956 16.1615 6.2356 16.2764L5.9436 16.584L4.01099 18.5166L4.53638 19.042L6.46899 17.1094C6.6792 16.8992 6.89939 16.7012 7.12817 16.5156L6.5354 15.9229ZM7.91235 13.5088C7.82723 13.7385 7.73174 13.964 7.6272 14.1855L8.86548 15.4229C9.08704 15.3183 9.31352 15.2248 9.54321 15.1396L7.91235 13.5088ZM13.7502 3.99805C12.3435 3.99811 10.994 4.55705 9.99927 5.55176C9.00467 6.5465 8.44653 7.89604 8.44653 9.30273V10.542C8.44653 10.7582 8.43542 10.9734 8.41919 11.1875L11.8645 14.6328C12.0789 14.6165 12.2945 14.6064 12.511 14.6064H13.7502C16.6797 14.6064 19.0548 12.2321 19.0549 9.30273C19.0549 7.89592 18.496 6.54653 17.5012 5.55176C16.5065 4.55699 15.1571 3.99805 13.7502 3.99805Z"),
            )
        }.build()
        return _ic_tennis_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTennis24Preview() {
    Icon(
        imageVector = Icons.ic_tennis_24,
        contentDescription = null,
    )
}