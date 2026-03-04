package com.tangem.core.ui.ds.image

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Composable function for displaying a wallet icon based on the provided [DeviceIconUM] state.
 *
 * The icon can represent different types of devices, such as cards, rings, stubs, or mobile wallet,
 * with customizable colors and styles.
 *
 * @param state The state of the device icon, which determines its appearance.
 * @param modifier Optional [Modifier] for styling the composable.
 */
@Composable
fun TangemDeviceIcon(state: DeviceIconUM, modifier: Modifier = Modifier) {
    when (state) {
        is DeviceIconUM.Card -> DeviceIcon(
            modifier = modifier,
            isRing = false,
            mainColor = state.mainColor,
            secondColor = state.secondColor,
            thirdColor = state.thirdColor,
            tColor = null,
        )
        is DeviceIconUM.Ring -> DeviceIcon(
            modifier = modifier,
            isRing = true,
            mainColor = state.mainColor,
            secondColor = state.cardColor,
            thirdColor = state.secondCardColor,
            tColor = null,
        )
        is DeviceIconUM.Stub -> DeviceIcon(
            modifier = modifier,
            isRing = false,
            mainColor = Color.Unspecified,
            secondColor = Color.Unspecified.takeIf { state.cardsCount > 1 },
            thirdColor = Color.Unspecified.takeIf { state.cardsCount > 2 },
            tColor = TangemTheme.colors2.graphic.neutral.secondary,
        )
        DeviceIconUM.Mobile -> Icon(
            modifier = modifier,
            imageVector = ImageVector.vectorResource(R.drawable.ic_shield_24),
            contentDescription = null,
            tint = TangemTheme.colors2.graphic.status.attention,
        )
    }
}

@Composable
private fun DeviceIcon(
    isRing: Boolean,
    mainColor: Color,
    secondColor: Color?,
    thirdColor: Color?,
    tColor: Color?,
    modifier: Modifier = Modifier,
) {
    val main = mainColor.takeOrElse { TangemTheme.colors2.graphic.neutral.tertiaryConstant }
    val second = secondColor?.takeOrElse { TangemTheme.colors2.graphic.neutral.tertiaryConstant }
    val third = thirdColor?.takeOrElse { TangemTheme.colors2.graphic.neutral.tertiaryConstant }
    val borderColor = TangemTheme.colors2.border.walletIcon

    val imageVector = remember(isRing, main, second, third, borderColor, tColor) {
        when {
            isRing && second != null && third != null -> WalletIconVectorBuilders.buildRingWithCard2(
                mainColor = main,
                cardColor = second,
                secondCardColor = third,
                borderColor = borderColor,
            )
            !isRing && second != null && third != null -> WalletIconVectorBuilders.buildCard3(
                mainColor = main,
                secondColor = second,
                thirdColor = third,
                tColor = tColor,
                borderColor = borderColor,
            )
            isRing && second != null -> WalletIconVectorBuilders.buildRingWithCard(
                mainColor = main,
                cardColor = second,
                borderColor = borderColor,
            )
            !isRing && second != null -> WalletIconVectorBuilders.buildCard2(
                mainColor = main,
                secondColor = second,
                tColor = tColor,
                borderColor = borderColor,
            )
            isRing -> WalletIconVectorBuilders.buildRing(
                mainColor = main,
                borderColor = borderColor,
            )
            else -> WalletIconVectorBuilders.buildCard(
                mainColor = main,
                borderColor = borderColor,
                tColor = tColor,
            )
        }
    }

    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier,
        tint = Color.Unspecified,
    )
}

// region Preview

private val previewCardBlue
    get() = Color(0xFF1C5FBF)
private val previewCardGold
    get() = Color(0xFFD4A017)
private val previewCardPurple
    get() = Color(0xFF7B2FBE)
private val previewRingGreen
    get() = Color(0xFF2ECC71)

private val previewStates: List<Pair<String, DeviceIconUM>>
    get() = listOf(
        "Card 1" to DeviceIconUM.Card(
            mainColor = previewCardBlue,
            secondColor = null,
        ),
        "Card 2" to DeviceIconUM.Card(
            mainColor = previewCardBlue,
            secondColor = previewCardGold,
        ),
        "Card 3" to DeviceIconUM.Card(
            mainColor = previewCardBlue,
            secondColor = previewCardGold,
            thirdColor = previewCardPurple,
        ),
        "Ring" to DeviceIconUM.Ring(
            mainColor = previewRingGreen,
        ),
        "Ring + Card" to DeviceIconUM.Ring(
            mainColor = previewRingGreen,
            cardColor = previewCardBlue,
        ),
        "Ring + 2 Cards" to DeviceIconUM.Ring(
            mainColor = previewRingGreen,
            cardColor = previewCardBlue,
            secondCardColor = previewCardGold,
        ),
        "Stub 1" to DeviceIconUM.Stub(cardsCount = 1),
        "Stub 2" to DeviceIconUM.Stub(cardsCount = 2),
        "Stub 3" to DeviceIconUM.Stub(cardsCount = 3),
        "Mobile" to DeviceIconUM.Mobile,
    )

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemDeviceIcon_Preview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            previewStates.forEach { (label, state) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TangemDeviceIcon(
                        modifier = Modifier.size(40.dp),
                        state = state,
                    )
                    Text(
                        text = label,
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.primary1,
                    )
                }
            }
        }
    }
}

// endregion