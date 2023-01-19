package com.tangem.tap.common.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Created by Anton Zhilenkov on 13/04/2022.
 * Compose views are not typically used as a main or base view.
 */

@Composable
fun TitleSubtitle(
    title: String,
    subtitle: String
) {
    Column {
        Text(text = title)
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}