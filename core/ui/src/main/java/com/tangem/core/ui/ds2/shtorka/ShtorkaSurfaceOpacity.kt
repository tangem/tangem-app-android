package com.tangem.core.ui.ds2.shtorka

import androidx.compose.runtime.staticCompositionLocalOf

val LocalShtorkaSurfaceOpacity = staticCompositionLocalOf { OpaqueShtorkaSurface }

internal val OpaqueShtorkaSurface: () -> Float = { 1f }