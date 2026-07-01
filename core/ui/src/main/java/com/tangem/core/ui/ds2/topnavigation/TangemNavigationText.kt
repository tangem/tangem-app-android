package com.tangem.core.ui.ds2.topnavigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Text component for [TangemTopNavigation] title / subtitle slots.
 *
 * Unlike a regular [Text], this composable pins the font scale to `1f` for its subtree so that the
 * system accessibility "font size" setting cannot stretch the top navigation vertically.
 *
 * @param text Label text.
 * @param role Semantic role. See [TangemNavigationText.Role].
 * @param modifier Modifier applied to the underlying [Text].
 * @param maxLines Maximum visible lines before truncation.
 * @param overflow Overflow behavior. Defaults to ellipsis.
 */
@Composable
fun TangemNavigationText(
    text: String,
    role: TangemNavigationText.Role,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val density = LocalDensity.current
    val fixedFontScaleDensity = remember(density) {
        Density(density = density.density, fontScale = 1f)
    }
    CompositionLocalProvider(LocalDensity provides fixedFontScaleDensity) {
        Text(
            text = text,
            modifier = modifier,
            color = navigationTextColor(role),
            style = navigationTextStyle(role),
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}

@Composable
fun TangemNavigationText(
    text: AnnotatedString,
    role: TangemNavigationText.Role,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val density = LocalDensity.current
    val fixedFontScaleDensity = remember(density) {
        Density(density = density.density, fontScale = 1f)
    }
    CompositionLocalProvider(LocalDensity provides fixedFontScaleDensity) {
        Text(
            text = text,
            modifier = modifier,
            color = navigationTextColor(role),
            style = navigationTextStyle(role),
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}

@Composable
@NonRestartableComposable
fun TangemNavigationText(
    text: TextReference,
    role: TangemNavigationText.Role,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    TangemNavigationText(
        text = text.resolveAnnotatedReference(),
        role = role,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
private fun navigationTextStyle(role: TangemNavigationText.Role): TextStyle = when (role) {
    TangemNavigationText.Role.Title -> TangemTheme.typography3.body.medium
    TangemNavigationText.Role.Subtitle -> TangemTheme.typography3.caption.medium
}

@Composable
private fun navigationTextColor(role: TangemNavigationText.Role): Color = when (role) {
    TangemNavigationText.Role.Title -> TangemTheme.colors3.text.primary
    TangemNavigationText.Role.Subtitle -> TangemTheme.colors3.text.secondary
}

object TangemNavigationText {

    /** Semantic role of a label inside [TangemTopNavigation], driving its typography and color. */
    @Immutable
    enum class Role { Title, Subtitle }
}