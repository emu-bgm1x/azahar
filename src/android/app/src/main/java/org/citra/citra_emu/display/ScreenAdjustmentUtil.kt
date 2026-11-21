// Copyright 2023-2026 Citra Emulator Project / Azahar Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package org.citra.citra_emu.display

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import org.citra.citra_emu.NativeLibrary
import org.citra.citra_emu.R
import org.citra.citra_emu.features.settings.model.BooleanSetting
import org.citra.citra_emu.features.settings.model.IntListSetting
import org.citra.citra_emu.features.settings.model.IntSetting
import org.citra.citra_emu.features.settings.model.Settings
import org.citra.citra_emu.features.settings.utils.SettingsFile
import org.citra.citra_emu.utils.EmulationMenuSettings

class ScreenAdjustmentUtil(
    private val context: Context,
    private val windowManager: WindowManager,
    private val settings: Settings
) {
    fun swapScreen() {
        val isEnabled = !EmulationMenuSettings.swapScreens
        EmulationMenuSettings.swapScreens = isEnabled
        NativeLibrary.swapScreens(
            isEnabled,
            windowManager.defaultDisplay.rotation
        )
        BooleanSetting.SWAP_SCREEN.boolean = isEnabled
        settings.saveSetting(BooleanSetting.SWAP_SCREEN, SettingsFile.FILE_NAME_CONFIG)
    }

    fun cycleLayouts() {
        val landscapeLayoutsToCycle = IntListSetting.LAYOUTS_TO_CYCLE.list
        val landscapeValues =
            if (landscapeLayoutsToCycle.isNotEmpty()) {
                landscapeLayoutsToCycle.toIntArray()
            } else {
                context.resources.getIntArray(
                    R.array.landscapeValues
                )
            }
        val portraitValues = context.resources.getIntArray(R.array.portraitValues)

        if (NativeLibrary.isPortraitMode()) {
            val currentLayout = IntSetting.PORTRAIT_SCREEN_LAYOUT.int
            val pos = portraitValues.indexOf(currentLayout)
            val layoutOption = portraitValues[(pos + 1) % portraitValues.size]
            changePortraitOrientation(layoutOption)
        } else {
            val currentLayout = IntSetting.SCREEN_LAYOUT.int
            val pos = landscapeValues.indexOf(currentLayout)
            val layoutOption = landscapeValues[(pos + 1) % landscapeValues.size]
            changeScreenOrientation(layoutOption)
        }
    }

    fun changePortraitOrientation(layoutOption: Int) {
        IntSetting.PORTRAIT_SCREEN_LAYOUT.int = layoutOption
        settings.saveSetting(IntSetting.PORTRAIT_SCREEN_LAYOUT, SettingsFile.FILE_NAME_CONFIG)
        NativeLibrary.reloadSettings()
        NativeLibrary.updateFramebuffer(NativeLibrary.isPortraitMode())
    }

    fun changeScreenOrientation(layoutOption: Int) {
        IntSetting.SCREEN_LAYOUT.int = layoutOption
        settings.saveSetting(IntSetting.SCREEN_LAYOUT, SettingsFile.FILE_NAME_CONFIG)
        NativeLibrary.reloadSettings()
        NativeLibrary.updateFramebuffer(NativeLibrary.isPortraitMode())
    }

    // ff-bgm1x: 화면 배치 편집기용. 지금 저장된 사용자 배치 (x, y, 너비, 높이) 를 위·아래 화면 순서로 돌려준다
    fun customLayout(portrait: Boolean): Pair<IntArray, IntArray> {
        val s = customLayoutSettings(portrait)
        return intArrayOf(s[0].int, s[1].int, s[2].int, s[3].int) to
            intArrayOf(s[4].int, s[5].int, s[6].int, s[7].int)
    }

    // ff-bgm1x: 편집기에서 정한 사용자 배치를 저장하고, 배치를 '사용자 지정' 으로 바꿔 바로 적용한다
    fun applyCustomLayout(portrait: Boolean, top: IntArray, bottom: IntArray) {
        val s = customLayoutSettings(portrait)
        val values = top + bottom
        for (i in s.indices) {
            s[i].int = values[i].coerceAtLeast(0)
            settings.saveSetting(s[i], SettingsFile.FILE_NAME_CONFIG)
        }
        if (portrait) {
            changePortraitOrientation(PortraitScreenLayout.CUSTOM_PORTRAIT_LAYOUT.int)
        } else {
            changeScreenOrientation(ScreenLayout.CUSTOM_LAYOUT.int)
        }
    }

    private fun customLayoutSettings(portrait: Boolean): List<IntSetting> =
        if (portrait) {
            listOf(
                IntSetting.PORTRAIT_TOP_X, IntSetting.PORTRAIT_TOP_Y,
                IntSetting.PORTRAIT_TOP_WIDTH, IntSetting.PORTRAIT_TOP_HEIGHT,
                IntSetting.PORTRAIT_BOTTOM_X, IntSetting.PORTRAIT_BOTTOM_Y,
                IntSetting.PORTRAIT_BOTTOM_WIDTH, IntSetting.PORTRAIT_BOTTOM_HEIGHT
            )
        } else {
            listOf(
                IntSetting.LANDSCAPE_TOP_X, IntSetting.LANDSCAPE_TOP_Y,
                IntSetting.LANDSCAPE_TOP_WIDTH, IntSetting.LANDSCAPE_TOP_HEIGHT,
                IntSetting.LANDSCAPE_BOTTOM_X, IntSetting.LANDSCAPE_BOTTOM_Y,
                IntSetting.LANDSCAPE_BOTTOM_WIDTH, IntSetting.LANDSCAPE_BOTTOM_HEIGHT
            )
        }

    fun changeSecondaryOrientation(layoutOption: Int) {
        IntSetting.SECONDARY_DISPLAY_LAYOUT.int = layoutOption
        settings.saveSetting(IntSetting.SECONDARY_DISPLAY_LAYOUT, SettingsFile.FILE_NAME_CONFIG)
        NativeLibrary.reloadSettings()
        NativeLibrary.updateFramebuffer(NativeLibrary.isPortraitMode())
    }

    fun enableSecondaryDisplay(layoutOption: Int) {
        BooleanSetting.ENABLE_SECONDARY_DISPLAY.boolean = true
        settings.saveSetting(BooleanSetting.ENABLE_SECONDARY_DISPLAY, SettingsFile.FILE_NAME_CONFIG)
        changeSecondaryOrientation(layoutOption)
    }

    fun disableSecondaryDisplay() {
        BooleanSetting.ENABLE_SECONDARY_DISPLAY.boolean = false
        settings.saveSetting(BooleanSetting.ENABLE_SECONDARY_DISPLAY, SettingsFile.FILE_NAME_CONFIG)
    }

    fun changeActivityOrientation(orientationOption: Int) {
        val activity = context as? Activity ?: return
        IntSetting.ORIENTATION_OPTION.int = orientationOption
        settings.saveSetting(IntSetting.ORIENTATION_OPTION, SettingsFile.FILE_NAME_CONFIG)
        activity.requestedOrientation = orientationOption
    }

    fun toggleScreenUpright() {
        val uprightBoolean = BooleanSetting.UPRIGHT_SCREEN.boolean
        BooleanSetting.UPRIGHT_SCREEN.boolean = !uprightBoolean
        settings.saveSetting(BooleanSetting.UPRIGHT_SCREEN, SettingsFile.FILE_NAME_CONFIG)
        NativeLibrary.reloadSettings()
        NativeLibrary.updateFramebuffer(NativeLibrary.isPortraitMode())
    }
}
