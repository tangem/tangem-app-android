package com.tangem.features.details.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed class DetailsBlock {

    abstract val id: String

    data class Basic(
        override val id: String,
        val items: ImmutableList<Item>,
    ) : DetailsBlock() {

        data class Item(
            val title: TextReference,
            @DrawableRes
            val iconRes: Int,
            val onClick: () -> Unit,
        )
    }

    data class Component(
        override val id: String,
        val content: Content,
    ) : DetailsBlock() {

        fun interface Content {

            @Composable
            @Suppress("TopLevelComposableFunctions", "ComposableFunctionName")
            operator fun invoke(modifier: Modifier)
        }
    }
}
