package com.tangem.features.feed.ui.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.ambient.TangemAmbientBackground
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_16

private val FeedBlockShape = RoundedCornerShape(size = 20.dp)

private const val TITLE_SKELETON_WIDTH_FRACTION = 0.45f
private const val DESCRIPTION_SKELETON_WIDTH_FRACTION = 0.6f

/**
 * Half-height shortcut card: icon at the top start, title at the bottom start.
 *
 * @param modifier must carry the card's size — the row decides it.
 */
@Composable
internal fun FeedShortcutCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    TangemSurface(
        modifier = modifier,
        color = TangemTheme.colors3.bg.opaque.primary,
        shape = FeedBlockShape,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            icon()
            FixedFontScale {
                Text(
                    text = title,
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.primary,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
            }
        }
    }
}

/**
 * Full-height promo card: an ambient-lit surface carrying a product icon and a two-line pitch. The
 * "+" in the top end corner is an affordance, not a second target — the whole card is one click.
 *
 * @param title the muted line the pitch opens with.
 * @param description the pitch itself; `null` leaves the line out, for a card whose pitch needs a
 * figure it doesn't have yet.
 * @param modifier must carry the card's size — the row decides it.
 */
@Suppress("LongParameterList")
@Composable
internal fun FeedPromoCard(
    title: String,
    description: String?,
    ambientVariant: TangemAmbientBackground.Variant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    FeedPromoCardFrame(
        ambientVariant = ambientVariant,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
    ) {
        Text(
            text = title,
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
        if (description != null) {
            Text(
                text = description,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3,
            )
        }
    }
}

@Composable
internal fun FeedPromoCardSkeleton(
    ambientVariant: TangemAmbientBackground.Variant,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    FeedPromoCardFrame(
        ambientVariant = ambientVariant,
        onClick = null,
        modifier = modifier,
        icon = icon,
    ) {
        TangemShimmer(
            style = TangemTheme.typography3.caption.medium,
            modifier = Modifier.fillMaxWidth(fraction = TITLE_SKELETON_WIDTH_FRACTION),
        )
        TangemShimmer(
            style = TangemTheme.typography3.subheading.medium,
            modifier = Modifier.fillMaxWidth(fraction = DESCRIPTION_SKELETON_WIDTH_FRACTION),
        )
    }
}

@Composable
private fun FeedPromoCardFrame(
    ambientVariant: TangemAmbientBackground.Variant,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    pitch: @Composable ColumnScope.() -> Unit,
) {
    TangemSurface(
        modifier = modifier,
        color = TangemTheme.colors3.bg.opaque.primary,
        shape = FeedBlockShape,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TangemAmbientBackground(
                variant = ambientVariant,
                placement = TangemAmbientBackground.Placement.Card,
                modifier = Modifier.matchParentSize(),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    icon()
                    PlusAffordance()
                }
                FixedFontScale {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
                        content = pitch,
                    )
                }
            }
        }
    }
}

/**
 * A card with nothing but its title, for a block whose pitch hasn't been designed yet.
 *
 * @param modifier must carry the card's size — the row decides it.
 */
@Composable
internal fun FeedTitleCard(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier,
        color = TangemTheme.colors3.bg.opaque.primary,
        shape = FeedBlockShape,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.padding(all = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
    }
}

@Composable
private fun FixedFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val fixedFontScaleDensity = remember(density) {
        Density(density = density.density, fontScale = 1f)
    }

    CompositionLocalProvider(LocalDensity provides fixedFontScaleDensity, content = content)
}

/** Rounded-square badge every promo card puts its product icon in. */
@Composable
internal fun FeedBlockIconBadge(background: Color, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(size = 40.dp)
            .clip(shape = RoundedCornerShape(size = 12.dp))
            .background(color = background),
        contentAlignment = Alignment.Center,
        content = { icon() },
    )
}

@Composable
private fun PlusAffordance() {
    Box(
        modifier = Modifier
            .size(size = 24.dp)
            .clip(shape = CircleShape)
            .background(color = TangemTheme.colors3.bg.opaque.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.ic_sign_plus_16,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.secondary,
            modifier = Modifier.size(size = 16.dp),
        )
    }
}