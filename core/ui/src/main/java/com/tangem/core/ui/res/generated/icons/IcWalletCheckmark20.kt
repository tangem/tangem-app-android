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

private var _ic_wallet_checkmark_20: ImageVector? = null

val Icons.ic_wallet_checkmark_20: ImageVector
    get() {
        if (_ic_wallet_checkmark_20 != null) return _ic_wallet_checkmark_20!!
        _ic_wallet_checkmark_20 = ImageVector.Builder(
            name = "ic_wallet_checkmark_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.14062 8.60254C9.43356 8.30985 9.90838 8.30977 10.2012 8.60254C10.4938 8.89545 10.4939 9.37029 10.2012 9.66309L8.46484 11.3975C8.17203 11.6898 7.69807 11.6899 7.40527 11.3975L6.36328 10.3574C6.07046 10.0647 6.07077 9.58987 6.36328 9.29688C6.65601 9.0039 7.1308 9.00325 7.42383 9.2959L7.93457 9.80664L9.14062 8.60254Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.9492 4.64258C15.272 4.64271 16.3447 5.71622 16.3447 7.03906V7.82227C16.7396 8.07174 17.0029 8.51116 17.0029 9.0127V10.9873C17.0028 11.4886 16.7394 11.9273 16.3447 12.1768V12.9619C16.3445 14.2845 15.2719 15.3573 13.9492 15.3574H5.39355C4.07079 15.3574 2.99831 14.2846 2.99805 12.9619V7.03906C2.99805 5.71613 4.07063 4.64258 5.39355 4.64258H13.9492ZM5.39355 6.14355C4.89905 6.14355 4.49805 6.54456 4.49805 7.03906V12.9619C4.49831 13.4562 4.89922 13.8574 5.39355 13.8574H13.9492C14.4434 13.8573 14.8445 13.4561 14.8447 12.9619V12.3955H13.9492C12.6264 12.3955 11.5539 11.3228 11.5537 10C11.5537 8.70321 12.5843 7.6506 13.8711 7.60938C13.8967 7.60673 13.9229 7.60449 13.9492 7.60449H14.8447V7.03906C14.8447 6.54464 14.4436 6.14369 13.9492 6.14355H5.39355ZM13.9492 9.10449C13.4547 9.10449 13.0537 9.5055 13.0537 10C13.0539 10.4943 13.4548 10.8955 13.9492 10.8955H15.5029V9.10449H13.9492Z"),
            )
        }.build()
        return _ic_wallet_checkmark_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletCheckmark20Preview() {
    Icon(
        imageVector = Icons.ic_wallet_checkmark_20,
        contentDescription = null,
    )
}