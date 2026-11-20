package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.decorations.RoundedItemGroupGlowRing
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.expandable.EXPANDABLE_ROW_REVEAL_DURATION_MILLIS
import com.tangem.core.ui.ds2.expandable.ExpandableRowRevealItem
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_usd_20
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayGroupUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockComponent
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.features.tangempay.component.TangemPayMainBlockComponent
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.moneyaccount.main.component.MoneyAccountMainBlockComponent
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM

internal const val ORGANIZE_TOKENS_BUTTON_ITEM_KEY = "OrganizeTokensButton"
private const val TANGEM_PAY_GROUP_CHILDREN_COUNT = 2
private val TANGEM_PAY_GROUP_RADIUS = 24.dp

internal fun LazyListScope.nftCollections2(state: WalletUM, itemModifier: Modifier) {
    (state as? WalletUM.Content)?.let { content ->
        item(key = "NFTCollections", contentType = "NFTCollections") {
            WalletNFTItem2(
                modifier = itemModifier,
                state = content.nftState,
            )
        }
    }
}

/**
 * @param onButtonBoundsChange called with the button root-coordinates bounds on every placement change
 * and with `null` when the button leaves composition
 */
internal fun LazyListScope.organizeTokens2(
    state: WalletUM,
    itemModifier: Modifier,
    onButtonBoundsChange: (Rect?) -> Unit,
) {
    val organizeButton = state.tokensListUM.organizeButtonUM
    if (organizeButton != null) {
        item(
            key = ORGANIZE_TOKENS_BUTTON_ITEM_KEY,
            contentType = ORGANIZE_TOKENS_BUTTON_ITEM_KEY,
        ) {
            val hapticManager = LocalHapticManager.current
            val testTag = if (organizeButton.text == resourceReference(R.string.main_add_and_manage_tokens)) {
                MainScreenTestTags.ADD_AND_MANAGE_BUTTON
            } else {
                MainScreenTestTags.ORGANIZE_TOKENS_BUTTON
            }

            val currentOnButtonBoundsChange by rememberUpdatedState(onButtonBoundsChange)
            DisposableEffect(Unit) {
                onDispose { currentOnButtonBoundsChange(null) }
            }

            TangemButton(
                modifier = itemModifier
                    .onGloballyPositioned { currentOnButtonBoundsChange(it.boundsInRoot()) }
                    .testTag(testTag),
                size = TangemButton.Size.X9,
                onClick = {
                    hapticManager.perform(TangemHapticEffect.View.ContextClick)
                    organizeButton.onClick()
                },
                variant = TangemButton.Variant.Secondary,
                iconStart = organizeButton.iconStart,
                text = organizeButton.text,
                isEnabled = organizeButton.isEnabled,
            )
        }
    }
}

internal fun LazyListScope.tangemPay(
    tangemPayComponent: TangemPayMainBlockComponent,
    tangemPayUM: TangemPayMainUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    with(tangemPayComponent) {
        tangemPayMainContent(modifier = modifier, state = tangemPayUM, isBalanceHidden = isBalanceHidden)
    }
}

internal fun LazyListScope.polymarketWalletBlock(
    polymarketComponent: PolymarketWalletBlockComponent,
    polymarketUM: PolymarketWalletBlockUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    with(polymarketComponent) {
        polymarketWalletBlockContent(modifier = modifier, state = polymarketUM, isBalanceHidden = isBalanceHidden)
    }
}

internal fun LazyListScope.moneyAccount(
    moneyAccountComponent: MoneyAccountMainBlockComponent,
    moneyAccountUM: MoneyAccountMainUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    with(moneyAccountComponent) {
        moneyAccountMainContent(modifier = modifier, state = moneyAccountUM, isBalanceHidden = isBalanceHidden)
    }
}

@Suppress("LongParameterList", "LongMethod")
internal fun LazyListScope.tangemPayGroup(
    groupUM: TangemPayGroupUM.Content,
    tangemPayComponent: TangemPayMainBlockComponent,
    tangemPayUM: TangemPayMainUM,
    moneyAccountComponent: MoneyAccountMainBlockComponent,
    moneyAccountUM: MoneyAccountMainUM,
    isBalanceHidden: Boolean,
    modifier: Modifier,
) {
    item(key = groupUM.id, contentType = TangemPayGroupUM.Content::class.java) {
        val effectiveLastIndex by animateIntAsState(
            targetValue = if (groupUM.isExpanded) TANGEM_PAY_GROUP_CHILDREN_COUNT else 0,
            animationSpec = if (groupUM.isExpanded) {
                snap()
            } else {
                snap(delayMillis = EXPANDABLE_ROW_REVEAL_DURATION_MILLIS)
            },
            label = "tangemPayGroupLastIndex",
        )
        val backgroundColor = tangemPayGroupBackgroundColor(groupUM.isActionRequired)
        TangemPayGroupSlice(
            groupUM = groupUM,
            currentIndex = 0,
            lastIndex = effectiveLastIndex,
            modifier = modifier
                .padding(top = 12.dp)
                .roundedShapeItemDecoration(
                    currentIndex = 0,
                    radius = TANGEM_PAY_GROUP_RADIUS,
                    addDefaultPadding = false,
                    lastIndex = effectiveLastIndex,
                    backgroundColor = backgroundColor,
                    groupKey = groupUM.id,
                ),
        ) {
            TangemAccountRow(
                avatar = TangemAvatar.UiState.Icon(
                    imageVector = Icons.ic_sign_usd_20,
                    appearance = TangemAvatar.Appearance.Neutral,
                    shape = TangemAvatar.Shape.RoundedSquare,
                ),
                title = groupUM.title,
                label = groupUM.label,
                balance = groupUM.balance,
                balanceBottom = {
                    if (groupUM.balance != null) {
                        Text(
                            text = groupUM.balanceCaption.resolveReference(),
                            style = TangemTheme.typography3.caption.medium,
                            color = TangemTheme.colors3.text.secondary,
                            maxLines = 1,
                        )
                    }
                },
                isBalanceHidden = isBalanceHidden,
                isBalanceFlickering = groupUM.isBalanceFlickering,
                showCompactBalance = false,
                isExpanded = groupUM.isExpanded,
                collapseContentDescription = stringResourceSafe(id = R.string.common_hide),
                onClick = groupUM.onClick,
                isExpandable = true,
                shape = RectangleShape,
                labelColor = if (groupUM.isActionRequired) {
                    TangemTheme.colors3.text.status.warning
                } else {
                    TangemTheme.colors3.text.secondary
                },
                backgroundColor = backgroundColor,
            )
        }
    }
    item(key = "${groupUM.id}_tangem_pay", contentType = TangemPayMainUM::class.java) {
        ExpandableRowRevealItem(
            isExpanded = groupUM.isExpanded,
            index = 0,
            count = TANGEM_PAY_GROUP_CHILDREN_COUNT,
            modifier = modifier
                .animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
                .roundedShapeItemDecoration(
                    radius = TANGEM_PAY_GROUP_RADIUS,
                    currentIndex = 1,
                    addDefaultPadding = false,
                    lastIndex = TANGEM_PAY_GROUP_CHILDREN_COUNT,
                    backgroundColor = tangemPayGroupBackgroundColor(groupUM.isActionRequired),
                    groupKey = groupUM.id,
                ),
        ) {
            TangemPayGroupSlice(groupUM = groupUM, currentIndex = 1, lastIndex = TANGEM_PAY_GROUP_CHILDREN_COUNT) {
                tangemPayComponent.GroupedRow(
                    state = tangemPayUM,
                    isBalanceHidden = isBalanceHidden,
                    modifier = Modifier,
                )
            }
        }
    }
    item(key = "${groupUM.id}_money_account", contentType = MoneyAccountMainUM::class.java) {
        ExpandableRowRevealItem(
            isExpanded = groupUM.isExpanded,
            index = 1,
            count = TANGEM_PAY_GROUP_CHILDREN_COUNT,
            modifier = modifier
                .animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
                .roundedShapeItemDecoration(
                    radius = TANGEM_PAY_GROUP_RADIUS,
                    currentIndex = TANGEM_PAY_GROUP_CHILDREN_COUNT,
                    addDefaultPadding = false,
                    lastIndex = TANGEM_PAY_GROUP_CHILDREN_COUNT,
                    backgroundColor = tangemPayGroupBackgroundColor(groupUM.isActionRequired),
                    groupKey = groupUM.id,
                ),
        ) {
            TangemPayGroupSlice(
                groupUM = groupUM,
                currentIndex = TANGEM_PAY_GROUP_CHILDREN_COUNT,
                lastIndex = TANGEM_PAY_GROUP_CHILDREN_COUNT,
            ) {
                moneyAccountComponent.GroupedRow(
                    state = moneyAccountUM,
                    isBalanceHidden = isBalanceHidden,
                    modifier = Modifier,
                )
            }
        }
    }
}

@Composable
private fun TangemPayGroupSlice(
    groupUM: TangemPayGroupUM.Content,
    currentIndex: Int,
    lastIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
        if (groupUM.isActionRequired) {
            RoundedItemGroupGlowRing(
                groupKey = groupUM.id,
                currentIndex = currentIndex,
                lastIndex = lastIndex,
                variant = TangemGlowRing.Variant.Warning,
                cornerRadius = TANGEM_PAY_GROUP_RADIUS,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun tangemPayGroupBackgroundColor(isActionRequired: Boolean): Color = if (isActionRequired) {
    TangemTheme.colors3.bg.status.warningSubtle
} else {
    TangemTheme.colors3.bg.secondary
}