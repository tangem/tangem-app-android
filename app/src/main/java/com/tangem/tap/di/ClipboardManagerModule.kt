package com.tangem.tap.di

import android.content.Context
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.tap.common.clipboard.MockClipboardManager
import com.tangem.tap.common.clipboard.DefaultClipboardManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.ClipboardManager as AndroidClipboardManager

@Module
@InstallIn(SingletonComponent::class)
internal class ClipboardManagerModule {

    @Provides
    @Singleton
    fun provideClipboardManager(@ApplicationContext context: Context): ClipboardManager {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? AndroidClipboardManager

        return if (clipboardManager != null) {
            DefaultClipboardManager(clipboardManager = clipboardManager)
        } else {
            MockClipboardManager
        }
    }
}