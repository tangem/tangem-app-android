package com.tangem.features.details.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class DetailsItemUM {

    abstract val id: String

    data class Basic(
        override val id: String,
        val items: ImmutableList<Item>,
    ) : DetailsItemUM() {

        data class Item(
            val id: String,
            val title: TextReference,
            @DrawableRes
            val iconRes: Int,
            val onClick: () -> Unit,
        )
    }

    data class Component(
        override val id: String,
        val content: Content,
    ) : DetailsItemUM() {

        fun interface Content {

            @Composable
            @Suppress("TopLevelComposableFunctions", "ComposableFunctionName")
            operator fun invoke(modifier: Modifier)
        }
    }
}