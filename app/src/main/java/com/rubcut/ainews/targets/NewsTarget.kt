package com.rubcut.ainews.targets

import android.content.Intent
import android.graphics.drawable.Icon as AndroidIcon
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.rubcut.ainews.Constants
import com.rubcut.ainews.R
import com.rubcut.ainews.SettingsRepository
import com.rubcut.ainews.ui.NewsActivity
import com.rubcut.ainews.ui.SettingsActivity

/**
 * Target that shows the short headline on the smartspace with a
 * "Tap to view full" line underneath; tapping opens the article screen.
 */
class NewsTarget : SmartspacerTargetProvider() {

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val context = provideContext()
        val settings = SettingsRepository(context).forTarget(smartspacerId)
        val stories = settings.getVisibleStories()

        if (stories.isEmpty()) {
            // Everything was dismissed: hide the target completely until the
            // next generation brings new stories.
            if (settings.isConfigured && settings.hasDismissedEverything()) {
                return emptyList()
            }
            // Not set up yet (or the last generation failed): point the user at
            // the settings instead of silently disappearing.
            val subtitle = settings.lastError
                ?: context.getString(R.string.target_empty_subtitle)
            return listOf(
                TargetTemplate.Basic(
                    id = "ai_news_empty_$smartspacerId",
                    componentName = componentName(),
                    title = Text(context.getString(R.string.target_empty_title)),
                    subtitle = Text(subtitle),
                    icon = Icon(icon(), shouldTint = true),
                    onClick = TapAction(intent = settingsIntent(smartspacerId))
                ).create().apply { canBeDismissed = false }
            )
        }

        // One target for everything: a single story shows its short headline,
        // several stories show a "N stories to read" summary instead.
        val isSingle = stories.size == 1
        val title = if (isSingle) {
            stories.first().shortTitle
        } else {
            context.resources.getQuantityString(
                R.plurals.stories_to_read, stories.size, stories.size
            )
        }

        return listOf(
            TargetTemplate.Basic(
                id = "${TARGET_PREFIX}${stories.first().id}_${stories.size}_$smartspacerId",
                componentName = componentName(),
                title = Text(title),
                // The complication line below the headline.
                subtitle = Text(context.getString(R.string.tap_to_view_full)),
                icon = Icon(icon(), shouldTint = true),
                onClick = TapAction(
                    intent = Intent(context, NewsActivity::class.java).apply {
                        // Open on the newest story; the rest are one swipe away.
                        putExtra(NewsActivity.EXTRA_NEWS_ID, stories.first().id)
                        putExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID, smartspacerId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            ).create().apply { canBeDismissed = true }
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        val context = provideContext()
        val refreshPeriodMinutes = smartspacerId
            ?.let { SettingsRepository(context).forTarget(it).refreshIntervalMinutes }
            ?: Constants.DEFAULT_REFRESH_PERIOD_MINUTES
        return Config(
            label = context.getString(R.string.target_label),
            description = context.getString(R.string.target_description),
            icon = icon(),
            refreshPeriodMinutes = refreshPeriodMinutes,
            refreshIfNotVisible = true,
            // All settings live inside the target — there is no launcher app.
            configActivity = settingsIntent(smartspacerId)
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        // The target represents every visible story, so swiping it away hides
        // all of them until the next generation.
        val settings = SettingsRepository(provideContext()).forTarget(smartspacerId)
        settings.getVisibleStories().forEach { settings.dismiss(it.id) }
        notifyChange(smartspacerId)
        return true
    }

    override fun onProviderRemoved(smartspacerId: String) {
        SettingsRepository(provideContext()).clearTarget(smartspacerId)
    }

    private fun settingsIntent(smartspacerId: String?) =
        Intent(provideContext(), SettingsActivity::class.java).apply {
            smartspacerId?.let { putExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID, it) }
            putExtra(SmartspacerConstants.EXTRA_AUTHORITY, Constants.TARGET_AUTHORITY)
        }

    private fun icon() = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_news)

    private fun componentName() =
        android.content.ComponentName(provideContext(), NewsTarget::class.java)

    companion object {
        const val TARGET_PREFIX = "ai_news_"
    }
}
