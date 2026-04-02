package com.tangem.common.extensions

import android.support.annotation.PluralsRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.platform.app.InstrumentationRegistry
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import io.github.kakaocup.compose.node.builder.ViewBuilder

fun ViewBuilder.hasLazyListItemPosition(position: Int) = apply {
    addSemanticsMatcher(SemanticsMatcher.expectValue(LazyListItemPositionSemantics, position))
}

fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String =
    InstrumentationRegistry.getInstrumentation()
        .targetContext
        .resources
        .getQuantityString(resId, quantity, *formatArgs)