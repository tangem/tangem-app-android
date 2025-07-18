package com.tangem.core.ui.utils

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Semantics key for storing and retrieving the visual position
 * of an item within a LazyList. Used to verify item ordering
 * and visibility in UI tests.
 */
val LazyListItemPositionSemantics = SemanticsPropertyKey<Int>("LazyListItemPosition")

/**
 * Provides access to the item position semantics property
 * through semantics property receiver DSL.
 */
var SemanticsPropertyReceiver.lazyListItemPosition by LazyListItemPositionSemantics

/**
 * Semantics key for storing the total length/count of items
 * in a LazyList. Enables validation of list completeness in tests.
 */
val LazyListLengthSemantics = SemanticsPropertyKey<Int>("LazyListLength")

/**
 * Provides access to the list length semantics property
 * through semantics property receiver DSL.
 */
var SemanticsPropertyReceiver.lazyListLength by LazyListLengthSemantics