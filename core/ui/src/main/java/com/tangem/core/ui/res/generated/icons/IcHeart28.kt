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

private var _ic_heart_28: ImageVector? = null

val Icons.ic_heart_28: ImageVector
    get() {
        if (_ic_heart_28 != null) return _ic_heart_28!!
        _ic_heart_28 = ImageVector.Builder(
            name = "ic_heart_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.4189 3.5C23.043 3.5 25.999 7.66221 25.999 11.4092C25.9989 13.379 25.2017 15.181 24.1201 16.7227C23.0366 18.2667 21.615 19.6276 20.2305 20.7432C18.8407 21.8628 17.4505 22.7663 16.3965 23.3896C15.8687 23.7018 15.4192 23.9472 15.0898 24.1172C14.9263 24.2016 14.7858 24.2708 14.6768 24.3213C14.6239 24.3457 14.5671 24.3711 14.5137 24.3926C14.4884 24.4027 14.4483 24.4177 14.4023 24.4326C14.3796 24.44 14.3404 24.4527 14.293 24.4639C14.263 24.4709 14.1481 24.498 14 24.498C13.851 24.498 13.7352 24.4707 13.7061 24.4639C13.6586 24.4527 13.6204 24.44 13.5977 24.4326C13.5514 24.4176 13.5106 24.4027 13.4854 24.3926C13.4319 24.3711 13.3751 24.3457 13.3223 24.3213C13.2131 24.2708 13.0728 24.2016 12.9092 24.1172C12.5799 23.9472 12.1311 23.7017 11.6035 23.3896C10.5494 22.7662 9.15845 21.863 7.76855 20.7432C6.38398 19.6276 4.9634 18.2668 3.87988 16.7227C2.79814 15.181 2.00013 13.3791 2 11.4092C2.00004 7.66235 4.95627 3.50031 9.58008 3.5C11.5194 3.5 12.9731 4.2018 13.999 5.01465C15.0249 4.20158 16.4793 3.50006 18.4189 3.5ZM18.4189 6C16.6992 6.00008 15.5931 6.81735 14.9326 7.55859C14.6955 7.82435 14.3561 7.97646 14 7.97656C13.6436 7.97649 13.3035 7.8246 13.0664 7.55859C12.4059 6.81731 11.3 6 9.58008 6C6.63197 6.00032 4.50004 8.72832 4.5 11.4092C4.50013 12.6933 5.02207 13.9991 5.92578 15.2871C6.82793 16.5728 8.05909 17.7655 9.33789 18.7959C10.6114 19.822 11.8967 20.6581 12.876 21.2373C13.333 21.5076 13.7192 21.7179 14 21.8643C14.2807 21.7179 14.6675 21.5073 15.124 21.2373C16.1032 20.6581 17.3888 19.8218 18.6621 18.7959C19.9407 17.7657 21.1712 16.5726 22.0732 15.2871C22.977 13.9991 23.4989 12.6933 23.499 11.4092C23.499 8.72817 21.3673 6 18.4189 6Z"),
            )
        }.build()
        return _ic_heart_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeart28Preview() {
    Icon(
        imageVector = Icons.ic_heart_28,
        contentDescription = null,
    )
}