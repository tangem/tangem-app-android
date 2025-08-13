package com.tangem.core.ui.components.provider

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.audits.AuditLabel
import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.badge.Badge
import com.tangem.core.ui.components.badge.entity.BadgeUM
import com.tangem.core.ui.components.provider.entity.ProviderChooseUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.persistentListOf

private const val DISABLED_ICON_ALPHA = 0.4f

/**
 * Provider Row - Choose Crypto component
 *
 * @param providerChooseUM  component ui model
 * @param onClick           click listener
 * @param modifier          composable modifier
 *
 * @see <a href="https://www.figma.com/design/JJuqr3gX9IC4WBv3C95uqj/Android--Copy-?node-id=3467-2643&t=Tg0OOJTQK9H4KBWL-4">Figma</a>
 */
@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries", "LongMethod")
fun ProviderChooseCrypto(providerChooseUM: ProviderChooseUM, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ConstraintLayout(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .selectedBorder(isSelected = providerChooseUM.isSelected)
            .clickable(
                enabled = !providerChooseUM.hasError(),
                onClick = onClick,
            )
            .fillMaxWidth()
            .padding(all = 12.dp),
    ) {
        val (iconRef, titleRef, extraRef, infoRef, labelRef) = createRefs()
        val titleEndRef = createStartBarrier(infoRef, labelRef)

        IconContent(
            iconUrl = providerChooseUM.iconUrl,
            modifier = Modifier
                .alpha(
                    if (providerChooseUM.hasError()) {
                        DISABLED_ICON_ALPHA
                    } else {
                        1.0f
                    },
                )
                .constrainAs(iconRef) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
        )
        TitleContent(
            providerChooseUM = providerChooseUM,
            modifier = Modifier.constrainAs(titleRef) {
                start.linkTo(iconRef.end, 12.dp)
                end.linkTo(titleEndRef, 4.dp)
                top.linkTo(parent.top)
                bottom.linkTo(extraRef.top)
                width = Dimension.fillToConstraints
            },
        )
        ExtraContent(
            providerChooseUM = providerChooseUM,
            modifier = Modifier.constrainAs(extraRef) {
                start.linkTo(iconRef.end, 12.dp)
                end.linkTo(labelRef.start, 4.dp)
                top.linkTo(titleRef.bottom)
                bottom.linkTo(parent.bottom)
                width = Dimension.fillToConstraints
                visibility = if (providerChooseUM.extraUM is ProviderChooseUM.ExtraUM.Empty) {
                    Visibility.Gone
                } else {
                    Visibility.Visible
                }
            },
        )
        InfoContent(
            text = providerChooseUM.infoText,
            modifier = Modifier.constrainAs(infoRef) {
                end.linkTo(parent.end)
                top.linkTo(parent.top)
                bottom.linkTo(labelRef.top, 4.dp)
            },
        )
        LabelContent(
            labelUM = providerChooseUM.labelUM,
            modifier = Modifier.constrainAs(labelRef) {
                end.linkTo(parent.end)
                top.linkTo(infoRef.bottom)
                bottom.linkTo(parent.bottom)
                visibility = if (providerChooseUM.labelUM == null) {
                    Visibility.Gone
                } else {
                    Visibility.Visible
                }
            },
        )
    }
}

@Composable
private fun IconContent(iconUrl: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp)),
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(iconUrl)
            .crossfade(enable = true)
            .allowHardware(false)
            .build(),
        loading = {
            RectangleShimmer(radius = 8.dp)
        },
        error = {
            Box(
                modifier = Modifier.background(
                    color = TangemColorPalette.Light1,
                    shape = RoundedCornerShape(8.dp),
                ),
            )
        },
        contentDescription = null,
    )
}

@Composable
private fun TitleContent(providerChooseUM: ProviderChooseUM, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = providerChooseUM.title.resolveReference(),
            style = TangemTheme.typography.subtitle2,
            color = if (providerChooseUM.hasError()) {
                TangemTheme.colors.text.secondary
            } else {
                TangemTheme.colors.text.primary1
            },
            maxLines = 1,
        )
        Text(
            text = providerChooseUM.subtitle.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
        )
    }
}

@Composable
private fun InfoContent(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = text.resolveReference(),
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.primary1,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun ExtraContent(providerChooseUM: ProviderChooseUM, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        when (val extraUM = providerChooseUM.extraUM) {
            is ProviderChooseUM.ExtraUM.Action -> Text(
                text = extraUM.text.resolveReference(),
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TangemTheme.colors.button.secondary)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
            is ProviderChooseUM.ExtraUM.Badges -> extraUM.badgeList.fastForEach { badge ->
                Badge(
                    iconRes = badge.iconRes,
                    text = badge.text,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            is ProviderChooseUM.ExtraUM.Error -> Text(
                text = extraUM.text.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.padding(top = 6.dp),
            )
            else -> Unit
        }
    }
}

@Composable
private fun LabelContent(labelUM: ProviderChooseUM.LabelUM?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
    ) {
        when (labelUM) {
            is ProviderChooseUM.LabelUM.Info -> AuditLabel(labelUM.auditLabelUM)
            is ProviderChooseUM.LabelUM.Text -> Text(
                text = labelUM.text.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.warning,
            )
            null -> Unit
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ProviderChooseCrypto_Preview(
    @PreviewParameter(ProviderChooseCryptoPreviewProvider::class) params: ProviderChooseUM,
) {
    TangemThemePreview {
        ProviderChooseCrypto(
            providerChooseUM = params,
            onClick = {},
        )
    }
}

private class ProviderChooseCryptoPreviewProvider : PreviewParameterProvider<ProviderChooseUM> {
    override val values: Sequence<ProviderChooseUM>
        get() = sequenceOf(
            ProviderChooseUM(
                title = stringReference("ChangeHero"),
                subtitle = stringReference("CEX"),
                infoText = stringReference("1 800,00 POL"),
                iconUrl = "",
                extraUM = ProviderChooseUM.ExtraUM.Badges(
                    persistentListOf(
                        BadgeUM(
                            text = stringReference("4.9"),
                            iconRes = R.drawable.ic_star_outline_24,
                        ),
                        BadgeUM(
                            text = stringReference("5 mins"),
                            iconRes = R.drawable.ic_speed_24,
                        ),
                        BadgeUM(
                            text = stringReference("$3.45"),
                            iconRes = R.drawable.ic_gas_24,
                        ),
                    ),
                ),
                isSelected = true,
                labelUM = ProviderChooseUM.LabelUM.Info(
                    auditLabelUM = AuditLabelUM(
                        text = resourceReference(R.string.express_provider_best_rate),
                        type = AuditLabelUM.Type.Info,
                    ),
                ),
            ),
            ProviderChooseUM(
                title = stringReference("Changelly"),
                subtitle = stringReference("CEX"),
                infoText = stringReference("1 799,12 POL"),
                iconUrl = "",
                extraUM = ProviderChooseUM.ExtraUM.Action(
                    text = resourceReference(R.string.express_provider_permission_needed),
                ),
                isSelected = false,
                labelUM = ProviderChooseUM.LabelUM.Text(
                    text = stringReference("–0.2%"),
                ),
            ),
            ProviderChooseUM(
                title = stringReference("Changelly"),
                subtitle = stringReference("CEX"),
                infoText = stringReference("1 799,12 POL"),
                iconUrl = "",
                extraUM = ProviderChooseUM.ExtraUM.Error(
                    text = resourceReference(R.string.express_provider_min_amount, wrappedList("1 POL")),
                ),
                isSelected = false,
                labelUM = ProviderChooseUM.LabelUM.Text(
                    text = stringReference("–0.2%"),
                ),
            ),
            ProviderChooseUM(
                title = stringReference("Changelly"),
                subtitle = stringReference("CEX"),
                infoText = stringReference("1 POL"),
                iconUrl = "",
                extraUM = ProviderChooseUM.ExtraUM.Empty,
                isSelected = false,
                labelUM = ProviderChooseUM.LabelUM.Text(
                    text = stringReference("–0.2%"),
                ),
            ),
            ProviderChooseUM(
                title = stringReference("Changelly"),
                subtitle = stringReference("CEX"),
                infoText = stringReference("1 POL"),
                iconUrl = "",
                extraUM = ProviderChooseUM.ExtraUM.Empty,
                isSelected = false,
                labelUM = null,
            ),
            ProviderChooseUM(
                title = stringReference("Changelly"),
                subtitle = stringReference("CEX"),
                infoText = stringReference("1 POL"),
                iconUrl = "",
                extraUM = ProviderChooseUM.ExtraUM.Badges(
                    persistentListOf(
                        BadgeUM(
                            text = stringReference("4.9"),
                            iconRes = R.drawable.ic_star_outline_24,
                        ),
                        BadgeUM(
                            text = stringReference("5 mins"),
                            iconRes = R.drawable.ic_speed_24,
                        ),
                        BadgeUM(
                            text = stringReference("$3.45"),
                            iconRes = R.drawable.ic_gas_24,
                        ),
                    ),
                ),
                isSelected = false,
                labelUM = null,
            ),
        )
}
// endregion