package com.tangem.feature.wallet.child.organizetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM

/**
 * Helper class for the DND list items
 *
 * @property id             ID of the item
 * @property roundingModeUM item [RoundingModeUM]
 * @property isShowShadow   if true then item should be elevated
 * */
@Immutable
internal sealed class OrganizeRowItemUM {
    abstract val id: String
    abstract val roundingModeUM: RoundingModeUM
    abstract val isShowShadow: Boolean

    /**
     * Item for token.
     *
     * @property id                 ID of the token
     * @property groupId            ID of the network group which contains this token
     * @property accountId          ID of account which contains this token
     * @property tokenRowUM         state of the token item
     * @property roundingModeUM     item [RoundingModeUM]
     * @property isShowShadow       if true then item should be elevated
     * */
    data class Token(
        override val isShowShadow: Boolean = false,
        override val roundingModeUM: RoundingModeUM = RoundingModeUM.None,
        val tokenRowUM: TangemTokenRowUM,
        val groupId: String,
        val accountId: String = "",
    ) : OrganizeRowItemUM() {
        override val id: String = tokenRowUM.id
    }

    /**
     * Item for network group header.
     *
     * @property id             ID of the network group
     * @property accountId      ID of account which contains this network group
     * @property headerRowUM    state of the network group header item
     * @property roundingModeUM item [RoundingModeUM]
     * @property isShowShadow   if true then item should be elevated
     * */
    data class Network(
        override val roundingModeUM: RoundingModeUM = RoundingModeUM.None,
        override val isShowShadow: Boolean = false,
        val headerRowUM: TangemHeaderRowUM,
        val accountId: String = "",
    ) : OrganizeRowItemUM() {
        override val id: String = headerRowUM.id
    }

    /**
     * Item for portfolio.
     *
     * @property id             ID of the portfolio
     * @property headerRowUM    state of the portfolio item
     * @property roundingModeUM item [RoundingModeUM]
     * @property isShowShadow   if true then item should be elevated
     * */
    data class Portfolio(
        override val roundingModeUM: RoundingModeUM = RoundingModeUM.None,
        val headerRowUM: TangemHeaderRowUM,
    ) : OrganizeRowItemUM() {
        override val id: String = headerRowUM.id
        override val isShowShadow: Boolean = false
    }

    /**
     * Helper item used to detect possible positions where a draggable item can be placed.
     *
     * @property id         ID of the placeholder for corresponding ID of the group
     * @property accountId  ID of account which contains this placeholder
     * */
    data class Placeholder(
        override val id: String,
        val accountId: String = "",
    ) : OrganizeRowItemUM() {
        override val isShowShadow: Boolean = false
        override val roundingModeUM: RoundingModeUM = RoundingModeUM.None
    }

    /**
     * Update item [RoundingModeUM]
     *
     * @param mode new [RoundingModeUM]
     *
     * @return updated [DraggableItem]
     * */
    fun updateRoundingMode(mode: RoundingModeUM): OrganizeRowItemUM = when (this) {
        is Placeholder -> this
        is Portfolio -> this.copy(roundingModeUM = mode)
        is Network -> this.copy(roundingModeUM = mode)
        is Token -> this.copy(roundingModeUM = mode)
    }

    /**
     * Update item shadow visibility
     *
     * @param show if true then item should be elevated
     *
     * @return updated [DraggableItem]
     * */
    fun updateShadowVisibility(show: Boolean): OrganizeRowItemUM = when (this) {
        is Portfolio,
        is Placeholder,
        -> this
        is Network -> this.copy(isShowShadow = show)
        is Token -> this.copy(isShowShadow = show)
    }
}