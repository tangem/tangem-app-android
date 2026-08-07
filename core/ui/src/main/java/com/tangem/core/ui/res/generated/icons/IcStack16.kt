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

private var _ic_stack_16: ImageVector? = null

val Icons.ic_stack_16: ImageVector
    get() {
        if (_ic_stack_16 != null) return _ic_stack_16!!
        _ic_stack_16 = ImageVector.Builder(
            name = "ic_stack_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.4951 10.0579C12.8103 9.91724 13.1797 10.0591 13.3203 10.3743C13.4607 10.6894 13.3199 11.0588 13.0049 11.1995L8.24902 13.3206C8.08687 13.3929 7.90134 13.393 7.73926 13.3206L2.99512 11.1995C2.68008 11.0586 2.53894 10.6884 2.67969 10.3733C2.82063 10.0584 3.18991 9.91721 3.50488 10.0579C4.4313 10.4721 5.15501 10.7952 5.87793 11.1184C6.53316 11.4114 7.18832 11.7049 7.99512 12.0657C8.80392 11.7048 9.46133 11.4115 10.1182 11.1184C10.8425 10.7953 11.5669 10.472 12.4951 10.0579ZM12.4951 7.42994C12.8102 7.28935 13.1796 7.43039 13.3203 7.74537C13.4607 8.06043 13.3198 8.42984 13.0049 8.57057C12.0764 8.98483 11.3515 9.30883 10.627 9.6321C9.90248 9.95534 9.17745 10.2784 8.24902 10.6926C8.08691 10.7649 7.90129 10.7651 7.73926 10.6926L2.99512 8.57057C2.68012 8.42971 2.53903 8.06041 2.67969 7.74537C2.82058 7.4303 3.1898 7.28912 3.50488 7.42994L7.99512 9.43679C8.80382 9.07596 9.46139 8.78353 10.1182 8.49049C10.8425 8.16732 11.5669 7.84407 12.4951 7.42994ZM7.75195 2.67992C7.91432 2.60743 8.10041 2.60726 8.2627 2.67992L13.0059 4.80395C13.2305 4.9048 13.3751 5.12803 13.375 5.37427C13.3748 5.62062 13.2298 5.8441 13.0049 5.94458C12.0761 6.35936 11.3508 6.68342 10.626 7.00709C9.90141 7.33066 9.17655 7.65396 8.24805 8.06862C8.0857 8.14108 7.89957 8.14126 7.7373 8.06862L2.99414 5.94458C2.76942 5.84381 2.62496 5.61957 2.625 5.37329C2.62529 5.12699 2.77022 4.90343 2.99512 4.80298L7.75195 2.67992ZM4.78027 5.37427L7.99316 6.81373C8.80234 6.45237 9.46007 6.15894 10.1172 5.86548C10.4759 5.70531 10.8344 5.54451 11.2178 5.37329L8.00586 3.93383L4.78027 5.37427Z"),
            )
        }.build()
        return _ic_stack_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcStack16Preview() {
    Icon(
        imageVector = Icons.ic_stack_16,
        contentDescription = null,
    )
}