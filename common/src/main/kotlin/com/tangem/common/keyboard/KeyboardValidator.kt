package com.tangem.common.keyboard

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keyboard validator
 *
 * @property context application context
 */
@Singleton
class KeyboardValidator @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Get keyboard identifier [KeyboardID] */
    fun getKeyboardId(): KeyboardID? {
        val id = Settings.Secure.getString(
            this.context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
        ) ?: return null

        return KeyboardID(value = id)
    }

    /** Check if [id] is trusted */
    fun validate(id: KeyboardID): Boolean = trustedIdentifiers.contains(id.getPackageName())

    /**
     * Keyboard identifier
     *
     * @property value value
     */
    @JvmInline
    value class KeyboardID(val value: String) {

        /** Get package name */
        fun getPackageName(): String? = value.split("/").firstOrNull()
    }

    private companion object {

        val trustedIdentifiers = listOf(
            // Google
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin",
            "com.google.android.tts",

            // Samsung
            "com.sec.android.inputmethod.latin",
            "com.sec.android.inputmethod/.SamsungVoiceIME",
            "com.samsung.android.honeyboard",

            // Microsoft
            "com.microsoft.SwiftKeyApp",
            "com.touchtype.swiftkey",
            "com.touchtype.swiftkey.beta",

            // HtC
            "com.htc.sense.ime.langpack.tger",

            // LG
            "com.lge.ime",

            // Huawei
            "com.huawei.ohos.inputmethod",

            // Third party
            "com.menny.android.anysoftkeyboard",
            "ch.icoaching.wrio",
            "rkr.simplekeyboard.inputmethod",
            "keepass2android.keepass2android",
            "keepass2android.keepass2android_nonet",
            "com.softwarevalencia.openboard.inputmethod.latin",
            "kl.ime.oh",
            "com.syntellia.fleksy.keyboard",
            "org.pocketworkstation.pckeyboard",
        )
    }
}