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
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.approval.impl.model.GiveApprovalUM
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
@Suppress("LongParameterList")
internal fun GiveApprovalContent(
    currency: String,
    uiState: GiveApprovalUM,
    amountFooter: TextReference,
    feeFooter: TextReference,
    onChangeApproveType: (ApproveType) -> Unit,
    onApproveClick: () -> Unit,
    onOpenLearnMoreAboutApproveClick: () -> Unit,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = amountFooter.resolveReference(),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing24),
        )

        SpacerH16()

        ApprovalInfo(
            currency = currency,
            approveType = uiState.approveType,
            approveItems = uiState.approveItems,
            onChangeApproveType = onChangeApproveType,
            onOpenLearnMoreAboutApproveClick = onOpenLearnMoreAboutApproveClick,
            feeSelectorBlockComponent = feeSelectorBlockComponent,
            feeFooter = feeFooter,
            isResetApproval = uiState.isResetApproval,
        )

        SpacerH(height = TangemTheme.dimens.spacing20)

        if (uiState.isHoldToConfirm) {
            HoldToConfirmButton(
                text = stringResourceSafe(id = R.string.common_approve),
                enabled = uiState.isApproveButtonEnabled,
                isLoading = uiState.isApproveLoading,
                onConfirm = onApproveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TangemTheme.dimens.spacing16),
            )
        } else {
            PrimaryButtonIconEnd(
                text = stringResourceSafe(id = R.string.common_approve),
                iconResId = uiState.walletInteractionIcon,
                showProgress = uiState.isApproveLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TangemTheme.dimens.spacing16),
                onClick = onApproveClick,
                enabled = uiState.isApproveButtonEnabled,
            )
        }

        SpacerH16()
    }
}

@Suppress("LongParameterList")
@Composable
private fun ApprovalInfo(
    currency: String,
    approveType: ApproveType,
    isResetApproval: Boolean,
    approveItems: ImmutableList<ApproveType>,
    onChangeApproveType: (ApproveType) -> Unit,
    onOpenLearnMoreAboutApproveClick: () -> Unit,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
    feeFooter: TextReference,
) {
    FooterContainer(
        footer = annotatedReference {
            append(stringResourceSafe(R.string.swap_approve_description))
            append(" ")
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "APPROVE_TAG",
                    linkInteractionListener = { onOpenLearnMoreAboutApproveClick() },
                ),
                block = {
                    appendColored(
                        text = stringResourceSafe(R.string.common_learn_more),
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
        footer = if (isResetApproval) {
            resourceReference(R.string.update_approval_permission_fee_note)
        } else {
            feeFooter
        },
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
                text = stringResourceSafe(id = R.string.give_permission_rows_amount, currency),
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
                painter = rememberVectorPainter(ImageVector.vectorResource(id = R.drawable.ic_chevron_24)),
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
                                        id = R.string.give_permission_current_transaction,
                                    )
                                    ApproveType.UNLIMITED -> stringResourceSafe(
                                        id = R.string.give_permission_unlimited,
                                    )
                                },
                                color = TangemTheme.colors.text.primary1,
                                style = TangemTheme.typography.body1,
                                maxLines = 1,
                            )
                            SpacerWMax()
                            Icon(
                                painter = rememberVectorPainter(
                                    image = ImageVector.vectorResource(id = R.drawable.ic_check_24),
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
            amountFooter = params.amountFooter,
            feeFooter = params.feeFooter,
            uiState = params.uiState,
            onChangeApproveType = {},
            onApproveClick = {},
            onOpenLearnMoreAboutApproveClick = {},
            feeSelectorBlockComponent = PreviewFeeSelectorBlockComponent(),
        )
    }
}

private data class GiveApprovalPreviewParams(
    val currency: String,
    val amountFooter: TextReference,
    val feeFooter: TextReference,
    val uiState: GiveApprovalUM,
)

private class GiveApprovalContentPreviewProvider : PreviewParameterProvider<GiveApprovalPreviewParams> {
    override val values: Sequence<GiveApprovalPreviewParams>
        get() = sequenceOf(
            GiveApprovalPreviewParams(
                currency = "USDT",
                amountFooter = stringReference("Allow this app to access your USDT"),
                feeFooter = stringReference("The network will charge a token approval fee"),
                uiState = GiveApprovalUM(
                    approveType = ApproveType.LIMITED,
                    approveItems = persistentListOf(ApproveType.LIMITED, ApproveType.UNLIMITED),
                    walletInteractionIcon = R.drawable.ic_tangem_24,
                    isApproveButtonEnabled = true,
                    isApproveLoading = false,
                ),
            ),
            GiveApprovalPreviewParams(
                currency = "USDC",
                amountFooter = stringReference("Allow this app to access your USDC"),
                feeFooter = stringReference("The network will charge a token approval fee"),
                uiState = GiveApprovalUM(
                    approveType = ApproveType.UNLIMITED,
                    approveItems = persistentListOf(ApproveType.LIMITED, ApproveType.UNLIMITED),
                    walletInteractionIcon = R.drawable.ic_tangem_24,
                    isApproveButtonEnabled = false,
                    isApproveLoading = true,
                ),
            ),
            GiveApprovalPreviewParams(
                currency = "USDC",
                amountFooter = stringReference("Allow this app to access your USDC"),
                feeFooter = stringReference("The network will charge a token approval fee"),
                uiState = GiveApprovalUM(
                    approveType = ApproveType.UNLIMITED,
                    approveItems = persistentListOf(ApproveType.LIMITED, ApproveType.UNLIMITED),
                    walletInteractionIcon = R.drawable.ic_tangem_24,
                    isApproveButtonEnabled = true,
                    isApproveLoading = false,
                    isResetApproval = true,
                ),
            ),
        )
}
// endregion