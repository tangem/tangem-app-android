package com.tangem.features.approval.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.PopupProperties
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.common.ui.R as CommonUiR

@Composable
@Suppress("LongParameterList")
internal fun GiveApprovalContent(
    currency: String,
    subtitle: TextReference,
    approveType: ApproveType,
    approveItems: ImmutableList<ApproveType>,
    onChangeApproveType: (ApproveType) -> Unit,
    walletInteractionIcon: Int?,
    isApproveEnabled: Boolean,
    isApproveLoading: Boolean,
    onApproveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onOpenLearnMoreAboutApproveClick: () -> Unit,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = subtitle.resolveReference(),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing24),
        )

        SpacerH16()

        ApprovalInfo(
            currency = currency,
            approveType = approveType,
            approveItems = approveItems,
            onChangeApproveType = onChangeApproveType,
            onOpenLearnMoreAboutApproveClick = onOpenLearnMoreAboutApproveClick,
            feeSelectorBlockComponent = feeSelectorBlockComponent,
        )

        SpacerH(height = TangemTheme.dimens.spacing20)

        PrimaryButtonIconEnd(
            text = stringResourceSafe(id = CommonUiR.string.common_approve),
            iconResId = walletInteractionIcon,
            showProgress = isApproveLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing16),
            onClick = onApproveClick,
            enabled = isApproveEnabled,
        )

        SpacerH12()

        SecondaryButton(
            text = stringResourceSafe(id = CommonUiR.string.common_cancel),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing16),
            onClick = onCancelClick,
        )

        SpacerH16()
    }
}

@Suppress("LongParameterList")
@Composable
private fun ApprovalInfo(
    currency: String,
    approveType: ApproveType,
    approveItems: ImmutableList<ApproveType>,
    onChangeApproveType: (ApproveType) -> Unit,
    onOpenLearnMoreAboutApproveClick: () -> Unit,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
) {
    FooterContainer(
        footer = annotatedReference {
            append(stringResourceSafe(CommonUiR.string.swap_approve_description))
            append(" ")
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "APPROVE_TAG",
                    linkInteractionListener = { onOpenLearnMoreAboutApproveClick() },
                ),
                block = {
                    appendColored(
                        text = stringResourceSafe(CommonUiR.string.common_learn_more),
                        color = TangemTheme.colors.text.accent,
                    )
                },
            )
        },
        modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        AmountItem(
            currency = currency,
            approveType = approveType,
            onChangeApproveType = onChangeApproveType,
            approveItems = approveItems,
        )
    }
    SpacerH16()
    FooterContainer(
        footer = resourceReference(CommonUiR.string.give_permission_policy_type_footer),
        modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        feeSelectorBlockComponent.Content(
            modifier = Modifier
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
        )
    }
}

@Composable
private fun AmountItem(
    currency: String,
    approveType: ApproveType,
    approveItems: ImmutableList<ApproveType>,
    onChangeApproveType: (ApproveType) -> Unit,
) {
    var isExpandSelector by remember { mutableStateOf(false) }
    var amountSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { isExpandSelector = true },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { amountSize = it }
                .padding(
                    top = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing16,
                    start = TangemTheme.dimens.spacing12,
                    end = TangemTheme.dimens.spacing12,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResourceSafe(id = CommonUiR.string.give_permission_rows_amount, currency),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
                maxLines = 1,
            )
            SpacerWMax()
            Text(
                text = approveType.text.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body1,
                maxLines = 1,
            )
            Icon(
                painter = rememberVectorPainter(ImageVector.vectorResource(id = CommonUiR.drawable.ic_chevron_24)),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing2),
            )
        }
        DropdownSelector(
            isExpanded = isExpandSelector,
            onDismiss = { isExpandSelector = false },
            onItemClick = { type ->
                isExpandSelector = false
                onChangeApproveType(type)
            },
            items = approveItems,
            selectedType = approveType,
            amountSize = amountSize,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun DropdownSelector(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    onItemClick: (ApproveType) -> Unit,
    items: ImmutableList<ApproveType>,
    selectedType: ApproveType,
    amountSize: IntSize,
) {
    var dropDownWidth by remember { mutableStateOf(IntSize.Zero) }
    val offsetY = amountSize.height.times(-1)
    val offsetX = amountSize.width - dropDownWidth.width

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(surface = TangemTheme.colors.background.action),
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(TangemTheme.dimens.radius16)),
    ) {
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onDismiss,
            properties = PopupProperties(clippingEnabled = false),
            offset = with(LocalDensity.current) {
                DpOffset(x = offsetX.toDp(), y = offsetY.toDp())
            },
            modifier = Modifier
                .wrapContentSize()
                .background(TangemTheme.colors.background.action)
                .onSizeChanged { dropDownWidth = it },
        ) {
            items.forEach { item ->
                val color = if (item == selectedType) TangemTheme.colors.icon.accent else Color.Transparent

                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    text = {
                        Row {
                            Text(
                                text = when (item) {
                                    ApproveType.LIMITED -> stringResourceSafe(
                                        id = CommonUiR.string.give_permission_current_transaction,
                                    )
                                    ApproveType.UNLIMITED -> stringResourceSafe(
                                        id = CommonUiR.string.give_permission_unlimited,
                                    )
                                },
                                color = TangemTheme.colors.text.primary1,
                                style = TangemTheme.typography.body1,
                                maxLines = 1,
                            )
                            SpacerWMax()
                            Icon(
                                painter = rememberVectorPainter(
                                    image = ImageVector.vectorResource(id = CommonUiR.drawable.ic_check_24),
                                ),
                                tint = color,
                                contentDescription = null,
                                modifier = Modifier.padding(start = TangemTheme.dimens.size20),
                            )
                        }
                    },
                    onClick = {
                        onItemClick.invoke(item)
                    },
                )
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun GiveApprovalContentPreview(
    @PreviewParameter(GiveApprovalContentPreviewProvider::class) params: GiveApprovalPreviewParams,
) {
    TangemThemePreview {
        GiveApprovalContent(
            currency = params.currency,
            subtitle = params.subtitle,
            approveType = params.approveType,
            approveItems = params.approveItems,
            onChangeApproveType = {},
            walletInteractionIcon = params.walletInteractionIcon,
            isApproveEnabled = params.isApproveEnabled,
            isApproveLoading = params.isApproveLoading,
            onApproveClick = {},
            onCancelClick = {},
            onOpenLearnMoreAboutApproveClick = {},
            feeSelectorBlockComponent = PreviewFeeSelectorBlockComponent(),
        )
    }
}

private data class GiveApprovalPreviewParams(
    val currency: String,
    val subtitle: TextReference,
    val approveType: ApproveType,
    val approveItems: ImmutableList<ApproveType>,
    val walletInteractionIcon: Int?,
    val isApproveEnabled: Boolean,
    val isApproveLoading: Boolean,
)

private class GiveApprovalContentPreviewProvider : PreviewParameterProvider<GiveApprovalPreviewParams> {
    override val values: Sequence<GiveApprovalPreviewParams>
        get() = sequenceOf(
            GiveApprovalPreviewParams(
                currency = "USDT",
                subtitle = stringReference("Allow this app to access your USDT"),
                approveType = ApproveType.LIMITED,
                approveItems = persistentListOf(ApproveType.LIMITED, ApproveType.UNLIMITED),
                walletInteractionIcon = CommonUiR.drawable.ic_tangem_24,
                isApproveEnabled = true,
                isApproveLoading = false,
            ),
            GiveApprovalPreviewParams(
                currency = "USDC",
                subtitle = stringReference("Allow this app to access your USDC"),
                approveType = ApproveType.UNLIMITED,
                approveItems = persistentListOf(ApproveType.LIMITED, ApproveType.UNLIMITED),
                walletInteractionIcon = CommonUiR.drawable.ic_tangem_24,
                isApproveEnabled = false,
                isApproveLoading = true,
            ),
        )
}
// endregion