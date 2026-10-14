@file:Suppress("all")

package com.tangem.core.ui.res.generated.assets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.generated.icons.Icons

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _il_account_beach: ImageVector? = null

val Icons.il_account_beach: ImageVector
    get() {
        if (_il_account_beach != null) return _il_account_beach!!
        _il_account_beach = ImageVector.Builder(
            name = "il_account_beach",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.7184 2.09396C12.2334 1.94683 12.7701 2.24501 12.9172 2.75997C12.9994 3.04759 13.0629 3.10471 13.3641 3.06955C16.2632 2.73101 19.0232 4.36193 20.3204 7.0373C20.445 7.29396 20.5758 7.5521 20.6511 7.82852C20.743 8.16528 20.761 8.59795 20.4889 9.02002C20.1957 9.4747 19.7514 9.61688 19.4089 9.65595C19.1423 9.68638 18.8261 9.6658 18.5585 9.64838L17.1753 9.5595C17.0399 9.5508 17.0375 9.55149 16.9266 9.62981L15.7231 10.4803C15.6164 10.5556 15.5631 10.5933 15.5424 10.6505C15.5217 10.7078 15.5385 10.7708 15.5721 10.897L16.5896 14.7125C16.6689 15.0097 16.7085 15.1583 16.6267 15.2375C16.5449 15.3167 16.3977 15.2725 16.1031 15.1839L14.8975 14.8215C14.5935 14.7301 14.5846 14.7209 14.5028 14.4142L13.7637 11.6423C13.729 11.5123 13.7117 11.4473 13.6634 11.4077C13.6151 11.368 13.548 11.3637 13.4137 11.3551L11.3715 11.2239C11.236 11.2152 11.2336 11.2159 11.1227 11.2942L9.98699 12.0968C9.75818 12.2588 9.4919 12.4473 9.24407 12.5635C8.93062 12.7105 8.45807 12.8317 7.95645 12.5721C7.49009 12.3308 7.29739 11.9213 7.22022 11.5766C7.15578 11.2887 7.15102 10.9905 7.14096 10.6969C7.04272 7.88463 8.48247 5.19114 10.8855 3.86584C11.1761 3.70558 11.139 3.59587 11.0524 3.29278C10.9053 2.77782 11.2035 2.24109 11.7184 2.09396Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.85865 14.9052C9.25364 14.9052 11.4143 15.5533 13.5477 16.1932C15.6579 16.8261 17.769 17.4576 20.2278 17.5936C20.6972 17.6196 20.9319 17.6326 21.0646 17.7728C21.1973 17.913 21.1973 18.1387 21.1973 18.5902V20.7236C21.1973 21.1807 21.1973 21.4093 21.0553 21.5513C20.9132 21.6933 20.6847 21.6933 20.2275 21.6933H2.77246C2.31533 21.6933 2.08676 21.6933 1.94475 21.5513C1.80273 21.4093 1.80273 21.1807 1.80273 20.7236V16.3279C1.80273 15.9891 1.80273 15.8197 1.90163 15.6896C2.00053 15.5596 2.15459 15.5168 2.46272 15.4313C2.69436 15.3671 2.92835 15.3102 3.16557 15.2606C4.02399 15.0812 5.27077 14.9052 6.85865 14.9052Z"),
            )
        }.build()
        return _il_account_beach!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountBeachPreview() {
    Icon(
        imageVector = Icons.il_account_beach,
        contentDescription = null,
    )
}