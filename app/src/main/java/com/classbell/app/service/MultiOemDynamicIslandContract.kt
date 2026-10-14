package com.classbell.app.service

/**
 * Universal Dynamic Island / Live Activity / Capsule Contract for Chinese OEMs:
 * - Google Android 15+ Promoted Ongoing Notifications
 * - OPPO / OnePlus (ColorOS 14/15/16 Fluid Cloud / Aqua Dynamics)
 * - Xiaomi (HyperOS 1.0 & 2.0 Focus Notifications)
 * - vivo (OriginOS 4/5 Atomic Island)
 * - Huawei (HarmonyOS 4/NEXT Live View)
 */
object MultiOemDynamicIslandContract {
    // 1. Google Android 15+ Standard
    const val KEY_PROMOTED_ONGOING = "android.requestPromotedOngoing"

    // 2. OPPO / OnePlus ColorOS 14/15/16 Fluid Cloud (流体云 / 灵动胶囊)
    const val KEY_OPLUS_FLUID_CLOUD = "oplus.fluid_cloud"
    const val KEY_COLOROS_FLUID_CLOUD = "coloros.fluid_cloud"
    const val KEY_OPLUS_CAPSULE = "oplus_capsule"
    const val KEY_COLOROS_CAPSULE = "coloros_capsule"
    const val KEY_OPLUS_NOTIFICATION_TYPE = "oplus_notification_type"
    const val KEY_OPLUS_VIEW_TYPE = "oplus_view_type"
    const val KEY_COLOROS_NOTIFICATION_TYPE = "coloros_notification_type"
    const val KEY_COLOROS_VIEW_TYPE = "coloros_view_type"
    const val KEY_COLOROS_PROGRESS = "coloros_progress"
    const val KEY_OPLUS_PROGRESS = "oplus_progress"
    const val KEY_COLOROS_PROGRESS_MAX = "coloros_progress_max"
    const val KEY_OPLUS_PROGRESS_MAX = "oplus_progress_max"
    const val KEY_COLOROS_PROGRESS_CURRENT = "coloros_progress_current"
    const val KEY_OPLUS_PROGRESS_CURRENT = "oplus_progress_current"
    const val KEY_COLOROS_CAPSULE_ICON = "coloros_capsule_icon"
    const val KEY_OPLUS_CAPSULE_ICON = "oplus_capsule_icon"
    const val KEY_COLOROS_CAPSULE_TITLE = "coloros_capsule_title"
    const val KEY_OPLUS_CAPSULE_TITLE = "oplus_capsule_title"
    const val KEY_COLOROS_CAPSULE_TIME = "coloros_capsule_time"
    const val KEY_OPLUS_CAPSULE_TIME = "oplus_capsule_time"

    // 3. Xiaomi HyperOS 1.0 / 2.0 焦点通知 (Focus Notification)
    const val KEY_MIUI_FOCUS_NOTIFICATION = "miui.focus_notification"
    const val KEY_EXTRA_FOCUS_NOTIFICATION = "extra_focus_notification"
    const val KEY_MIUI_FOCUS_NOTIFICATION_TYPE = "miui.focus_notification_type"
    const val KEY_MIUI_FOCUS_TITLE = "miui.focus_title"

    // 4. vivo OriginOS 4 / 5 原子岛 (Atomic Island)
    const val KEY_VIVO_ATOMIC_ISLAND = "vivo.atomic.island"
    const val KEY_VIVO_SUMMARY = "vivo.summary"

    // 5. Huawei HarmonyOS 4 / HarmonyOS NEXT 实况窗 (Live View)
    const val KEY_HW_LIVE_VIEW = "hw_live_view"
    const val KEY_HW_LIVE_VIEW_TYPE = "hw_live_view_type"
    const val KEY_HW_LIVE_VIEW_TITLE = "hw_live_view_title"

    // General app substitution name
    const val KEY_ANDROID_SUBST_NAME = "android.substName"

    /**
     * Builds a pure Map of extras for cross-OEM dynamic island / capsule compatibility.
     */
    fun buildUniversalExtrasMap(
        capsuleTitle: String,
        whenMillis: Long,
        progressPercent: Int,
        appName: String = "Timeless"
    ): Map<String, Any> {
        return mapOf(
            // 1. Android 15+ Native Promoted Ongoing
            KEY_PROMOTED_ONGOING to true,

            // 2. OPPO / OnePlus ColorOS 14/15/16 Fluid Cloud
            KEY_OPLUS_FLUID_CLOUD to true,
            KEY_COLOROS_FLUID_CLOUD to true,
            KEY_OPLUS_CAPSULE to true,
            KEY_COLOROS_CAPSULE to true,
            KEY_OPLUS_NOTIFICATION_TYPE to 1,
            KEY_OPLUS_VIEW_TYPE to 0,
            KEY_COLOROS_NOTIFICATION_TYPE to 1,
            KEY_COLOROS_VIEW_TYPE to 0,
            KEY_COLOROS_PROGRESS to progressPercent,
            KEY_OPLUS_PROGRESS to progressPercent,
            KEY_COLOROS_PROGRESS_MAX to 100,
            KEY_OPLUS_PROGRESS_MAX to 100,
            KEY_COLOROS_PROGRESS_CURRENT to progressPercent,
            KEY_OPLUS_PROGRESS_CURRENT to progressPercent,
            KEY_COLOROS_CAPSULE_ICON to "ic_stat_in_class",
            KEY_OPLUS_CAPSULE_ICON to "ic_stat_in_class",
            KEY_COLOROS_CAPSULE_TITLE to capsuleTitle,
            KEY_OPLUS_CAPSULE_TITLE to capsuleTitle,
            KEY_COLOROS_CAPSULE_TIME to whenMillis,
            KEY_OPLUS_CAPSULE_TIME to whenMillis,
            KEY_ANDROID_SUBST_NAME to appName,

            // 3. Xiaomi HyperOS 1.0 / 2.0 焦点通知
            KEY_MIUI_FOCUS_NOTIFICATION to true,
            KEY_EXTRA_FOCUS_NOTIFICATION to true,
            KEY_MIUI_FOCUS_NOTIFICATION_TYPE to 1,
            KEY_MIUI_FOCUS_TITLE to capsuleTitle,

            // 4. vivo OriginOS 4 / 5 原子岛
            KEY_VIVO_ATOMIC_ISLAND to true,
            KEY_VIVO_SUMMARY to capsuleTitle,

            // 5. Huawei HarmonyOS 4 / HarmonyOS NEXT 实况窗
            KEY_HW_LIVE_VIEW to true,
            KEY_HW_LIVE_VIEW_TYPE to 1,
            KEY_HW_LIVE_VIEW_TITLE to capsuleTitle
        )
    }

    /**
     * Fills an Android Bundle with all cross-OEM dynamic island extras.
     */
    fun populateBundle(
        bundle: android.os.Bundle,
        capsuleTitle: String,
        whenMillis: Long,
        progressPercent: Int,
        appName: String = "Timeless"
    ): android.os.Bundle {
        val map = buildUniversalExtrasMap(capsuleTitle, whenMillis, progressPercent, appName)
        for ((key, value) in map) {
            when (value) {
                is Boolean -> bundle.putBoolean(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is String -> bundle.putString(key, value)
            }
        }
        return bundle
    }
}
