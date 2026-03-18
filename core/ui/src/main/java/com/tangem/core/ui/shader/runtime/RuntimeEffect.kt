package com.tangem.core.ui.shader.runtime

import android.os.Build
import androidx.compose.ui.graphics.Brush
import com.tangem.core.ui.shader.TangemShader

interface RuntimeEffect {

    val isSupported: Boolean
    val isReady: Boolean

    /** Sets a float array uniform for this shader */
    fun setFloatUniform(name: String, value1: Float) {}

    /** Sets a float array uniform for this shader */
    fun setFloatUniform(name: String, value1: Float, value2: Float) {}

    /** Sets a float array uniform for this shader */
    fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) {}

    /** Sets a float array uniform for this shader */
    fun setFloatUniform(name: String, values: FloatArray) {}

    fun update(shader: TangemShader, time: Float, width: Float, height: Float) {}

    fun build(): Brush
}

internal fun buildEffect(shader: TangemShader): RuntimeEffect {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RuntimeShaderEffect(shader)
    } else {
        FallbackRuntimeEffect()
    }
}