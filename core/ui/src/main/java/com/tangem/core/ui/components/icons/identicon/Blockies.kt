package com.tangem.core.ui.components.icons.identicon

import kotlin.math.abs
import kotlin.math.floor

/**
 * Ident icon. Blockies algorithm.
 * @see [Blockies](https://github.com/ethereum/blockies)
 *
 * @param address Address to generate the ident icon
 */
@Suppress("MagicNumber")
internal data class Blockies(
    val address: String,
) {

    val primaryColor: Int
    val backgroundColor: Int
    val spotColor: Int
    val data: MutableList<Float>

    init {
        val seed = seedFromAddress(address)
        // IMPORTANT! Color generation is based on the seed, thus ORDER is important.
        primaryColor = colorFromSeed(seed)
        backgroundColor = colorFromSeed(seed)
        spotColor = colorFromSeed(seed)
        data = dataFromSeed(seed)
    }

    private fun seedFromAddress(address: String): MutableList<Long> {
        val seed = MutableList(SIZE) { DEFAULT_VALUE_L }
        address.indices.forEach { index ->
            seed[index % HALF_SIZE] =
                (seed[index % HALF_SIZE] shl 5) - seed[index % HALF_SIZE] + Character.codePointAt(address, index)
        }

        // Important cast to preserve JavaScript type behavior
        seed.indices.map { seed[it] = seed[it].toInt().toLong() }
        return seed
    }

    private fun colorFromSeed(seed: MutableList<Long>): Int {
        val h = floor(nextSeed(seed) * DEGREE_CIRCLE_F)
        val s = nextSeed(seed) * SATURATION_MAX + SATURATION_MIN
        val l = (nextSeed(seed) + nextSeed(seed) + nextSeed(seed) + nextSeed(seed)) * PROBABILITY_LIGHTNESS
        return toRgb(h, s, l)
    }

    private fun nextSeed(seed: MutableList<Long>): Float {
        val t = (seed[0] xor (seed[0] shl 11)).toInt()
        seed[0] = seed[1]
        seed[1] = seed[2]
        seed[2] = seed[3]
        seed[3] = seed[3] xor (seed[3] shr 19) xor t.toLong() xor (t shr 8).toLong()

        return abs(seed[3]).toFloat() / Integer.MAX_VALUE
    }

    private fun dataFromSeed(seed: MutableList<Long>) = MutableList(SIZE * SIZE) { DEFAULT_VALUE_F }.apply {
        (0 until SIZE).forEach { row ->
            (0 until HALF_SIZE).forEach { column ->
                val value = floor(nextSeed(seed) * PROBABILITY_COLOR)
                this[row * SIZE + column] = value
                this[(row + 1) * SIZE - column - 1] = value
            }
        }
    }

    private fun toRgb(paramH: Float, paramS: Float, paramL: Float): Int {
        val h = paramH % DEGREE_CIRCLE_F / DEGREE_CIRCLE_F
        val s = paramS / PERCENT_MAX
        val l = paramL / PERCENT_MAX

        val q = if (l < 0.5) l * (1 + s) else l + s - s * l
        val p = 2 * l - q

        val r = hueToRGB(p, q, h + 1f / 3f).coerceIn(0f, 1f)
        val g = hueToRGB(p, q, h).coerceIn(0f, 1f)
        val b = hueToRGB(p, q, h - 1f / 3f).coerceIn(0f, 1f)

        val red = (r * HEX_COLOR_MAX).toInt()
        val green = (g * HEX_COLOR_MAX).toInt()
        val blue = (b * HEX_COLOR_MAX).toInt()
        return RGB_ALPHA_PART shl RGB_R_PART or (red shl RGB_G_PART) or (green shl RGB_B_PART) or blue
    }

    private fun hueToRGB(p: Float, q: Float, h: Float): Float {
        var hue = h
        if (hue < 0) hue += 1f
        if (hue > 1) hue -= 1f
        if (6 * hue < 1) return p + (q - p) * 6f * hue
        if (2 * hue < 1) return q

        return if (3 * hue < 2) p + (q - p) * 6f * (2f / 3f - hue) else p
    }

    companion object {
        internal const val SIZE = 8
        private const val HALF_SIZE = SIZE / 2
        private const val DEFAULT_VALUE_L = 0L
        private const val DEFAULT_VALUE_F = 0f
        private const val DEGREE_CIRCLE_F = 360.0f
        private const val HEX_COLOR_MAX = 255
        private const val PERCENT_MAX = 100f
        private const val SATURATION_MIN = 40f
        private const val SATURATION_MAX = 60f
        private const val PROBABILITY_LIGHTNESS = 25f
        private const val PROBABILITY_COLOR = 2.3f
        private const val RGB_ALPHA_PART = 0xFF
        private const val RGB_R_PART = 24
        private const val RGB_G_PART = 16
        private const val RGB_B_PART = 8
    }
}