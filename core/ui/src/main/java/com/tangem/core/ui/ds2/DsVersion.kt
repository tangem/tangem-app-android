package com.tangem.core.ui.ds2

import androidx.compose.runtime.Immutable

/**
 * Version of a design-system v2 (DS3) component, printed as `major.minor.patch`.
 *
 * [major] and [minor] mirror the component version in Figma. [patch] counts changes that have no
 * Figma release behind them — an Android-only bug fix, a new overload, an internal rework.
 *
 * A component declares its version in its `object Tangem<Name>` and repeats it in the `Version:`
 * line of its KDoc; the Storybook reads the declared value. Bump both in the change that alters
 * the component.
 */
@Immutable
data class DsVersion(val major: Int, val minor: Int, val patch: Int) {

    override fun toString(): String = "$major.$minor.$patch"
}