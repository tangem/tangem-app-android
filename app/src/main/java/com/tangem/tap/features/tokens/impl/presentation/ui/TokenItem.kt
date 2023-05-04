package com.tangem.tap.features.tokens.impl.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.compose.extensions.toPx
import com.tangem.tap.features.tokens.impl.presentation.states.TokenItemState
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun TokenItem(model: TokenItemState) {
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TangemTheme.dimens.spacing16),
    ) {
        val (icon, title, availableNetworksText) = createRefs()
        val (briefNetworksList, detailedNetworksList, changeNetworksViewButton) = createRefs()

        val spacing16 = TangemTheme.dimens.spacing16
        val spacing6 = TangemTheme.dimens.spacing6

        Icon(
            name = model.fullName,
            iconUrl = model.iconUrl,
            modifier = Modifier.constrainAs(icon) {
                top.linkTo(parent.top)
                start.linkTo(anchor = parent.start, margin = spacing16)
            },
        )

        Title(
            title = model.fullName,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top)
                start.linkTo(icon.end, margin = spacing16)
                end.linkTo(anchor = changeNetworksViewButton.start, margin = spacing16)
                width = Dimension.fillToConstraints
            },
        )

        Subtitle(
            isExpanded = isExpanded,
            modifier = Modifier.constrainAs(availableNetworksText) {
                top.linkTo(title.bottom, margin = spacing6)
                start.linkTo(icon.end, margin = spacing16)
                end.linkTo(anchor = changeNetworksViewButton.start, margin = spacing16)
                width = Dimension.fillToConstraints
            },
        )

        BriefNetworksList(
            isCollapsed = !isExpanded,
            networks = model.networks,
            modifier = Modifier.constrainAs(briefNetworksList) {
                top.linkTo(title.bottom, margin = spacing6)
                start.linkTo(icon.end, margin = spacing16)
                end.linkTo(anchor = changeNetworksViewButton.start, margin = spacing16)
                width = Dimension.fillToConstraints
            },
        )

        DetailedNetworksList(
            isExpanded = isExpanded,
            token = model,
            networks = model.networks,
            modifier = Modifier.constrainAs(detailedNetworksList) {
                top.linkTo(anchor = availableNetworksText.bottom, margin = spacing16)
                centerHorizontallyTo(parent)
                width = Dimension.fillToConstraints
            },
        )

        ChangeNetworksViewButton(
            isExpanded = isExpanded,
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.constrainAs(changeNetworksViewButton) {
                top.linkTo(parent.top)
                end.linkTo(anchor = parent.end, margin = spacing6)
                if (!isExpanded) bottom.linkTo(parent.bottom)
            },
        )
    }
}

@Composable
private fun Icon(name: String, iconUrl: String, modifier: Modifier = Modifier) {
    val iconModifier = modifier.size(size = TangemTheme.dimens.size46)

    SubcomposeAsyncImage(
        modifier = iconModifier,
        model = ImageRequest.Builder(context = LocalContext.current)
            .size(size = TangemTheme.dimens.size46.toPx().toInt())
            .data(data = iconUrl)
            .crossfade(enable = true)
            .build(),
        contentDescription = null,
        loading = { PlaceholderIcon(name = name, modifier = iconModifier) },
        error = { PlaceholderIcon(name = name, modifier = iconModifier) },
    )
}

// TODO(use CurrencyPlaceholderIcon here?)
@Composable
private fun PlaceholderIcon(name: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(TangemTheme.colors.icon.primary1, shape = CircleShape),
    ) {
        Text(
            text = name.firstOrNull()?.titlecase() ?: "",
            color = TangemTheme.colors.icon.primary2,
            maxLines = 1,
            // FIXME("Incorrect typography. Replace with typography from design system")
            style = TangemTheme.typography.h3.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun Title(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.Start,
        maxLines = 1,
        style = TangemTheme.typography.body1,
    )
}

@Composable
private fun Subtitle(isExpanded: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isExpanded,
        modifier = modifier,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Text(
            text = stringResource(id = R.string.currency_subtitle_expanded),
            color = TangemTheme.colors.text.secondary,
            maxLines = 1,
            style = TangemTheme.typography.body2,
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ChangeNetworksViewButton(isExpanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(size = TangemTheme.dimens.size46)) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = { fadeIn() + scaleIn() with scaleOut() + fadeOut() },
        ) { isExpanded ->
            Icon(
                painter = painterResource(
                    id = if (isExpanded) R.drawable.ic_arrow_extended else R.drawable.ic_arrow_collapsed,
                ),
                contentDescription = null,
                tint = TangemTheme.colors.icon.secondary,
            )
        }
    }
}

@Preview
@Composable
private fun Preview_TokenItem_ManageAccess() {
    TangemTheme {
        TokenItem(model = TokenListPreviewData.createManageToken())
    }
}

@Preview
@Composable
private fun Preview_TokenItem_ReadAccess() {
    TangemTheme {
        TokenItem(model = TokenListPreviewData.createReadToken())
    }
}