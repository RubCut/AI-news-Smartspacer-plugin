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
            // Nothing to show yet: point the user at the settings instead of
            // silently hiding the target.
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

        return stories.map { story ->
            TargetTemplate.Basic(
                id = "${TARGET_PREFIX}${story.id}_$smartspacerId",
                componentName = componentName(),
                // Short headline on the target itself…
                title = Text(story.shortTitle),
                // …and the complication line below it.
                subtitle = Text(context.getString(R.string.tap_to_view_full)),
                icon = Icon(icon(), shouldTint = true),
                onClick = TapAction(
                    intent = Intent(context, NewsActivity::class.java).apply {
                        putExtra(NewsActivity.EXTRA_NEWS_ID, story.id)
                        putExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID, smartspacerId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            ).create().apply { canBeDismissed = true }
        }
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
        val newsId = targetId.removePrefix(TARGET_PREFIX).removeSuffix("_$smartspacerId")
        SettingsRepository(provideContext()).forTarget(smartspacerId).dismiss(newsId)
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
