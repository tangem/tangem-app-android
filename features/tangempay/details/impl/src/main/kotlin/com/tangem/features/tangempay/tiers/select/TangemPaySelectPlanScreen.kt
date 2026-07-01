package com.tangem.features.tangempay.tiers.select

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun TangemPaySelectPlanScreen(state: TangemPaySelectPlanUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SelectPlanTopBar(state = state)

            AnimatedContent(
                targetState = state.content,
                modifier = Modifier.weight(1f),
                contentKey = { it::class },
                label = "SelectPlanContent",
            ) { content ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (content) {
                        is TangemPaySelectPlanUM.Content.Select -> SelectContent(state = state)
                        is TangemPaySelectPlanUM.Content.Confirm -> ConfirmContent(state = state, content = content)
                    }
                }
            }
            AnimatedContent(
                targetState = state.content,
                contentKey = { it::class },
                label = "SelectPlanFooter",
            ) { content ->
                when (content) {
                    is TangemPaySelectPlanUM.Content.Select -> SelectFooter(content = content)
                    is TangemPaySelectPlanUM.Content.Confirm -> ConfirmFooter(content = content)
                }
            }
        }
    }
}

@Composable
private fun SelectPlanTopBar(state: TangemPaySelectPlanUM) {
    TangemTopBar(
        modifier = Modifier.statusBarsPadding(),
        title = state.topBarTitle,
        startContent = { TangemButton.Back(onClick = state.onBackClick) },
        endContent = { TangemButton.Close(onClick = state.onCloseClick) },
    )
}

@Composable
private fun ColumnScope.SelectContent(state: TangemPaySelectPlanUM) {
    val plans = state.plans.takeIf { it.isNotEmpty() } ?: return

    val pagerState = rememberPagerState(
        initialPage = state.selectedIndex.coerceIn(0, plans.lastIndex),
        pageCount = { plans.size },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect(state.onPlanSelected)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        pageSpacing = 8.dp,
        beyondViewportPageCount = 1,
    ) { page ->
        PlanCard(imageUrl = plans[page].imageUrl)
    }

    if (plans.size > 1) {
        TangemPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 24.dp, top = 16.dp),
        )
    }
    val plan = plans[state.selectedIndex.coerceIn(0, plans.lastIndex)]
    PlanDetails(
        title = plan.name,
        points = plan.points,
    )
}

@Composable
private fun ColumnScope.PlanDetails(title: TextReference, points: ImmutableList<TangemPaySelectPlanUM.PointUM>) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 12.dp),
        text = title.resolveReference(),
        style = TangemTheme.typography3.heading.medium,
        color = TangemTheme.colors3.text.primary,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        points.fastForEach { point -> PlanPoint(point = point) }
    }
}

@Composable
private fun PlanPoint(point: TangemPaySelectPlanUM.PointUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = CoreUiR.drawable.ic_information_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.secondary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = point.title.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            point.body?.let { body ->
                Text(
                    text = body.resolveReference(),
                    style = TangemTheme.typography3.subheading.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ConfirmContent(state: TangemPaySelectPlanUM, content: TangemPaySelectPlanUM.Content.Confirm) {
    val selectedPlan = state.plans.getOrNull(state.selectedIndex) ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        PlanCard(imageUrl = selectedPlan.imageUrl)
    }
    Spacer(modifier = Modifier.weight(1f))
    PlanDetails(
        title = content.title,
        points = content.points,
    )
}

@Composable
private fun SelectFooter(content: TangemPaySelectPlanUM.Content.Select, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_select_plan_compare),
            onClick = content.onComparePlansClick,
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_select_plan_select),
            onClick = content.onSelectClick,
        )
    }
}

@Composable
private fun ConfirmFooter(content: TangemPaySelectPlanUM.Content.Confirm, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_select_plan_cancel),
            onClick = content.onCancelClick,
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = content.confirmButtonText,
            onClick = content.onConfirmClick,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun PlanCard(imageUrl: String?, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio = 266f / 172f)
            .clip(RoundedCornerShape(12.dp)),
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        loading = {
            RectangleShimmer(
                modifier = Modifier.fillMaxSize(),
                radius = 12.dp,
            )
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TangemTheme.colors3.bg.secondary),
            )
        },
        contentScale = ContentScale.Crop,
        contentDescription = null,
    )
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun TangemPaySelectPlanScreenPreview() {
    TangemThemePreviewRedesign {
        TangemPaySelectPlanScreen(state = previewState(isConfirm = false))
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Composable
private fun TangemPaySelectPlanConfirmPreview() {
    TangemThemePreviewRedesign {
        TangemPaySelectPlanScreen(state = previewState(isConfirm = true))
    }
}

private fun previewState(isConfirm: Boolean) = TangemPaySelectPlanUM(
    topBarTitle = stringReference(if (isConfirm) "Confirm selection" else "Select plan"),
    plans = persistentListOf(
        TangemPaySelectPlanUM.PlanUM(
            name = stringReference("Plus"),
            imageUrl = null,
            points = persistentListOf(
                TangemPaySelectPlanUM.PointUM(
                    title = stringReference("2 airport lounge pass a year"),
                    body = stringReference("Travel insurance and other benefits"),
                ),
                TangemPaySelectPlanUM.PointUM(stringReference("$50.000 daily spending limit"), null),
                TangemPaySelectPlanUM.PointUM(stringReference("$29.99 / month"), null),
            ),
        ),
    ),
    selectedIndex = 0,
    onPlanSelected = {},
    onBackClick = {},
    onCloseClick = {},
    content = if (isConfirm) {
        TangemPaySelectPlanUM.Content.Confirm(
            title = stringReference("We will issue Visa Plus for you"),
            points = persistentListOf(
                TangemPaySelectPlanUM.PointUM(
                    title = stringReference("You will get your virtual Visa Plus in minutes"),
                    body = null,
                ),
                TangemPaySelectPlanUM.PointUM(
                    title = stringReference("$29.99 / month will be charged from your account"),
                    body = null,
                ),
            ),
            confirmButtonText = stringReference("Upgrade plan"),
            onCancelClick = {},
            onConfirmClick = {},
        )
    } else {
        TangemPaySelectPlanUM.Content.Select(
            onComparePlansClick = {},
            onSelectClick = {},
        )
    },
)