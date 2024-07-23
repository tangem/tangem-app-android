package com.tangem.core.ui.components.transactions

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.tangem.core.ui.R
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TransactionState.Content.Direction
import com.tangem.core.ui.components.transactions.state.TransactionState.Content.Status
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.utils.StringsSigns
import java.util.UUID

/**
 * Transaction component
 *
 * @param state            state
 * @param isBalanceHidden  is balance hidden
 * @param modifier         modifier
 *
 * @see <a href = "https://www.figma.com/file/RU7AIgwHtGdMfy83T5UOoR/iOS?type=design&node-id=446-438&t=71jPDxMMk4e0
 * a025-4">Figma Component</a>
 *
[REDACTED_AUTHOR]
 */
@Composable
@Suppress("LongMethod")
fun Transaction(state: TransactionState, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .clickable(
                enabled = state is TransactionState.Content,
                onClick = (state as? TransactionState.Content)?.onClick ?: {},
            )
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size56)
            .padding(
                vertical = TangemTheme.dimens.spacing8,
                horizontal = TangemTheme.dimens.spacing12,
            ),
        color = TangemTheme.colors.background.primary,
    ) {
        @Suppress("DestructuringDeclarationWithTooManyEntries")
        // FIXME: split this composable to small composables(loading/content/error) to properly handle onClick
        ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
            val (iconItem, titleItem, subtitleItem, amountItem, timestampItem) = createRefs()

            Icon(
                state = state,
                modifier = Modifier
                    .size(TangemTheme.dimens.size40)
                    .constrainAs(iconItem) {
                        start.linkTo(parent.start)
                        centerVerticallyTo(parent)
                    },
            )

            Title(
                state = state,
                modifier = Modifier
                    .padding(
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing4,
                    )
                    .constrainAs(titleItem) {
                        top.linkTo(parent.top)
                        bottom.linkTo(subtitleItem.top)
                        start.linkTo(iconItem.end)
                        end.linkTo(amountItem.start)
                        width = Dimension.fillToConstraints
                    },
            )

            Subtitle(
                state = state,
                modifier = Modifier
                    .padding(
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing4,
                    )
                    .constrainAs(subtitleItem) {
                        top.linkTo(titleItem.bottom)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(iconItem.end)
                        end.linkTo(timestampItem.start)
                        width = Dimension.fillToConstraints
                    },
            )

            Amount(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.constrainAs(amountItem) {
                    top.linkTo(parent.top)
                    bottom.linkTo(timestampItem.top)
                    start.linkTo(titleItem.end)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            )

            Timestamp(
                state = state,
                modifier = Modifier.constrainAs(timestampItem) {
                    top.linkTo(amountItem.bottom)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                },
            )
        }
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun Icon(state: TransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TransactionState.Content -> {
            Box(
                modifier = modifier
                    .size(TangemTheme.dimens.size40)
                    .background(
                        color = when (state.status) {
                            is Status.Unconfirmed -> TangemTheme.colors.icon.attention.copy(alpha = 0.1f)
                            is Status.Confirmed -> TangemTheme.colors.background.secondary
                            is Status.Failed -> TangemTheme.colors.icon.warning.copy(alpha = 0.1f)
                        },
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    painter = painterResource(id = state.iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(TangemTheme.dimens.size20)
                        .align(Alignment.Center),
                    tint = when (state.status) {
                        Status.Confirmed -> TangemTheme.colors.icon.informative
                        Status.Unconfirmed -> TangemTheme.colors.icon.attention
                        Status.Failed -> TangemTheme.colors.icon.warning
                    },
                )
            }
        }
        is TransactionState.Loading -> {
            CircleShimmer(modifier = modifier.size(TangemTheme.dimens.size40))
        }
        is TransactionState.Locked -> {
            Box(modifier = modifier.size(TangemTheme.dimens.size40)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color = TangemTheme.colors.button.secondary,
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun Title(state: TransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TransactionState.Content -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6),
            ) {
                Text(
                    text = state.title.resolveReference(),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle2,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )

                if (state.status is Status.Unconfirmed) {
                    Image(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        painter = painterResource(id = R.drawable.img_loader_15),
                        contentDescription = null,
                    )
                }
            }
        }
        is TransactionState.Loading -> {
            RectangleShimmer(
                modifier = modifier.size(width = TangemTheme.dimens.size70, height = TangemTheme.dimens.size12),
            )
        }
        is TransactionState.Locked -> {
            LockedContent(
                modifier = modifier.size(width = TangemTheme.dimens.size70, height = TangemTheme.dimens.size12),
            )
        }
    }
}

@Composable
private fun Subtitle(state: TransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TransactionState.Content -> {
            Text(
                text = when (state.status) {
                    is Status.Failed -> stringResource(id = R.string.common_transaction_failed)
                    else -> state.subtitle.resolveReference()
                },
                modifier = modifier,
                textAlign = TextAlign.Start,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
            )
        }
        is TransactionState.Loading -> {
            RectangleShimmer(
                modifier = modifier.size(width = TangemTheme.dimens.size52, height = TangemTheme.dimens.size12),
            )
        }
        is TransactionState.Locked -> {
            LockedContent(
                modifier = modifier.size(width = TangemTheme.dimens.size52, height = TangemTheme.dimens.size12),
            )
        }
    }
}

@Composable
private fun Amount(state: TransactionState, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TransactionState.Content -> {
            Text(
                text = if (isBalanceHidden) StringsSigns.STARS else state.amount,
                modifier = modifier,
                textAlign = TextAlign.End,
                color = when (state.status) {
                    Status.Failed -> TangemTheme.colors.text.warning
                    else -> when (state.direction) {
                        Direction.INCOMING -> TangemTheme.colors.text.accent
                        Direction.OUTGOING -> TangemTheme.colors.text.primary1
                    }
                },
                style = TangemTheme.typography.body2,
            )
        }
        is TransactionState.Loading -> {
            RectangleShimmer(
                modifier = modifier.size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12),
            )
        }
        is TransactionState.Locked -> {
            LockedContent(
                modifier = modifier.size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12),
            )
        }
    }
}

@Composable
private fun Timestamp(state: TransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TransactionState.Content -> {
            Text(
                text = state.time,
                modifier = modifier,
                textAlign = TextAlign.End,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
            )
        }
        is TransactionState.Loading -> {
            RectangleShimmer(
                modifier = modifier.size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12),
            )
        }
        is TransactionState.Locked -> {
            LockedContent(
                modifier = modifier.size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12),
            )
        }
    }
}

@Composable
private fun LockedContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.field.primary,
            shape = RoundedCornerShape(TangemTheme.dimens.radius6),
        ),
    )
}

@Preview(showBackground = true, widthDp = 368)
@Preview(showBackground = true, widthDp = 368, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionItem(@PreviewParameter(TransactionItemStateProvider::class) state: TransactionState) {
    TangemThemePreview {
        Transaction(state = state, isBalanceHidden = false, modifier = Modifier.padding(TangemTheme.dimens.spacing20))
    }
}

private class TransactionItemStateProvider : CollectionPreviewParameterProvider<TransactionState>(
    collection = listOf(
        TransactionState.Content(
            txHash = UUID.randomUUID().toString(),
            amount = "-0.500913 BTC",
            time = "8:41",
            status = Status.Confirmed,
            direction = Direction.OUTGOING,
            iconRes = R.drawable.ic_arrow_up_24,
            title = resourceReference(R.string.common_transfer),
            subtitle = TextReference.Str("33BddS...ga2B"),
            timestamp = 0L,
            onClick = {},
        ),
        TransactionState.Content(
            txHash = UUID.randomUUID().toString(),
            amount = "+0.500913 BTC",
            time = "8:41",
            status = Status.Unconfirmed,
            direction = Direction.INCOMING,
            iconRes = R.drawable.ic_arrow_down_24,
            title = resourceReference(R.string.common_transfer),
            subtitle = TextReference.Str("33BddS...ga2B"),
            timestamp = 0L,
            onClick = {},
        ),
        TransactionState.Content(
            txHash = UUID.randomUUID().toString(),
            amount = "+0.500913 BTC",
            time = "8:41",
            status = Status.Unconfirmed,
            direction = Direction.OUTGOING,
            iconRes = R.drawable.ic_doc_24,
            title = resourceReference(R.string.common_approval),
            subtitle = TextReference.Str("33BddS...ga2B"),
            timestamp = 0L,
            onClick = {},
        ),
        TransactionState.Content(
            txHash = UUID.randomUUID().toString(),
            amount = "+0.500913 BTC",
            time = "8:41",
            status = Status.Failed,
            direction = Direction.OUTGOING,
            iconRes = R.drawable.ic_doc_24,
            title = resourceReference(R.string.common_approval),
            subtitle = TextReference.Str("33BddS...ga2B"),
            timestamp = 0L,
            onClick = {},
        ),
        TransactionState.Content(
            txHash = UUID.randomUUID().toString(),
            amount = "+0.500913 BTC",
            time = "8:41",
            status = Status.Confirmed,
            direction = Direction.OUTGOING,
            iconRes = R.drawable.ic_doc_24,
            title = resourceReference(R.string.common_approval),
            subtitle = TextReference.Str("33BddS...ga2B"),
            timestamp = 0L,
            onClick = {},
        ),
        TransactionState.Content(
            txHash = UUID.randomUUID().toString(),
            amount = "+0.500913 BTC",
            time = "8:41",
            status = Status.Unconfirmed,
            direction = Direction.INCOMING,
            iconRes = R.drawable.ic_arrow_down_24,
            title = resourceReference(R.string.common_swap),
            subtitle = TextReference.Str("33BddS...ga2B"),
            timestamp = 0L,
            onClick = {},
        ),
        TransactionState.Content(
            txHash = UUID.randomUUID().toString(),
            amount = "+0.500913 BTC",
            time = "8:41",
            status = Status.Confirmed,
            direction = Direction.INCOMING,
            iconRes = R.drawable.ic_doc_24,
            title = TextReference.Str("Submit"),
            subtitle = TextReference.Str("33BddS...ga2B"),
            timestamp = 0L,
            onClick = {},
        ),
        TransactionState.Content(
            txHash = UUID.randomUUID().toString(),
            amount = "+0.500913 BTC",
            time = "8:41",
            status = Status.Confirmed,
            direction = Direction.OUTGOING,
            iconRes = R.drawable.ic_arrow_up_24,
            title = TextReference.Str("Submit"),
            subtitle = TextReference.Str("33BddS...ga2B"),
            timestamp = 0L,
            onClick = {},
        ),
        TransactionState.Content(
            txHash = UUID.randomUUID().toString(),
            amount = "0.625 USDT",
            time = "€0.50",
            status = Status.Confirmed,
            direction = Direction.OUTGOING,
            iconRes = R.drawable.ic_arrow_up_24,
            title = TextReference.Str("Unlimint Banking Cards Sandbox London"),
            subtitle = stringReference(value = "8:41 • authorized"),
            timestamp = 0L,
            onClick = {},
        ),
        TransactionState.Loading(txHash = UUID.randomUUID().toString()),
    ),
)