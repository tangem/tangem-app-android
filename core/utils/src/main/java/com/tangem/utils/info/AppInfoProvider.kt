package com.tangem.utils.info

/**
 * Runtime information about the host application and device.
 *
 * Consumed by API layers to populate request headers and bodies (see
 * `RequestHeader.AppVersionPlatformHeaders` and push-notification registration) and by feature
 * code that needs to branch on platform / vendor.
 */
interface AppInfoProvider {

    /** Platform identifier, e.g. `"Android"`. */
    val platform: String

    /** Human-readable device name in the form `"$MANUFACTURER $MODEL"`, e.g. `"Google Pixel 8"`. */
    val device: String

    /** OS release string, e.g. `"14"` on Android 14. Corresponds to `Build.VERSION.RELEASE`. */
    val osVersion: String

    /**
     * Android API level of the running system, e.g. `34` on Android 14. Corresponds to
     * `Build.VERSION.SDK_INT`. Use this (not [osVersion]) when branching by framework capability.
     */
    val sdkVersion: Int

    /** Current locale as a BCP 47 language tag (e.g. `"en-US"`, `"zh-CN"`). */
    val language: String

    /**
     * Display density as a float (e.g. `"2.0"`, `"2.75"`, `"3.0"`).
     */
    val deviceScale: Float

    /** IANA time-zone id of the device's current time zone, e.g. `"Europe/Moscow"`, `"UTC"`. */
    val timezone: String

    /** User-visible app version string (e.g. `"5.36.4"`), matching `BuildConfig.VERSION_NAME`. */
    val appVersion: String

    /** Monotonically-increasing internal build number, matching `BuildConfig.VERSION_CODE`. */
    val appVersionCode: Int

    /**
     * `true` if the device manufacturer or brand is Huawei. Used to gate features that depend on
     * Google Play Services (HMS-only devices cannot rely on FCM, Play Billing, etc.).
     */
    val isHuaweiDevice: Boolean
}