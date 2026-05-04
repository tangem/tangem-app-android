package com.tangem.tap.core.security

import android.os.Build
import com.dexprotector.rtc.RtcStatus
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.utils.logging.TangemLogger

internal class DefaultDeviceSecurityInfoProvider : DeviceSecurityInfoProvider {
    override val isRooted: Boolean
        get() = getRtcStatusSafely()?.root == true
    override val isBootloaderUnlocked: Boolean
        get() = getRtcStatusSafely()?.unlockedBootloader == true
    override val isXposed: Boolean
        get() = getRtcStatusSafely()?.xposed == true

    override val isVulnerableToMediaTekExploit: Boolean by lazy {
        val isAffected by lazy { isAffectedMediaTekDevice() }
        val isPatched by lazy { hasSecurityPatch() }
        val isVulnerable = isAffected && !isPatched
        TangemLogger.i(
            "CVE-2026-20435 check: isAffectedMediaTek=$isAffected, " +
                "isPatched=$isPatched, isVulnerable=$isVulnerable",
        )
        isVulnerable
    }

    private fun isAffectedMediaTekDevice(): Boolean {
        val socModel = resolveMediaTekSocModel()
        val isAffected = socModel != null && socModel in AFFECTED_MEDIATEK_SOCS
        TangemLogger.i("CVE-2026-20435 SoC result: model=$socModel, isAffected=$isAffected")
        return isAffected
    }

    private fun resolveMediaTekSocModel(): String? {
        // Layer 1: API 31+ provides direct SoC info (public API, most reliable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manufacturer = Build.SOC_MANUFACTURER
            val model = Build.SOC_MODEL
            TangemLogger.i("CVE-2026-20435 Layer 1: SOC_MANUFACTURER=$manufacturer, SOC_MODEL=$model")
            if (manufacturer.equals("MediaTek", ignoreCase = true)) {
                extractSocModel(model)?.let { return it }
            }
        }

        // Layer 2: Build.HARDWARE often contains "mtXXXX" on MediaTek devices (public API)
        val hardware = Build.HARDWARE
        TangemLogger.i("CVE-2026-20435 Layer 2: HARDWARE=$hardware")
        extractSocModel(hardware)?.let { return it }

        return null
    }

    private fun extractSocModel(value: String): String? {
        val match = MEDIATEK_SOC_PATTERN.find(value.uppercase()) ?: return null
        return match.value
    }

    private fun hasSecurityPatch(): Boolean {
        val patch = Build.VERSION.SECURITY_PATCH
        val isPatched = try {
            patch >= MEDIATEK_CVE_FIX_PATCH_LEVEL
        } catch (e: Exception) {
            TangemLogger.w("CVE-2026-20435 patch check: failed to parse SECURITY_PATCH=$patch", e)
            false // fail-safe: treat unknown patch level as unpatched
        }
        TangemLogger.i(
            "CVE-2026-20435 patch check: SECURITY_PATCH=$patch, " +
                "required=$MEDIATEK_CVE_FIX_PATCH_LEVEL, isPatched=$isPatched",
        )
        return isPatched
    }

    private fun getRtcStatusSafely(): RtcStatus? {
        return try {
            RtcStatus.getRtcStatus()
        } catch (e: Throwable) {
            TangemLogger.e("Error", e)
            null
        }
    }

    private companion object {
        /** Android security patch level that includes the fix for CVE-2026-20435 */
        const val MEDIATEK_CVE_FIX_PATCH_LEVEL = "2026-03-05"

        /** Regex to extract MediaTek SoC model number (e.g., MT6789) */
        val MEDIATEK_SOC_PATTERN = Regex("MT\\d{4}")

        /** Affected MediaTek SoC models per Ledger Donjon disclosure */
        val AFFECTED_MEDIATEK_SOCS = setOf(
            "MT6739", "MT6761", "MT6765", "MT6768", "MT6781",
            "MT6789", "MT6813", "MT6833", "MT6853", "MT6855",
            "MT6877", "MT6878", "MT6879", "MT6880", "MT6885",
            "MT6886", "MT6890", "MT6893", "MT6895", "MT6897",
            "MT6983", "MT6985", "MT6989", "MT6990", "MT6993",
        )
    }
}