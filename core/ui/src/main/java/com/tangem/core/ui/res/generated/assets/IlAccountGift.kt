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

private var _il_account_gift: ImageVector? = null

val Icons.il_account_gift: ImageVector
    get() {
        if (_il_account_gift != null) return _il_account_gift!!
        _il_account_gift = ImageVector.Builder(
            name = "il_account_gift",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.3456 14.8154C25.4668 13.7108 27.2475 13.7278 28.346 14.8705C29.4461 16.0154 29.446 17.8604 28.346 19.0053C28.3051 19.0478 28.2608 19.0871 28.2188 19.1275H32.2051C33.1961 19.1275 34 19.9315 34 20.9225V22.9739C33.9997 23.9647 33.196 24.7688 32.2051 24.7688H31.9487H30.4103H24.7692H23.2308H17.5897H16.0513H15.7949C14.804 24.7688 14.0003 23.9647 14 22.9739V20.9225C14 19.9315 14.8039 19.1275 15.7949 19.1275H19.7813C19.7391 19.0871 19.6949 19.0479 19.654 19.0053C19.635 18.9854 19.6175 18.9645 19.601 18.9432C18.557 17.7947 18.5736 15.9957 19.655 14.8705C20.753 13.7286 22.5313 13.7113 23.6524 14.8144C23.6734 14.832 23.6941 14.8504 23.7135 14.8705C23.8195 14.9808 23.9149 15.099 24 15.2221C24.0851 15.099 24.1805 14.9808 24.2865 14.8705C24.3054 14.8509 24.3252 14.8326 24.3456 14.8154ZM20.7638 15.9373C20.2362 16.4867 20.236 17.3903 20.7638 17.9396C21.431 18.6339 22.429 18.9524 23.2067 19.0694C23.2236 18.8068 23.2369 18.4923 23.2268 18.1589C23.2133 17.7138 23.164 17.2557 23.0565 16.8498C22.9482 16.4416 22.7946 16.1369 22.6088 15.9413C22.6073 15.9398 22.6052 15.9388 22.6038 15.9373C22.0922 15.405 21.2755 15.4052 20.7638 15.9373ZM25.3972 15.9373C25.3956 15.939 25.3936 15.9405 25.3916 15.9419C25.3897 15.9433 25.3878 15.9447 25.3862 15.9463C25.2026 16.1421 25.0508 16.4451 24.9435 16.8498C24.836 17.2557 24.7867 17.7138 24.7732 18.1589C24.7631 18.4923 24.7754 18.8068 24.7923 19.0694C25.5703 18.9525 26.5687 18.6343 27.2362 17.9396C27.7641 17.3902 27.7641 16.4867 27.2362 15.9373C26.7245 15.405 25.9089 15.4051 25.3972 15.9373Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M31.9487 26.3484V32.205C31.9485 33.1959 31.1447 34 30.1538 34H24.7692V32.4615V26.3484H30.4103H31.9487Z"),
            )
            addPath(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.8462 34C16.8553 34 16.0515 33.1959 16.0513 32.205V26.3484H17.5897H23.2308V32.4615V34H17.8462Z"),
            )
        }.build()
        return _il_account_gift!!
    }

@Composable
@Preview(showBackground = true)
private fun IlAccountGiftPreview() {
    Icon(
        imageVector = Icons.il_account_gift,
        contentDescription = null,
    )
}