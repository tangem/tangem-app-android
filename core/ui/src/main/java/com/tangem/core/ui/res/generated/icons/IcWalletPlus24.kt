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

private var _ic_wallet_plus_24: ImageVector? = null

val Icons.ic_wallet_plus_24: ImageVector
    get() {
        if (_ic_wallet_plus_24 != null) return _ic_wallet_plus_24!!
        _ic_wallet_plus_24 = ImageVector.Builder(
            name = "ic_wallet_plus_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.4219 4.00146C18.6833 4.08315 20.4981 5.8973 20.5801 8.15869C20.5811 8.18682 20.5796 8.31037 20.5801 8.46631C21.4639 8.70966 22 9.58684 22 10.439V13.562C21.9999 14.4137 21.4641 15.2899 20.5811 15.5337C20.5808 15.684 20.5821 15.8057 20.5811 15.8354C20.4993 18.0969 18.6842 19.9107 16.4229 19.9927C16.3405 19.9957 16.247 19.9956 16.1143 19.9956H12.9482C12.396 19.9956 11.9483 19.5479 11.9482 18.9956C11.9484 18.4434 12.396 17.9956 12.9482 17.9956H16.1143C16.2644 17.9956 16.3133 17.996 16.3506 17.9946C17.5645 17.9506 18.5382 16.9764 18.582 15.7632C18.5818 15.769 18.5827 15.732 18.583 15.6362V15.603H17.7998C15.8675 15.6028 14.5138 13.864 14.5137 12.0005C14.5137 10.1369 15.8674 8.39817 17.7998 8.39795H18.582V8.23096C18.538 7.01779 17.5636 6.04434 16.3496 6.00049C16.3124 5.99916 16.2635 5.99854 16.1143 5.99854H8.30664C7.36166 5.99854 6.7128 5.99946 6.20996 6.04053C5.71877 6.08066 5.45495 6.15453 5.26465 6.25147C4.82904 6.47346 4.47499 6.82778 4.25293 7.26318C4.15602 7.45335 4.08213 7.71755 4.04199 8.2085C4.00095 8.7111 4 9.35976 4 10.3042C4 10.8565 3.55228 11.3042 3 11.3042C2.44779 11.3041 2 10.8564 2 10.3042C2 9.39255 1.99957 8.64836 2.04883 8.04541C2.09908 7.43068 2.20615 6.87408 2.4707 6.35498C2.88463 5.54307 3.54535 4.88293 4.35742 4.46924C4.87647 4.20487 5.43217 4.0976 6.04688 4.04736C6.65 3.9981 7.39459 3.99854 8.30664 3.99854H16.1143C16.2466 3.99853 16.3396 3.99851 16.4219 4.00146ZM17.7998 10.3979C17.2077 10.3982 16.5137 10.99 16.5137 12.0005C16.5138 13.0109 17.2077 13.6028 17.7998 13.603H19.9932C19.9959 13.5918 20 13.5784 20 13.562V10.439C20 10.4226 19.9958 10.4092 19.9932 10.3979H17.7998Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.03613 11.9937C6.58842 11.9937 7.03613 12.4414 7.03613 12.9937V14.9214H8.96289C9.51518 14.9214 9.96289 15.3691 9.96289 15.9214C9.96263 16.4734 9.51501 16.9214 8.96289 16.9214H7.03613V18.8472C7.03604 19.3994 6.58836 19.8472 6.03613 19.8472C5.48414 19.8469 5.03622 19.3992 5.03613 18.8472V16.9214H3.10938C2.55744 16.9212 2.10964 16.4733 2.10938 15.9214C2.10938 15.3692 2.55728 14.9216 3.10938 14.9214H5.03613V12.9937C5.03613 12.4415 5.48408 11.9939 6.03613 11.9937Z"),
            )
        }.build()
        return _ic_wallet_plus_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletPlus24Preview() {
    Icon(
        imageVector = Icons.ic_wallet_plus_24,
        contentDescription = null,
    )
}