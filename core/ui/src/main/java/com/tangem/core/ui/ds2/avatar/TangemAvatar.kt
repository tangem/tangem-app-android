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
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.assets.il_account_airplane
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_grid_plus_20

/**
 * Design-system v2 (DS3) **Avatar** — the round or rounded-square image that leads a row: an account's
 * coloured glyph or letter, a person's initial, an address identicon, a status glyph, a transaction's icon,
 * or an arbitrary image asset.
 *
 * Version: 1.3.1
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=9737-838)
 *
 * Behavior notes:
 * - Every variant takes a [TangemAvatar.Shape]; [TangemAvatar.UiState.Account] defaults to a rounded
 *   square, every other one to a circle.
 * - [TangemAvatar.UiState.User] with a `null` initial falls back to a neutral person glyph and ignores
 *   its colour, matching the Figma `Fallback` state.
 * - The pending indicator is drawn only for [TangemAvatar.UiState.Account] at [TangemAvatar.Size.X72],
 *   [TangemAvatar.Size.X44] and [TangemAvatar.Size.X40] — Figma defines no indicator at
 *   [TangemAvatar.Size.X16]. It sits flush with the corner at [TangemAvatar.Size.X72], overhangs it by 1dp
 *   at the smaller sizes, and punches a transparent ring behind itself.
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

    val version = DsVersion(major = 1, minor = 3, patch = 1)

    /** Fixed size presets: `X72` = 72dp … `X16` = 16dp. */
    enum class Size { X72, X44, X40, X16 }

    /**
     * Fill palette of the [UiState.Account] and [UiState.User] variants — the twelve user-selectable account
     * colours of the Figma `Avatar Module [Fill Color]`. The built-in account kinds carry their own fill, see
     * [AccountKind].
     */
    enum class FillColor {
        AzureBlue,
        CaribbeanBlue,
        DullLavender,
        CandyGrapeFizz,
        SweetDesire,
        PalatinateBlue,
        FuchsiaNebula,
        MexicanPink,
        Pelati,
        Pattypan,
        UfoGreen,
        VitalGreen,
    }

    /**
     * Built-in accounts of the Figma `Avatar Module [Account]`, each with a fixed fill and icon of its own.
     */
    enum class AccountKind {
        /** Violet fill with the two-tone predictions icon. */
        Prediction,

        /** Yellow fill with the two-tone gacha cards icon. */
        Gacha,

        /** Visa-violet fill with the Visa wordmark, drawn larger than the other account icons. */
        Visa,

        /** Green fill with the safe icon. */
        Money,
    }

    /** Background and glyph colour scheme of the [UiState.Icon] variant. */
    enum class Appearance { Neutral, Success, Error, Warning, Info }

    /**
     * Outline every [UiState] is clipped to, mirroring the Figma `Rounded` property: `Rounded=True` is
     * [Circle], `Rounded=False` is [RoundedSquare] with a per-[Size] corner radius.
     */
    enum class Shape { Circle, RoundedSquare }

    /** Lifecycle of the transaction an avatar stands for, driving its colour scheme. */
    enum class Status { Progress, Finished, Failed, Pending }

    /** What a [TangemAvatar] shows — one subtype per Figma variant. */
    @Immutable
    sealed class UiState {

        abstract val shape: Shape

        /**
         * Account variant. Pick the content through a constructor:
         * - `iconRes` / `imageVector` with a [FillColor] — the Figma `Crypto` account: the colour fill with
         *   the icon (half the avatar's side) tinted white at 80%. Pass a single-colour icon; its own colours
         *   are replaced by the tint.
         * - `letter` with a [FillColor] — a single letter, uppercased when drawn. The `TextReference` form
         *   takes the first character of the resolved text, so a name defined as a string resource can be
         *   passed whole, and shows only the fill when it resolves empty.
         * - `kind` — one of the built-in accounts, with its own fill and icon. See [AccountKind].
         *
         * @param hasIndicator Draws the pending clock badge over the bottom-end corner. Ignored at
         * [Size.X16].
         * @param shape Outline. Defaults to [Shape.RoundedSquare], unlike every other variant.
         */
        @ConsistentCopyVisibility
        data class Account internal constructor(
            internal val glyph: AvatarAccountGlyph,
            val hasIndicator: Boolean,
            override val shape: Shape,
        ) : UiState() {

            constructor(
                @DrawableRes iconRes: Int,
                color: FillColor,
                hasIndicator: Boolean = false,
                shape: Shape = Shape.RoundedSquare,
            ) : this(AvatarAccountGlyph.Icon(Either.Left(iconRes), color), hasIndicator, shape)

            constructor(
                imageVector: ImageVector,
                color: FillColor,
                hasIndicator: Boolean = false,
                shape: Shape = Shape.RoundedSquare,
            ) : this(AvatarAccountGlyph.Icon(Either.Right(imageVector), color), hasIndicator, shape)

            constructor(
                letter: Char,
                color: FillColor,
                hasIndicator: Boolean = false,
                shape: Shape = Shape.RoundedSquare,
            ) : this(AvatarAccountGlyph.Letter(stringReference(letter.toString()), color), hasIndicator, shape)

            constructor(
                letter: TextReference,
                color: FillColor,
                hasIndicator: Boolean = false,
                shape: Shape = Shape.RoundedSquare,
            ) : this(AvatarAccountGlyph.Letter(letter, color), hasIndicator, shape)

            constructor(
                kind: AccountKind,
                hasIndicator: Boolean = false,
                shape: Shape = Shape.RoundedSquare,
            ) : this(AvatarAccountGlyph.Kind(kind), hasIndicator, shape)
        }

        /**
         * User variant — filled with [color] and carrying [initial].
         *
         * @param initial Single character, uppercased when drawn. `null` renders the neutral fallback
         * person glyph and ignores [color].
         * @param color Fill colour. See [FillColor].
         * @param shape Outline. See [Shape].
         */
        data class User(
            val initial: Char?,
            val color: FillColor,
            override val shape: Shape = Shape.Circle,
        ) : UiState()

        /**
         * Address variant — the Blockies identicon generated from [address].
         *
         * @param address Raw address used as the identicon seed. Blank renders nothing.
         * @param shape Outline the identicon is clipped to. See [Shape].
         */
        data class Address(val address: String, override val shape: Shape = Shape.Circle) : UiState()

        /**
         * Icon variant — a tinted background with a glyph.
         *
         * @param icon Glyph to draw.
         * @param appearance Colour scheme. See [Appearance].
         * @param shape Outline. See [Shape].
         */
        data class Icon(
            internal val icon: Either<Int, ImageVector>,
            val appearance: Appearance = Appearance.Neutral,
            override val shape: Shape = Shape.Circle,
        ) : UiState() {

            constructor(
                @DrawableRes iconRes: Int,
                appearance: Appearance = Appearance.Neutral,
                shape: Shape = Shape.Circle,
            ) : this(Either.Left(iconRes), appearance, shape)

            constructor(
                imageVector: ImageVector,
                appearance: Appearance = Appearance.Neutral,
                shape: Shape = Shape.Circle,
            ) : this(Either.Right(imageVector), appearance, shape)
        }

        /**
         * Transaction variant — the transaction's glyph, coloured by [status].
         *
         * @param icon Glyph describing the transaction kind (send, receive, swap…).
         * @param status Lifecycle driving the colour scheme. See [Status].
         * @param shape Outline. See [Shape].
         */
        data class Transaction(
            internal val icon: Either<Int, ImageVector>,
            val status: Status,
            override val shape: Shape = Shape.Circle,
        ) : UiState() {

            constructor(
                @DrawableRes iconRes: Int,
                status: Status,
                shape: Shape = Shape.Circle,
            ) : this(Either.Left(iconRes), status, shape)

            constructor(
                imageVector: ImageVector,
                status: Status,
                shape: Shape = Shape.Circle,
            ) : this(Either.Right(imageVector), status, shape)
        }

        /**
         * Asset variant — an image filling the whole avatar, cropped to fit. Pick the source through the
         * `url` (loaded remotely, shimmering until it arrives and falling back to a neutral fill on error),
         * `iconRes` (vector or raster drawable) or `imageVector` constructor.
         *
         * @param shape Outline the image is clipped to. An image with transparent corners, such as a round
         * token logo, looks the same in either shape.
         */
        @ConsistentCopyVisibility
        data class Asset internal constructor(
            internal val source: AvatarAssetSource,
            override val shape: Shape,
        ) : UiState() {

            constructor(url: String, shape: Shape = Shape.Circle) : this(AvatarAssetSource.Url(url), shape)

            constructor(
                @DrawableRes iconRes: Int,
                shape: Shape = Shape.Circle,
            ) : this(AvatarAssetSource.Local(Either.Left(iconRes)), shape)

            constructor(
                imageVector: ImageVector,
                shape: Shape = Shape.Circle,
            ) : this(AvatarAssetSource.Local(Either.Right(imageVector)), shape)
        }

        /**
         * Loading placeholder.
         *
         * @param shape Outline — match the variant the placeholder stands in for. See [Shape].
         */
        data class Shimmer(override val shape: Shape = Shape.Circle) : UiState()
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
                imageVector = Icons.il_account_airplane,
                color = TangemAvatar.FillColor.AzureBlue,
            ),
        )
        PreviewRow(state = TangemAvatar.UiState.User(initial = 'A', color = TangemAvatar.FillColor.CandyGrapeFizz))
        PreviewRow(state = TangemAvatar.UiState.User(initial = null, color = TangemAvatar.FillColor.CandyGrapeFizz))
        PreviewRow(state = TangemAvatar.UiState.Address(address = "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984"))
        PreviewRow(
            state = TangemAvatar.UiState.Account(
                letter = 'A',
                color = TangemAvatar.FillColor.CandyGrapeFizz,
                hasIndicator = true,
            ),
        )
        PreviewRow(state = TangemAvatar.UiState.Icon(Icons.ic_grid_plus_20, TangemAvatar.Appearance.Neutral))
        PreviewRow(
            state = TangemAvatar.UiState.Icon(
                imageVector = Icons.ic_grid_plus_20,
                appearance = TangemAvatar.Appearance.Info,
                shape = TangemAvatar.Shape.RoundedSquare,
            ),
        )
        PreviewRow(
            state = TangemAvatar.UiState.Asset(
                iconRes = R.drawable.ic_airplane_mode_24,
                shape = TangemAvatar.Shape.RoundedSquare,
            ),
        )
    }
}

@Preview(name = "Account kinds Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Account kinds Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TangemAvatarAccountKindsPreview() {
    PreviewContainer {
        TangemAvatar.AccountKind.entries.forEach { kind ->
            PreviewRow(state = TangemAvatar.UiState.Account(kind = kind, hasIndicator = true))
        }
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
                ),
            )
        }
        PreviewRow(state = TangemAvatar.UiState.Shimmer())
        PreviewRow(state = TangemAvatar.UiState.Shimmer(TangemAvatar.Shape.RoundedSquare))
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