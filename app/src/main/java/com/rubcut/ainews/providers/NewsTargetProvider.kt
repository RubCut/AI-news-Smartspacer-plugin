package com.rubcut.ainews.providers

import android.content.ComponentName
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.rubcut.ainews.R
import com.rubcut.ainews.data.NewsRepository
import com.rubcut.ainews.ui.NewsActivity

class NewsTargetProvider : SmartspacerTargetProvider() {

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val context = provideContext()
        return NewsRepository.getVisible(context).map { item ->
            TargetTemplate.Basic(
                id = "$TARGET_PREFIX${item.id}",
                componentName = ComponentName(context, NewsTargetProvider::class.java),
                title = Text(item.shortTitle),
                // The "complication" line underneath the headline.
                subtitle = Text(context.getString(R.string.tap_to_view_full)),
                icon = Icon(
                    android.graphics.drawable.Icon.createWithResource(
                        context, R.drawable.ic_news
                    )
                ),
                onClick = TapAction(
                    intent = Intent(context, NewsActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(NewsActivity.EXTRA_NEWS_ID, item.id)
                        putExtra(NewsActivity.EXTRA_SMARTSPACER_ID, smartspacerId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            ).create().apply {
                canBeDismissed = true
            }
        }
    }

    override fun getConfig(smartspacerId: String?): Config {
        val context = provideContext()
        return Config(
            label = context.getString(R.string.target_label),
            description = context.getString(R.string.target_description),
            icon = android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_news)
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        val newsId = targetId.removePrefix(TARGET_PREFIX)
        NewsRepository.dismiss(provideContext(), newsId)
        notifyChange(smartspacerId)
        return true
    }

    private fun provideContext() = requireNotNull(context) { "No context available" }

    companion object {
        const val TARGET_PREFIX = "ai_news_"
        const val AUTHORITY = "com.rubcut.ainews.target.news"
    }
}
