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

private var _ic_loading_spinner_28: ImageVector? = null

val Icons.ic_loading_spinner_28: ImageVector
    get() {
        if (_ic_loading_spinner_28 != null) return _ic_loading_spinner_28!!
        _ic_loading_spinner_28 = ImageVector.Builder(
            name = "ic_loading_spinner_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2C16.3734 2 18.6936 2.70388 20.667 4.02246C22.6403 5.34103 24.1787 7.21554 25.0869 9.4082C25.995 11.6007 26.2324 14.0133 25.7695 16.3408C25.3065 18.6686 24.1636 20.8071 22.4854 22.4854C20.8071 24.1636 18.6686 25.3065 16.3408 25.7695C14.0133 26.2324 11.6007 25.995 9.4082 25.0869C7.21555 24.1787 5.34103 22.6403 4.02246 20.667C2.70389 18.6936 2 16.3734 2 14C2 13.3096 2.55965 12.75 3.25 12.75C3.94036 12.75 4.5 13.3096 4.5 14C4.5 15.8789 5.05672 17.7161 6.10059 19.2783C7.14438 20.8404 8.62855 22.0573 10.3643 22.7764C12.1002 23.4954 14.0107 23.6839 15.8535 23.3174C17.6963 22.9508 19.3892 22.0463 20.7178 20.7178C22.0463 19.3892 22.9508 17.6963 23.3174 15.8535C23.6839 14.0107 23.4964 12.1002 22.7773 10.3643C22.0583 8.6284 20.8405 7.14445 19.2783 6.10059C17.7161 5.05671 15.8789 4.5 14 4.5C13.3096 4.5 12.75 3.94036 12.75 3.25C12.75 2.55964 13.3096 2 14 2Z"),
            )
        }.build()
        return _ic_loading_spinner_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLoadingSpinner28Preview() {
    Icon(
        imageVector = Icons.ic_loading_spinner_28,
        contentDescription = null,
    )
}