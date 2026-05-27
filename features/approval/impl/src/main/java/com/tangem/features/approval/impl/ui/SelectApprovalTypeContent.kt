package com.tangem.features.approval.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH18
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.approval.impl.model.SelectApprovalTypeUM
import kotlinx.collections.immutable.persistentListOf

/**
 * UI for the selection-only approval variant. Reuses [ApprovalTypeSelectorRow] for the
 * approval-type picker. The primary button calls [onConfirmClick] which is wired to a
 * callback that returns the chosen [ApproveType]
 * to the caller (instead of submitting an on-chain transaction).
 */
@Composable
@Suppress("LongParameterList")
internal fun SelectApprovalTypeContent(
    currency: String,
    uiState: SelectApprovalTypeUM,
    onChangeApproveType: (ApproveType) -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = uiState.subtitle.resolveAnnotatedReference(),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                top = 2.dp,
                start = 16.dp,
                end = 16.dp,
            ),
        )

        SpacerH18()

        ApprovalTypeSelectorRow(
            currency = currency,
            approveType = uiState.approveType,
            approveItems = uiState.approveItems,
            onChangeApproveType = onChangeApproveType,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
        )

        SpacerH(height = TangemTheme.dimens.spacing20)

        PrimaryButton(
            text = stringResourceSafe(id = R.string.common_continue),
            onClick = onConfirmClick,
            enabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing16),
        )

        SpacerH16()
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SelectApprovalTypeContentPreview(
    @PreviewParameter(SelectApprovalTypeContentPreviewProvider::class) params: SelectApprovalTypePreviewParams,
) {
    TangemThemePreview {
        SelectApprovalTypeContent(
            currency = params.currency,
            uiState = params.uiState,
            onChangeApproveType = {},
            onConfirmClick = {},
        )
    }
}

private data class SelectApprovalTypePreviewParams(
    val currency: String,
    val uiState: SelectApprovalTypeUM,
)

private class SelectApprovalTypeContentPreviewProvider : PreviewParameterProvider<SelectApprovalTypePreviewParams> {
    override val values: Sequence<SelectApprovalTypePreviewParams>
        get() = sequenceOf(
            SelectApprovalTypePreviewParams(
                currency = "USDT",
                uiState = SelectApprovalTypeUM(
                    subtitle = combinedReference(
                        resourceReference(
                            id = R.string.give_permission_swap_subtitle_v2,
                            // Arg is only used in iOS
                            formatArgs = wrappedList(""),
                        ),
                        styledResourceReference(
                            id = R.string.common_learn_more,
                            spanStyleReference = {
                                TangemTheme.typography.caption2
                                    .copy(color = TangemTheme.colors.text.accent)
                                    .toSpanStyle()
                            },
                            onClick = { },
                        ),
                    ),
                    approveType = ApproveType.LIMITED,
                    approveItems = persistentListOf(ApproveType.LIMITED, ApproveType.UNLIMITED),
                ),
            ),
            SelectApprovalTypePreviewParams(
                currency = "USDC",
                uiState = SelectApprovalTypeUM(
                    subtitle = combinedReference(
                        resourceReference(
                            id = com.tangem.common.ui.R.string.give_permission_swap_subtitle_v2,
                            // Arg is only used in iOS
                            formatArgs = wrappedList(""),
                        ),
                        styledResourceReference(
                            id = com.tangem.common.ui.R.string.common_learn_more,
                            spanStyleReference = {
                                TangemTheme.typography.caption2
                                    .copy(color = TangemTheme.colors.text.accent)
                                    .toSpanStyle()
                            },
                            onClick = {},
                        ),
                    ),
                    approveType = ApproveType.UNLIMITED,
                    approveItems = persistentListOf(ApproveType.LIMITED, ApproveType.UNLIMITED),
                ),
            ),
        )
}
// endregion