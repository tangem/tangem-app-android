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

private var _ic_users_20: ImageVector? = null

val Icons.ic_users_20: ImageVector
    get() {
        if (_ic_users_20 != null) return _ic_users_20!!
        _ic_users_20 = ImageVector.Builder(
            name = "ic_users_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.73926 10.7461C10.4974 10.7461 11.9685 12.1282 11.9688 13.8887V14.5C11.9686 14.914 11.6327 15.2498 11.2188 15.25C10.8046 15.25 10.4689 14.9141 10.4688 14.5V13.8887C10.4685 13.006 9.71928 12.2461 8.73926 12.2461H6.22852C5.24868 12.2463 4.50022 13.0061 4.5 13.8887V14.5C4.49987 14.9141 4.16413 15.25 3.75 15.25C3.33587 15.25 3.00013 14.9141 3 14.5V13.8887C3.00021 12.1283 4.47055 10.7463 6.22852 10.7461H8.73926Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.7715 10.7461C15.5294 10.7463 17.0008 12.1283 17.001 13.8887V14.5C17.0008 14.9138 16.6648 15.2496 16.251 15.25C15.8368 15.25 15.5011 14.9141 15.501 14.5V13.8887C15.5008 13.0062 14.7513 12.2463 13.7715 12.2461H13.0859C12.6718 12.2461 12.3361 11.9102 12.3359 11.4961C12.3359 11.0819 12.6717 10.7461 13.0859 10.7461H13.7715Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.4834 4.75C9.07181 4.75026 10.4052 5.99994 10.4053 7.59668C10.4053 9.19347 9.07184 10.4431 7.4834 10.4434C5.89483 10.4433 4.56152 9.19356 4.56152 7.59668C4.56157 5.99984 5.89486 4.7501 7.4834 4.75ZM7.4834 6.25C6.67298 6.2501 6.06158 6.8777 6.06152 7.59668C6.06152 8.3157 6.67294 8.94326 7.4834 8.94336C8.29371 8.94311 8.90527 8.31561 8.90527 7.59668C8.90522 6.87778 8.29367 6.25025 7.4834 6.25Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.085 5.93945C14.3333 5.93945 15.3916 6.92296 15.3916 8.19141C15.3916 9.45987 14.3333 10.4434 13.085 10.4434C11.8367 10.4432 10.7793 9.45978 10.7793 8.19141C10.7793 6.92305 11.8367 5.93959 13.085 5.93945ZM13.085 7.43945C12.6148 7.43959 12.2793 7.8009 12.2793 8.19141C12.2793 8.58192 12.6148 8.94322 13.085 8.94336C13.5552 8.94336 13.8916 8.58199 13.8916 8.19141C13.8916 7.80083 13.5552 7.43945 13.085 7.43945Z"),
            )
        }.build()
        return _ic_users_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUsers20Preview() {
    Icon(
        imageVector = Icons.ic_users_20,
        contentDescription = null,
    )
}