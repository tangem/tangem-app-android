package com.tangem.features.tokenreceive.entity

import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import kotlin.math.abs

internal data class ReceiveQrLayout(
    val moduleCount: Int,
    val dots: List<Module>,
) {

    val moduleSize: Float get() = (CANVAS_SIZE - 2 * QUIET_ZONE) / moduleCount

    val finderOrigins: List<Module>
        get() = listOf(
            Module(row = 0, column = 0),
            Module(row = 0, column = moduleCount - FINDER_MODULES),
            Module(row = moduleCount - FINDER_MODULES, column = 0),
        )

    data class Module(val row: Int, val column: Int)

    companion object {
        const val CANVAS_SIZE = 220f
        const val QUIET_ZONE = 10f
        const val LOGO_SIZE = 56f
        const val LOGO_MARGIN = 4f
        const val DOT_SCALE = 0.75f
        const val FINDER_OUTER_RADIUS = 8f
        const val FINDER_PUPIL_RADIUS = 3f
        const val FINDER_MODULES = 7
        const val FINDER_PUPIL_INSET_MODULES = 2
        const val FINDER_PUPIL_MODULES = 3
    }
}

internal fun receiveQrLayout(content: String): ReceiveQrLayout {
    val matrix = encodeReceiveQr(content)
    return receiveQrLayout(moduleCount = matrix.width) { row, column -> matrix.get(column, row).toInt() == 1 }
}

private fun encodeReceiveQr(content: String): ByteMatrix {
    val matrix = Encoder.encode(content, ErrorCorrectionLevel.H).matrix
    if (matrix.width >= MIN_MODULE_COUNT) return matrix
    return Encoder.encode(content, ErrorCorrectionLevel.H, mapOf(EncodeHintType.QR_VERSION to MIN_VERSION)).matrix
}

private const val MIN_VERSION = 3
private const val MIN_MODULE_COUNT = 29

internal fun receiveQrLayout(moduleCount: Int, isDark: (row: Int, column: Int) -> Boolean): ReceiveQrLayout {
    val moduleSize = (ReceiveQrLayout.CANVAS_SIZE - 2 * ReceiveQrLayout.QUIET_ZONE) / moduleCount
    val center = ReceiveQrLayout.CANVAS_SIZE / 2
    val logoHalf = ReceiveQrLayout.LOGO_SIZE / 2 + ReceiveQrLayout.LOGO_MARGIN
    val finderEnd = moduleCount - ReceiveQrLayout.FINDER_MODULES

    fun isInFinder(row: Int, column: Int): Boolean {
        val isTop = row < ReceiveQrLayout.FINDER_MODULES
        val isLeft = column < ReceiveQrLayout.FINDER_MODULES
        return isTop && isLeft || isTop && column >= finderEnd || row >= finderEnd && isLeft
    }

    fun isUnderLogo(row: Int, column: Int): Boolean {
        val x = ReceiveQrLayout.QUIET_ZONE + column * moduleSize + moduleSize / 2
        val y = ReceiveQrLayout.QUIET_ZONE + row * moduleSize + moduleSize / 2
        return abs(x - center) < logoHalf && abs(y - center) < logoHalf
    }

    val dots = buildList {
        for (row in 0 until moduleCount) {
            for (column in 0 until moduleCount) {
                if (isDark(row, column) && !isInFinder(row, column) && !isUnderLogo(row, column)) {
                    add(ReceiveQrLayout.Module(row = row, column = column))
                }
            }
        }
    }
    return ReceiveQrLayout(moduleCount = moduleCount, dots = dots)
}