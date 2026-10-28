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

private var _ic_stack_32: ImageVector? = null

val Icons.ic_stack_32: ImageVector
    get() {
        if (_ic_stack_32 != null) return _ic_stack_32!!
        _ic_stack_32 = ImageVector.Builder(
            name = "ic_stack_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M25.888 20.4405C26.6446 20.1029 27.5319 20.4427 27.8695 21.1993C28.2067 21.9557 27.8671 22.8422 27.1107 23.1797C25.0582 24.0955 23.4564 24.8099 21.8548 25.5245C20.2533 26.239 18.6522 26.9545 16.5999 27.8702C16.2107 28.0439 15.7655 28.0431 15.3763 27.8692L4.88803 23.1797C4.13178 22.8416 3.79301 21.9536 4.1312 21.1973C4.46949 20.4414 5.35653 20.1024 6.11264 20.4405C8.16051 21.3562 9.75877 22.0706 11.3568 22.7852C12.7921 23.427 14.2274 24.0689 15.9886 24.8565C17.7545 24.0686 19.1942 23.4272 20.6332 22.7852C22.2345 22.0707 23.8359 21.3561 25.888 20.4405ZM25.888 14.6299C26.6446 14.2924 27.5319 14.6322 27.8695 15.3887C28.207 16.1453 27.8672 17.0326 27.1107 17.3702C25.0583 18.2859 23.4564 19.0003 21.8548 19.7149C20.2534 20.4294 18.6521 21.144 16.5999 22.0596C16.2107 22.2333 15.7655 22.2335 15.3763 22.0596L4.88803 17.3692C4.1318 17.031 3.79302 16.144 4.1312 15.3877C4.46939 14.6315 5.3564 14.2927 6.11264 14.6309L15.9896 19.0469C17.7552 18.2591 19.1944 17.6176 20.6332 16.9756C22.2346 16.2611 23.8357 15.5456 25.888 14.6299ZM15.5521 4.07329C15.9037 3.95916 16.2874 3.9783 16.6283 4.13091L27.1126 8.82524C27.6525 9.06698 27.9996 9.60385 27.9994 10.1954C27.9991 10.7868 27.6516 11.3232 27.1117 11.5645L16.596 16.2598C16.2063 16.4338 15.7609 16.4343 15.3714 16.2598L4.88706 11.5635C4.34729 11.3217 3.99999 10.7849 4.00034 10.1934C4.00085 9.60203 4.34899 9.06635 4.88901 8.82524L15.4037 4.12993L15.5521 4.07329ZM9.17417 10.1963L15.9847 13.2471C17.7512 12.4582 19.1908 11.8157 20.6302 11.1729C21.3481 10.8523 22.0661 10.5312 22.8246 10.1924L16.014 7.14263L9.17417 10.1963Z"),
            )
        }.build()
        return _ic_stack_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcStack32Preview() {
    Icon(
        imageVector = Icons.ic_stack_32,
        contentDescription = null,
    )
}