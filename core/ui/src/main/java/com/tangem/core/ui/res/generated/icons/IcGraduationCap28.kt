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

private var _ic_graduation_cap_28: ImageVector? = null

val Icons.ic_graduation_cap_28: ImageVector
    get() {
        if (_ic_graduation_cap_28 != null) return _ic_graduation_cap_28!!
        _ic_graduation_cap_28 = ImageVector.Builder(
            name = "ic_graduation_cap_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.3751 4.58868C13.1549 3.74796 15.2405 3.80765 16.9767 4.76836L23.3507 8.29961C25.5496 9.51773 25.5497 12.6733 23.3507 13.8914L21.4523 14.9422V18.2449C21.4522 19.64 20.7525 20.9428 19.588 21.7147L19.589 21.7156L17.9298 22.8162C15.5497 24.395 12.4513 24.3944 10.0704 22.8162L8.41125 21.7156C7.24896 20.9444 6.54708 19.6421 6.547 18.2449V14.9422L4.64953 13.8914C2.4505 12.6733 2.45046 9.51767 4.64953 8.29961L11.0236 4.76836L11.3751 4.58868ZM9.047 18.2449C9.04708 18.8013 9.32656 19.322 9.79309 19.6316L11.4523 20.7322C12.9962 21.7557 15.0055 21.7555 16.548 20.7322L18.2072 19.6316C18.6734 19.3227 18.9522 18.8013 18.9523 18.2449V16.3279L16.9767 17.4227C15.2409 18.3832 13.1543 18.4439 11.3751 17.6033L11.0236 17.4227L9.047 16.327V18.2449ZM15.7657 6.95586C14.7358 6.38609 13.4983 6.34981 12.4425 6.84844L12.2345 6.95586L5.86047 10.4861C5.37978 10.7525 5.37983 11.4385 5.86047 11.7049L8.37414 13.0965C8.38837 13.1039 8.40223 13.112 8.41614 13.1199L12.2335 15.2352C13.3307 15.8427 14.6671 15.843 15.7657 15.2352L19.5782 13.1229C19.595 13.1132 19.6118 13.1034 19.629 13.0945L22.1398 11.7049C22.6203 11.4385 22.6203 10.7526 22.1398 10.4861L15.7657 6.95586Z"),
            )
        }.build()
        return _ic_graduation_cap_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGraduationCap28Preview() {
    Icon(
        imageVector = Icons.ic_graduation_cap_28,
        contentDescription = null,
    )
}