package com.tangem.features.earn.impl.di

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.impl.picker.model.EarnPickerModel
import com.tangem.features.earn.impl.widgets.calculator.model.EarnCalculatorModel
import dagger.multibindings.ClassKey
import org.junit.jupiter.api.Test

internal class EarnModelModuleTest {

    @Test
    fun `GIVEN model module WHEN reading its bindings THEN EarnCalculatorModel and EarnPickerModel are bound into the model map`() {
        // Act
        val boundModels = EarnModelModule::class.java.declaredMethods
            .mapNotNull { it.getAnnotation(ClassKey::class.java)?.value }

        // Assert
        assertThat(boundModels).containsAtLeast(EarnCalculatorModel::class, EarnPickerModel::class)
    }
}