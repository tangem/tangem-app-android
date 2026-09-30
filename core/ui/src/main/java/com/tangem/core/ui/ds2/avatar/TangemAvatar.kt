@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.avatar

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import arrow.core.Either
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_grid_plus_20

/**
 * Design-system v2 (DS3) **Avatar** — the round (or, for an account, rounded-square) image that leads a
 * row: an account's coloured glyph, a person's initial, an address identicon, a status glyph, or a
 * transaction's icon.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=9737-838)
 *
 * Behavior notes:
 * - [TangemAvatar.UiState.Account] is the only rounded-square variant; every other one is a circle.
 * - [TangemAvatar.UiState.User] with a `null` initial falls back to a neutral person glyph and ignores
 *   its colour, matching the Figma `Fallback` state.
 * - The pending indicator is drawn only for [TangemAvatar.UiState.Transaction] at
 *   [TangemAvatar.Size.X40] and [TangemAvatar.Size.X44] — Figma defines no indicator for the other two
 *   sizes. It overhangs the avatar's corner and punches a transparent ring behind itself.
 * - Fill colours are fixed brand hexes, not theme tokens, so an avatar looks the same in light and dark.
 *
 * @param state What the avatar shows. See [TangemAvatar.UiState].
 * @param size One of the fixed [TangemAvatar.Size] presets driving every dimension.
 * @param modifier Modifier applied to the avatar root. The avatar is fixed-size per [size]; use this to
 * position it, not to resize it.
 * @param contentDescription Accessibility label for the glyph. Supply it when the avatar carries meaning
 * the surrounding row does not already state — a transaction's direction, a status icon.
 */
@Composable
fun TangemAvatar(
    state: TangemAvatar.UiState,
    size: TangemAvatar.Size,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    TangemAvatarContent(
        state = state,
        tokens = size.tokens(),
        modifier = modifier,
        contentDescription = contentDescription,
    )
}

/** Public API surface of [TangemAvatar]. */
object TangemAvatar {

    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /** Fixed size presets: `X72` = 72dp … `X16` = 16dp. */
    enum class Size { X72, X44, X40, X16 }

    /**
     * Fill palette of the [UiState.Account] and [UiState.User] variants, mirroring the Figma
     * `Avatar Module [Fill Color]`. The first twelve are user-selectable; the last four are reserved for
     * the built-in account kinds.
     */
    enum class FillColor {
        DarkBlue,
        LightBlue,
        Lavender,
        Violet,
        Golden,
        LightPurple,
        DarkPurple,
        Pink,
        Red,
        Yellow,
        LightGreen,
        DarkGreen,
        Predictions,
        Gacha,
        PayVisa,
        Money,
    }

    /** Background and glyph colour scheme of the [UiState.Icon] variant. */
    enum class Appearance { Neutral, Success, Error, Warning, Info }

    /** Lifecycle of the transaction an avatar stands for, driving its colour scheme. */
    enum class Status { Progress, Finished, Failed }

    /** What a [TangemAvatar] shows — one subtype per Figma variant. */
    @Immutable
    sealed class UiState {

        /**
         * Account variant — a rounded square filled with [color] and a white glyph.
         *
         * @param icon Account glyph, half the avatar's side.
         * @param color Fill colour. See [FillColor].
         */
        data class Account(
            internal val icon: Either<Int, ImageVector>,
            val color: FillColor,
        ) : UiState() {

            constructor(@DrawableRes iconRes: Int, color: FillColor) : this(Either.Left(iconRes), color)

            constructor(imageVector: ImageVector, color: FillColor) : this(Either.Right(imageVector), color)
        }

        /**
         * User variant — a circle filled with [color] carrying [initial].
         *
         * @param initial Single character, uppercased when drawn. `null` renders the neutral fallback
         * person glyph and ignores [color].
         * @param color Fill colour. See [FillColor].
         */
        data class User(val initial: Char?, val color: FillColor) : UiState()

        /**
         * Address variant — the Blockies identicon generated from [address], clipped to a circle.
         *
         * @param address Raw address used as the identicon seed. Blank renders nothing.
         */
        data class Address(val address: String) : UiState()

        /**
         * Icon variant — a circle with a tinted background and a glyph.
         *
         * @param icon Glyph to draw.
         * @param appearance Colour scheme. See [Appearance].
         */
        data class Icon(
            internal val icon: Either<Int, ImageVector>,
            val appearance: Appearance = Appearance.Neutral,
        ) : UiState() {

            constructor(
                @DrawableRes iconRes: Int,
                appearance: Appearance = Appearance.Neutral,
            ) : this(Either.Left(iconRes), appearance)

            constructor(
                imageVector: ImageVector,
                appearance: Appearance = Appearance.Neutral,
            ) : this(Either.Right(imageVector), appearance)
        }

        /**
         * Transaction variant — the transaction's glyph, coloured by [status].
         *
         * @param icon Glyph describing the transaction kind (send, receive, swap…).
         * @param status Lifecycle driving the colour scheme. See [Status].
         * @param hasPendingIndicator Draws the pending clock badge over the bottom-end corner. Honoured
         * at [Size.X40] and [Size.X44] only.
         */
        data class Transaction(
            internal val icon: Either<Int, ImageVector>,
            val status: Status,
            val hasPendingIndicator: Boolean = false,
        ) : UiState() {

            constructor(
                @DrawableRes iconRes: Int,
                status: Status,
                hasPendingIndicator: Boolean = false,
            ) : this(Either.Left(iconRes), status, hasPendingIndicator)

            constructor(
                imageVector: ImageVector,
                status: Status,
                hasPendingIndicator: Boolean = false,
            ) : this(Either.Right(imageVector), status, hasPendingIndicator)
        }

        /**
         * Loading placeholder.
         *
         * @param shape Whether the placeholder is a circle or the account variant's rounded square.
         */
        data class Shimmer(val shape: Shape = Shape.Circle) : UiState() {

            /** Outline a [Shimmer] takes, so it matches the variant it stands in for. */
            enum class Shape { Circle, RoundedSquare }
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemAvatarPreview() {
    PreviewContainer {
        PreviewRow(
            state = TangemAvatar.UiState.Account(
                iconRes = R.drawable.ic_airplane_mode_24,
                color = TangemAvatar.FillColor.DarkBlue,
            ),
        )
        PreviewRow(state = TangemAvatar.UiState.User(initial = 'A', color = TangemAvatar.FillColor.Violet))
        PreviewRow(state = TangemAvatar.UiState.User(initial = null, color = TangemAvatar.FillColor.Violet))
        PreviewRow(state = TangemAvatar.UiState.Address(address = "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984"))
        PreviewRow(state = TangemAvatar.UiState.Icon(Icons.ic_grid_plus_20, TangemAvatar.Appearance.Neutral))
    }
}

@Preview(name = "States Light", showBackground = true, widthDp = 360)
@Preview(name = "States Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemAvatarStatesPreview() {
    PreviewContainer {
        TangemAvatar.Status.entries.forEach { status ->
            PreviewRow(
                state = TangemAvatar.UiState.Transaction(
                    imageVector = Icons.ic_arrow_up_20,
                    status = status,
                    hasPendingIndicator = true,
                ),
            )
        }
        PreviewRow(state = TangemAvatar.UiState.Shimmer())
        PreviewRow(state = TangemAvatar.UiState.Shimmer(TangemAvatar.UiState.Shimmer.Shape.RoundedSquare))
    }
}

@Composable
private fun PreviewRow(state: TangemAvatar.UiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TangemAvatar.Size.entries.forEach { size ->
            TangemAvatar(state = state, size = size)
        }
    }
}

@Composable
private fun PreviewContainer(content: @Composable () -> Unit) {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

// endregion