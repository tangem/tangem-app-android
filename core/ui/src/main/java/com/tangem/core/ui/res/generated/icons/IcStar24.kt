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

private var _ic_star_24: ImageVector? = null

val Icons.ic_star_24: ImageVector
    get() {
        if (_ic_star_24 != null) return _ic_star_24!!
        _ic_star_24 = ImageVector.Builder(
            name = "ic_star_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.0001 2.47266C12.9184 2.47277 13.7578 2.9936 14.167 3.81543L15.9542 7.38086L19.9131 7.94727C20.8323 8.07624 21.5976 8.71996 21.8819 9.60352C22.1657 10.4866 21.9198 11.4541 21.2491 12.0947L21.2501 12.0957L18.4083 14.8174L19.0821 18.6836C19.243 19.5918 18.8748 20.5131 18.1309 21.0586C17.387 21.6041 16.397 21.6796 15.5792 21.2529L12.0001 19.4023L8.41998 21.251L8.42096 21.252C7.6032 21.6786 6.61313 21.6029 5.8692 21.0576C5.12628 20.5127 4.7574 19.5928 4.91705 18.6855L5.59185 14.8174L2.75006 12.0957L2.75103 12.0947C2.08051 11.454 1.83434 10.4865 2.11822 9.60352C2.38477 8.77507 3.07356 8.15769 3.91607 7.97754L4.08697 7.94727L8.04498 7.38086L9.83307 3.81543C10.2423 2.99346 11.0816 2.47275 12.0001 2.47266ZM12.0001 4.47363C11.8402 4.47373 11.6941 4.56479 11.6231 4.70801L11.6212 4.71191L9.59869 8.74609C9.45179 9.03864 9.17079 9.24162 8.84674 9.28809L4.36724 9.92773L4.36432 9.92871C4.20471 9.95116 4.07202 10.0624 4.02252 10.2158C3.97944 10.3501 4.00618 10.4966 4.09185 10.6055L4.13287 10.6494L4.13385 10.6514L7.36041 13.7432C7.60056 13.9737 7.71058 14.3088 7.65338 14.6367L6.88775 19.0293L6.88678 19.0322C6.85905 19.1899 6.92271 19.3506 7.05182 19.4453C7.18112 19.5399 7.35412 19.5527 7.49615 19.4785L7.50006 19.4766L11.5411 17.3887C11.8289 17.24 12.1712 17.2401 12.459 17.3887L16.501 19.4775L16.5049 19.4795C16.647 19.5534 16.8191 19.54 16.9483 19.4453C17.0773 19.3506 17.141 19.1908 17.1133 19.0332V19.0303L16.3467 14.6367C16.2895 14.3086 16.4002 13.9736 16.6407 13.7432L19.8663 10.6514L19.8682 10.6494C19.9848 10.5382 20.0277 10.3693 19.9786 10.2158C19.9291 10.0624 19.7954 9.95122 19.6358 9.92871L19.6338 9.92773L15.1544 9.28809C14.8301 9.24174 14.5484 9.03874 14.4014 8.74609L12.379 4.71191L12.377 4.70801C12.306 4.56481 12.1599 4.47375 12.0001 4.47363Z"),
            )
        }.build()
        return _ic_star_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcStar24Preview() {
    Icon(
        imageVector = Icons.ic_star_24,
        contentDescription = null,
    )
}