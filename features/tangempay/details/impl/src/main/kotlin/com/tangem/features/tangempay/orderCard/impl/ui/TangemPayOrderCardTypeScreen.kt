package com.tangem.features.tangempay.orderCard.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_24
import com.tangem.core.ui.res.generated.icons.ic_cloud_12_filled
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderCardType
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardTypeUM
import kotlinx.coroutines.launch
import com.tangem.core.ui.R as CoreUiR

private const val CARD_ASPECT_RATIO = 1.585f

@Composable
internal fun TangemPayOrderCardTypeScreen(state: TangemPayOrderCardTypeUM, modifier: Modifier = Modifier) {
    val availableTypes = state.availableTypes
    val cardPagerState = rememberPagerState(pageCount = { availableTypes.size })
    val detailsPagerState = rememberPagerState(pageCount = { availableTypes.size })

    LaunchedEffect(cardPagerState, detailsPagerState) {
        snapshotFlow { cardPagerState.currentPage to cardPagerState.currentPageOffsetFraction }
            .collect { (page, offset) ->
                detailsPagerState.scrollToPage(page, offset)
            }
    }

    OrderTypeContent(
        state = state,
        availableTypes = availableTypes,
        cardPagerState = cardPagerState,
        detailsPagerState = detailsPagerState,
        modifier = modifier,
    )
}

@Composable
private fun OrderTypeContent(
    state: TangemPayOrderCardTypeUM,
    availableTypes: List<OrderCardType>,
    cardPagerState: PagerState,
    detailsPagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ill_tangempay_order_card_bg),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            OrderTypeTopBar(onCloseClick = state.onBackClick)
            if (state.isError) {
                OrderTypeErrorState()
            } else {
                OrderTypeBody(
                    state = state,
                    availableTypes = availableTypes,
                    cardPagerState = cardPagerState,
                    detailsPagerState = detailsPagerState,
                )
            }
        }
    }
}

@Composable
private fun OrderTypeTopBar(onCloseClick: () -> Unit) {
    TangemTopNavigation(
        title = resourceReference(R.string.tangempay_order_type_title),
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        blurBackground = false,
        onClose = onCloseClick,
    )
}

@Composable
private fun ColumnScope.OrderTypeBody(
    state: TangemPayOrderCardTypeUM,
    availableTypes: List<OrderCardType>,
    cardPagerState: PagerState,
    detailsPagerState: PagerState,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CardArea(imageUrl = state.cardImageUrl, availableTypes = availableTypes, pagerState = cardPagerState)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
    ) {
        CardTypeTabs(availableTypes = availableTypes, pagerState = cardPagerState)
        SpacerH(16.dp)
        DetailsArea(state = state, availableTypes = availableTypes, pagerState = detailsPagerState)
        SpacerH(16.dp)
        SelectButton(state = state, availableTypes = availableTypes, pagerState = cardPagerState)
        SpacerH(12.dp)
    }
}

@Suppress("MagicNumber")
@Composable
private fun CardArea(
    imageUrl: String?,
    availableTypes: List<OrderCardType>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    if (availableTypes.size <= 1) {
        CardArtwork(
            imageUrl = imageUrl,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
        )
        return
    }
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 48.dp),
        pageSpacing = 24.dp,
    ) {
        CardArtwork(imageUrl = imageUrl, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DetailsArea(
    state: TangemPayOrderCardTypeUM,
    availableTypes: List<OrderCardType>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val heightReserve = remember {
        TangemPayOrderCardTypeUM.Plastic(
            country = "",
            deliveryFee = "",
            deliveryEtaMaxBusinessDays = 1,
            feeState = TangemPayOrderCardTypeUM.FeeState.InsufficientFunds,
        )
    }
    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().alpha(0f).clearAndSetSemantics {}) {
            PlasticDetails(plastic = heightReserve)
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.matchParentSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = false,
        ) { page ->
            TypeDetails(state = state, type = availableTypes[page])
        }
    }
}

@Composable
private fun CardTypeTabs(availableTypes: List<OrderCardType>, pagerState: PagerState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        availableTypes.fastForEachIndexed { index, type ->
            val isSelected = pagerState.currentPage == index
            TangemButton(
                variant = if (isSelected) TangemButton.Variant.Material else TangemButton.Variant.Ghost,
                size = TangemButton.Size.X11,
                text = resourceReference(type.titleRes()),
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun CardArtwork(imageUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT_RATIO)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(colors = listOf(Color(0xFF2A2E3A), Color(0xFF1C1F29))),
            ),
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(20.dp),
            imageVector = Icons.ic_cloud_12_filled,
            tint = TangemTheme.colors3.icon.staticDark,
            contentDescription = null,
        )
        Icon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(height = 16.dp, width = 44.dp),
            imageVector = ImageVector.vectorResource(CoreUiR.drawable.ic_visa_logo),
            tint = TangemTheme.colors3.icon.staticDark,
            contentDescription = null,
        )
        if (imageUrl != null) {
            SubcomposeAsyncImage(
                modifier = Modifier.matchParentSize(),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                loading = {},
                error = {},
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun TypeDetails(state: TangemPayOrderCardTypeUM, type: OrderCardType, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (type) {
            OrderCardType.Virtual -> VirtualDetails(virtual = state.virtual, isLoading = state.isLoading)
            OrderCardType.Plastic -> state.plastic?.let { PlasticDetails(plastic = it) }
        }
    }
}

@Composable
private fun VirtualDetails(virtual: TangemPayOrderCardTypeUM.Virtual, isLoading: Boolean) {
    InfoRow(
        title = resourceReference(R.string.tangempay_order_type_issue_fee),
        value = if (isLoading) null else stringReference(virtual.issueFee),
        divider = true,
    )
    InfoRow(
        title = resourceReference(R.string.tangempay_order_type_delivery_time),
        value = resourceReference(R.string.tangempay_order_type_delivery_time_instant),
    )
}

@Composable
private fun PlasticDetails(plastic: TangemPayOrderCardTypeUM.Plastic) {
    TangemRow(
        divider = true,
        contentLead = TangemRowContentLead.End,
        titleSlot = {
            TangemRowText(
                text = resourceReference(R.string.tangempay_order_type_delivery_to),
                role = TangemRowTextRole.Title,
            )
        },
        valueSlot = { RowValueText(text = stringReference(plastic.country)) },
        endSlot = {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.ic_chevron_down_24,
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
            )
        },
    )
    TangemRow(
        divider = true,
        contentLead = TangemRowContentLead.End,
        titleSlot = {
            TangemRowText(
                text = resourceReference(R.string.tangempay_order_type_delivery_fee),
                role = TangemRowTextRole.Title,
            )
        },
        subtitleSlot = if (plastic.feeState == TangemPayOrderCardTypeUM.FeeState.InsufficientFunds) {
            {
                CaptionText(
                    text = resourceReference(R.string.tangempay_order_type_not_enough_money),
                    color = TangemTheme.colors3.text.status.warning,
                )
            }
        } else {
            null
        },
        valueSlot = { RowValueText(text = stringReference(plastic.deliveryFee)) },
        subvalueSlot = if (plastic.feeState == TangemPayOrderCardTypeUM.FeeState.FreeDelivery) {
            {
                CaptionText(
                    text = resourceReference(R.string.tangempay_order_type_first_delivery_free),
                    color = TangemTheme.colors3.text.status.success,
                )
            }
        } else {
            null
        },
    )
    InfoRow(
        title = resourceReference(R.string.tangempay_order_type_delivery_time),
        value = pluralReference(
            id = R.plurals.tangempay_order_type_delivery_eta,
            count = plastic.deliveryEtaMaxBusinessDays,
            formatArgs = wrappedList(plastic.deliveryEtaMaxBusinessDays),
        ),
    )
}

@Composable
private fun InfoRow(title: TextReference, value: TextReference?, divider: Boolean = false) {
    TangemRow(
        divider = divider,
        contentLead = TangemRowContentLead.End,
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        valueSlot = {
            if (value == null) {
                TangemShimmer(
                    modifier = Modifier.width(64.dp),
                    style = TangemTheme.typography3.body.medium,
                    textAlign = TextAlign.End,
                )
            } else {
                RowValueText(text = value)
            }
        },
    )
}

@Composable
private fun RowValueText(text: TextReference) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = text.resolveReference(),
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun CaptionText(text: TextReference, color: Color) {
    Text(
        text = text.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = color,
    )
}

@Composable
private fun SelectButton(
    state: TangemPayOrderCardTypeUM,
    availableTypes: List<OrderCardType>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val currentType = availableTypes.getOrElse(pagerState.currentPage) { OrderCardType.Virtual }
    val isEnabled = when (currentType) {
        OrderCardType.Virtual -> !state.isLoading
        OrderCardType.Plastic ->
            !state.isLoading && state.plastic?.feeState != TangemPayOrderCardTypeUM.FeeState.InsufficientFunds
    }
    val onClick = when (currentType) {
        OrderCardType.Virtual -> state.onSelectVirtual
        OrderCardType.Plastic -> state.onSelectPlastic
    }
    TangemButton(
        modifier = modifier.fillMaxWidth(),
        variant = TangemButton.Variant.Primary,
        size = TangemButton.Size.X12,
        text = resourceReference(R.string.tangempay_order_type_select),
        isEnabled = isEnabled,
        onClick = onClick,
    )
}

@Composable
private fun ColumnScope.OrderTypeErrorState() {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    )
}

private fun OrderCardType.titleRes(): Int = when (this) {
    OrderCardType.Virtual -> R.string.tangempay_order_type_segment_virtual
    OrderCardType.Plastic -> R.string.tangempay_order_type_segment_plastic
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Preview(showBackground = true, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayOrderCardTypeScreenPreview(
    @PreviewParameter(OrderCardTypePreviewProvider::class) state: TangemPayOrderCardTypeUM,
) {
    TangemThemePreviewRedesign {
        val availableTypes = state.availableTypes
        val cardPagerState = rememberPagerState(
            initialPage = availableTypes.lastIndex,
            pageCount = { availableTypes.size },
        )
        val detailsPagerState = rememberPagerState(
            initialPage = availableTypes.lastIndex,
            pageCount = { availableTypes.size },
        )
        OrderTypeContent(
            state = state,
            availableTypes = availableTypes,
            cardPagerState = cardPagerState,
            detailsPagerState = detailsPagerState,
        )
    }
}

private class OrderCardTypePreviewProvider : CollectionPreviewParameterProvider<TangemPayOrderCardTypeUM>(
    collection = listOf(
        TangemPayOrderCardTypeUM.stub(feeState = TangemPayOrderCardTypeUM.FeeState.Default),
        TangemPayOrderCardTypeUM.stub(feeState = TangemPayOrderCardTypeUM.FeeState.FreeDelivery),
        TangemPayOrderCardTypeUM.stub(feeState = TangemPayOrderCardTypeUM.FeeState.InsufficientFunds),
        TangemPayOrderCardTypeUM.stub(isPlasticAvailable = false),
        TangemPayOrderCardTypeUM.stub(isLoading = true, isPlasticAvailable = false, issueFee = ""),
        TangemPayOrderCardTypeUM.stub(isError = true),
    ),
)