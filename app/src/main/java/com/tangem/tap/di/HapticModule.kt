package com.tangem.tap.di

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import com.tangem.tap.common.haptic.DefaultVibratorHapticManager
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class HapticModule {

    @Provides
    @Singleton
    fun provideHapticManager(@ApplicationContext context: Context): VibratorHapticManager {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        return if (vibrator.hasVibrator()) {
            DefaultVibratorHapticManager(vibrator = vibrator)
        } else {
            // mock
            object : VibratorHapticManager {
                override fun performOneTime(effect: TangemHapticEffect.OneTime) = Unit
            }
        }
    }
}
