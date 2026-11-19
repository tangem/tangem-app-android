package com.tangem.features.earn.impl.di

import com.google.common.truth.Truth.assertThat
import com.tangem.core.media.RemoteClipCache
import dagger.Provides
import org.junit.jupiter.api.Test
import javax.inject.Singleton

internal class EarnHeroVideoModuleTest {

    @Test
    fun `GIVEN the hero clip module WHEN reading its provider THEN the cache is a qualified singleton of the interface type`() {
        // Arrange
        val providers = EarnHeroVideoModule::class.java.declaredMethods
            .filter { it.isAnnotationPresent(Provides::class.java) }

        // Act
        val provider = providers.single()

        // Assert
        assertThat(provider.isAnnotationPresent(Singleton::class.java)).isTrue()
        assertThat(provider.isAnnotationPresent(EarnHeroClip::class.java)).isTrue()
        assertThat(provider.returnType).isEqualTo(RemoteClipCache::class.java)
    }
}