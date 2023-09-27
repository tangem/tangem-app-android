package com.tangem.core.ui.components.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.tangem.common.Strings
import com.tangem.core.ui.R
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TransactionState.Content.Status
import com.tangem.core.ui.components.transactions.state.TransactionState.Content.Direction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
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
 * @author Andrew Khokhlov on 16/06/2023
 */
@Composable
fun Transaction(
    state: TransactionState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .defaultMinSize(minHeight = TangemTheme.dimens.size56)
            .padding(horizontal = TangemTheme.dimens.spacing12, vertical = TangemTheme.dimens.spacing10),
        color = TangemTheme.colors.background.primary,
    ) {
        @Suppress("DestructuringDeclarationWithTooManyEntries")
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
                    .padding(horizontal = TangemTheme.dimens.spacing12)
                    .constrainAs(titleItem) {
                        start.linkTo(iconItem.end)
                        top.linkTo(iconItem.top)
                    },
            )

            Subtitle(
                state = state,
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing6)
                    .padding(horizontal = TangemTheme.dimens.spacing12)
                    .constrainAs(subtitleItem) {
                        start.linkTo(iconItem.end)
                        top.linkTo(amountItem.bottom)
                        end.linkTo(timestampItem.start)
                        width = Dimension.fillToConstraints
                    },
            )

            Amount(
                state = state,
                modifier = Modifier.constrainAs(amountItem) {
                    start.linkTo(titleItem.end)
                    top.linkTo(titleItem.top)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
                isBalanceHidden = isBalanceHidden
            )

            Timestamp(
                state = state,
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing6)
                    .constrainAs(timestampItem) {
                        top.linkTo(amountItem.bottom)
                        end.linkTo(parent.end)
                    },
            )
        }
    }
}

@Composable
private fun Icon(state: TransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TransactionState.Content -> {
            Box(
                modifier = modifier
                    .size(TangemTheme.dimens.size40)
                    .background(
                        color = when (state.status) {
                            is Status.Unconfirmed -> {
                                TangemTheme.colors.icon.attention.copy(alpha = 0.1f)
                            }
                            is Status.Confirmed, Status.Failed -> {
                                TangemTheme.colors.background.secondary
                            }
                        },
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    painter = painterResource(
                        id = when (state) {
                            is TransactionState.Transfer -> {
                                when (state.direction) {
                                    Direction.OUTGOING -> R.drawable.ic_arrow_up_24
                                    Direction.INCOMING -> R.drawable.ic_arrow_down_24
                                }
                            }
                            is TransactionState.Approve -> R.drawable.ic_doc_24
                            is TransactionState.Swap -> R.drawable.ic_exchange_vertical_24
                            is TransactionState.Custom -> R.drawable.ic_exchange_vertical_24
                        },
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(TangemTheme.dimens.size20)
                        .align(Alignment.Center),
                    tint = when (state.status) {
                        Status.Confirmed, Status.Failed -> TangemTheme.colors.icon.informative
                        Status.Unconfirmed -> TangemTheme.colors.icon.attention
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
            Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6)) {
                Text(
                    text = when (state) {
                        is TransactionState.Approve -> stringResource(R.string.common_approval)
                        is TransactionState.Transfer -> stringResource(R.string.common_transfer)
                        is TransactionState.Swap -> stringResource(R.string.common_swap)
                        is TransactionState.Custom -> state.title.resolveReference()
                    },
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle2,
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
                text = when (state) {
                    is TransactionState.Transfer -> {
                        when (state.direction) {
                            Direction.OUTGOING -> stringResource(
                                id = R.string.transaction_history_transaction_to_address,
                                state.address.resolveReference(),
                            )
                            Direction.INCOMING -> stringResource(
                                id = R.string.transaction_history_transaction_from_address,
                                state.address.resolveReference(),
                            )
                        }
                    }
                    is TransactionState.Approve,
                    -> stringResource(
                        id = R.string.transaction_history_transaction_from_address,
                        state.address.resolveReference(),
                    )
                    is TransactionState.Swap -> stringResource(
                        id = R.string.transaction_history_contract_address,
                        state.address.resolveReference(),
                    )
                    is TransactionState.Custom -> stringResource(
                        id = when (state.direction) {
                            Direction.OUTGOING -> R.string.transaction_history_transaction_to_address
                            Direction.INCOMING -> R.string.transaction_history_transaction_from_address
                        },
                        state.subtitle.resolveReference(),
                    )
                },
                modifier = modifier,
                textAlign = TextAlign.Start,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption,
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
private fun Amount(state: TransactionState, modifier: Modifier = Modifier, isBalanceHidden: Boolean) {
    when (state) {
        is TransactionState.Content -> {
            Text(
                text = if (isBalanceHidden) Strings.STARS else state.amount,
                modifier = modifier,
                textAlign = TextAlign.End,
                color = when (state.direction) {
                    Direction.INCOMING -> TangemTheme.colors.text.accent
                    Direction.OUTGOING -> TangemTheme.colors.text.primary1
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
                text = state.timestamp,
                modifier = modifier,
                textAlign = TextAlign.End,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption,
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

@Preview
@Composable
private fun Preview_TransactionItem_LightTheme(
    @PreviewParameter(TransactionItemStateProvider::class) state: TransactionState,
) {
    TangemTheme(isDark = false) {
        Transaction(state = state, isBalanceHidden = false)
    }
}

@Preview
@Composable
private fun Preview_TransactionItem_DarkTheme(
    @PreviewParameter(TransactionItemStateProvider::class) state: TransactionState,
) {
    TangemTheme(isDark = true) {
        Transaction(state = state, isBalanceHidden = false)
    }
}

private class TransactionItemStateProvider : CollectionPreviewParameterProvider<TransactionState>(
    collection = listOf(
        TransactionState.Transfer(
            txHash = UUID.randomUUID().toString(),
            address = TextReference.Str("33BddS...ga2B"),
            amount = "-0.500913 BTC",
            timestamp = "8:41",
            status = Status.Confirmed,
            direction = Direction.OUTGOING,
        ),
        TransactionState.Transfer(
            txHash = UUID.randomUUID().toString(),
            address = TextReference.Str("33BddS...ga2B"),
            amount = "+0.500913 BTC",
            timestamp = "8:41",
            status = Status.Unconfirmed,
            direction = Direction.INCOMING,
        ),
        TransactionState.Approve(
            txHash = UUID.randomUUID().toString(),
            address = TextReference.Str("33BddS...ga2B"),
            amount = "+0.500913 BTC",
            timestamp = "8:41",
            status = Status.Failed,
            direction = Direction.OUTGOING,
        ),
        TransactionState.Swap(
            txHash = UUID.randomUUID().toString(),
            address = TextReference.Str("33BddS...ga2B"),
            amount = "+0.500913 BTC",
            timestamp = "8:41",
            status = Status.Unconfirmed,
            direction = Direction.INCOMING,
        ),
        TransactionState.Custom(
            txHash = UUID.randomUUID().toString(),
            address = TextReference.Str("33BddS...ga2B"),
            amount = "+0.500913 BTC",
            timestamp = "8:41",
            status = Status.Confirmed,
            direction = Direction.INCOMING,
            title = TextReference.Str("Submit"),
            subtitle = TextReference.Str("33BddS...ga2B"),
        ),
        TransactionState.Custom(
            txHash = UUID.randomUUID().toString(),
            address = TextReference.Str("33BddS...ga2B"),
            amount = "+0.500913 BTC",
            timestamp = "8:41",
            status = Status.Confirmed,
            direction = Direction.OUTGOING,
            title = TextReference.Str("Submit"),
            subtitle = TextReference.Str("33BddS...ga2B"),
        ),
        TransactionState.Loading(txHash = UUID.randomUUID().toString()),
    ),
)
