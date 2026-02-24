package com.tangem.core.ui.shader.runtime

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import com.tangem.core.ui.shader.TangemShader

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class RuntimeShaderEffect(tangemShader: TangemShader) : RuntimeEffect {
    private val compositeRuntimeEffect = RuntimeShader(tangemShader.sksl)

    override val isSupported: Boolean = true
    override var isReady: Boolean = false

    override fun setFloatUniform(name: String, value1: Float) {
        compositeRuntimeEffect.setFloatUniform(name, value1)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float) {
        compositeRuntimeEffect.setFloatUniform(name, value1, value2)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) {
        compositeRuntimeEffect.setFloatUniform(name, value1, value2, value3)
    }

    override fun setFloatUniform(name: String, values: FloatArray) {
        compositeRuntimeEffect.setFloatUniform(name, values)
    }

    override fun update(shader: TangemShader, time: Float, width: Float, height: Float) {
        shader.applyUniforms(runtimeEffect = this, time = time, width = width, height = height)
        isReady = width > 0 && height > 0
    }

    override fun build(): Brush {
        return ShaderBrush(compositeRuntimeEffect)
    }
}