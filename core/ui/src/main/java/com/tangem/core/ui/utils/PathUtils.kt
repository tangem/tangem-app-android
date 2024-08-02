package com.tangem.core.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)

fun Path.moveTo(offset: Offset) = moveTo(offset.x, offset.y)

fun Path.quadraticBezierTo(control: Offset, end: Offset) = quadraticBezierTo(control.x, control.y, end.x, end.y)