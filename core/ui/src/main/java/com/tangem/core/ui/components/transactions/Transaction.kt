package com.tangem.core.ui.components.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.tangem.core.ui.R
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.res.TangemTheme
import java.util.UUID

/**
 * Transaction component
 *
 * @param state    state
 * @param modifier modifier
 *
 * @see <a href = "https://www.figma.com/file/RU7AIgwHtGdMfy83T5UOoR/iOS?type=design&node-id=446-438&t=71jPDxMMk4e0
 * a025-4">Figma Component</a>
 *
[REDACTED_AUTHOR]
 */
@Composable
fun Transaction(state: TransactionState, modifier: Modifier = Modifier) {
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
                        color = when (state) {
                            is TransactionState.ProcessedTransactionContent -> {
                                TangemTheme.colors.icon.attention.copy(alpha = 0.1f)
                            }
                            is TransactionState.CompletedTransactionContent -> {
                                TangemTheme.colors.background.secondary
                            }
                        },
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    painter = painterResource(
                        id = when (state) {
                            is TransactionState.Sending,
                            is TransactionState.Send,
                            -> R.drawable.ic_arrow_up_24

                            is TransactionState.Receiving,
                            is TransactionState.Receive,
                            -> R.drawable.ic_arrow_down_24

                            is TransactionState.Approving,
                            is TransactionState.Approved,
                            -> R.drawable.ic_doc_24

                            is TransactionState.Swapping,
                            is TransactionState.Swapped,
                            -> R.drawable.ic_exchange_vertical_24
                        },
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(TangemTheme.dimens.size20)
                        .align(Alignment.Center),
                    tint = when (state) {
                        is TransactionState.ProcessedTransactionContent -> TangemTheme.colors.icon.attention
                        is TransactionState.CompletedTransactionContent -> TangemTheme.colors.icon.informative
                    },
                )
            }
        }
        is TransactionState.Loading -> {
            CircleShimmer(modifier = modifier.size(TangemTheme.dimens.size40))
        }
    }
}

@Composable
private fun Title(state: TransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TransactionState.ProcessedTransactionContent -> {
            Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6)) {
                Text(
                    text = stringResource(
                        id = when (state) {
                            is TransactionState.Sending -> R.string.common_transfer
                            is TransactionState.Receiving -> R.string.common_transfer
                            is TransactionState.Approving -> R.string.common_approval
                            is TransactionState.Swapping -> R.string.common_swap
                        },
                    ),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle2,
                )

                Image(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    painter = painterResource(id = R.drawable.img_loader_15),
                    contentDescription = null,
                )
            }
        }
        is TransactionState.CompletedTransactionContent -> {
            Text(
                text = stringResource(
                    id = when (state) {
                        is TransactionState.Send -> R.string.common_transfer
                        is TransactionState.Receive -> R.string.common_transfer
                        is TransactionState.Approved -> R.string.common_approval
                        is TransactionState.Swapped -> R.string.common_swap
                    },
                ),
                modifier = modifier,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle2,
            )
        }
        is TransactionState.Loading -> {
            RectangleShimmer(
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
                    is TransactionState.Sending,
                    is TransactionState.Send,
                    -> stringResource(
                        id = R.string.transaction_history_transaction_to_address,
                        state.address,
                    )
                    is TransactionState.Receiving,
                    is TransactionState.Receive,
                    is TransactionState.Approving,
                    is TransactionState.Approved,
                    -> stringResource(
                        id = R.string.transaction_history_transaction_from_address,
                        state.address,
                    )
                    is TransactionState.Swapping,
                    is TransactionState.Swapped,
                    -> stringResource(
                        id = R.string.transaction_history_contract_address,
                        state.address,
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
    }
}

@Composable
private fun Amount(state: TransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TransactionState.Content -> {
            Text(
                text = state.amount,
                modifier = modifier,
                textAlign = TextAlign.End,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.body2,
            )
        }
        is TransactionState.Loading -> {
            RectangleShimmer(
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
    }
}

@Preview
@Composable
private fun Preview_TransactionItem_LightTheme(
    @PreviewParameter(TransactionItemStateProvider::class) state: TransactionState,
) {
    TangemTheme(isDark = false) {
        Transaction(state)
    }
}

@Preview
@Composable
private fun Preview_TransactionItem_DarkTheme(
    @PreviewParameter(TransactionItemStateProvider::class) state: TransactionState,
) {
    TangemTheme(isDark = true) {
        Transaction(state)
    }
}

private class TransactionItemStateProvider : CollectionPreviewParameterProvider<TransactionState>(
    collection = listOf(
        TransactionState.Sending(
            txHash = UUID.randomUUID().toString(),
            address = "33BddS...ga2B",
            amount = "-0.500913 BTC",
            timestamp = "8:41",
        ),
        TransactionState.Receiving(
            txHash = UUID.randomUUID().toString(),
            address = "33BddS...ga2B",
            amount = "+0.500913 BTC",
            timestamp = "8:41",
        ),
        TransactionState.Approving(
            txHash = UUID.randomUUID().toString(),
            address = "33BddS...ga2B",
            amount = "+0.500913 BTC",
            timestamp = "8:41",
        ),
        TransactionState.Swapping(
            txHash = UUID.randomUUID().toString(),
            address = "33BddS...ga2B",
            amount = "+0.500913 BTC",
            timestamp = "8:41",
        ),
        TransactionState.Send(
            txHash = UUID.randomUUID().toString(),
            address = "33BddS...ga2B",
            amount = "-0.500913 BTC",
            timestamp = "8:41",
        ),
        TransactionState.Receive(
            txHash = UUID.randomUUID().toString(),
            address = "33BddS...ga2B",
            amount = "+0.500913 BTC",
            timestamp = "8:41",
        ),
        TransactionState.Approved(
            txHash = UUID.randomUUID().toString(),
            address = "33BddS...ga2B",
            amount = "+0.500913 BTC",
            timestamp = "8:41",
        ),
        TransactionState.Swapped(
            txHash = UUID.randomUUID().toString(),
            address = "33BddS...ga2B",
            amount = "+0.500913 BTC",
            timestamp = "8:41",
        ),
        TransactionState.Loading(txHash = UUID.randomUUID().toString()),
    ),
)