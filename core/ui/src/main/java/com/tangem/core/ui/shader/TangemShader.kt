package com.tangem.core.ui.shader

import com.tangem.core.ui.shader.runtime.RuntimeEffect

interface TangemShader {
    val speedModifier: Float
        get() = 0.5f

    val sksl: String

    /** Applies the uniforms required for this shader to the effect */
    fun applyUniforms(runtimeEffect: RuntimeEffect, time: Float, width: Float, height: Float) {
        runtimeEffect.setFloatUniform(name = "uResolution", value1 = width, value2 = height, value3 = width / height)
        runtimeEffect.setFloatUniform(name = "uTime", value1 = time)
    }
}