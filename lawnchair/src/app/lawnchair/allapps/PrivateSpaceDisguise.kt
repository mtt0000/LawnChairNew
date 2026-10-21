package app.lawnchair.allapps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.preferences2.firstCached
import com.android.launcher3.R
import com.android.launcher3.compat.AlphabeticIndexCompat
import com.android.launcher3.icons.LauncherIcons
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.ItemInfoWithIcon.FLAG_NOT_PINNABLE

/**
 * How the private space is shown in the app drawer while it is locked: either the stock bar at the
 * bottom of the app list, or a fake app icon that unlocks the private space when tapped.
 */
enum class PrivateSpaceDisguise(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    NONE(R.string.private_space_locked_style_bar, 0),
    CALCULATOR(R.string.private_space_disguise_calculator, R.drawable.ic_private_space_disguise_calculator),
    FILES(R.string.private_space_disguise_files, R.drawable.ic_private_space_disguise_files),
    NOTES(R.string.private_space_disguise_notes, R.drawable.ic_private_space_disguise_notes),
    WEATHER(R.string.private_space_disguise_weather, R.drawable.ic_private_space_disguise_weather),
    CLOCK(R.string.private_space_disguise_clock, R.drawable.ic_private_space_disguise_clock),
    ;

    /** Builds the fake app shown in place of the locked private space. */
    fun createAppInfo(context: Context): PrivateSpaceDisguiseInfo {
        check(this != NONE) { "NONE has no disguise icon" }
        val info = PrivateSpaceDisguiseInfo(this)
        info.title = context.getString(labelRes)
        info.contentDescription = info.title
        info.sectionName = AlphabeticIndexCompat(context).computeSectionName(info.title)
        info.componentName = ComponentName(context.packageName, "$DISGUISE_CLASS_PREFIX$name")
        info.intent = Intent(Intent.ACTION_MAIN).setComponent(info.componentName)
        info.user = Process.myUserHandle()
        info.runtimeStatusFlags = info.runtimeStatusFlags or FLAG_NOT_PINNABLE
        val shortcut = Intent.ShortcutIconResource.fromContext(context, iconRes)
        info.bitmap = LauncherIcons.obtain(context).use { it.createIconBitmap(shortcut) }
        return info
    }

    companion object {
        private const val DISGUISE_CLASS_PREFIX = "app.lawnchair.PrivateSpaceDisguise."

        fun fromString(value: String): PrivateSpaceDisguise = entries.firstOrNull { it.name == value } ?: NONE

        @JvmStatic
        fun current(context: Context): PrivateSpaceDisguise = PreferenceManager2.getInstance(context)
            .privateSpaceDisguise.firstCached()
    }
}

/** The fake app shown in the app drawer while the private space is locked and disguised. */
class PrivateSpaceDisguiseInfo(val disguise: PrivateSpaceDisguise) : AppInfo()
