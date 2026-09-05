package com.rubcut.ainews.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.rubcut.ainews.R
import com.rubcut.ainews.SettingsRepository
import com.rubcut.ainews.targets.NewsTarget
import java.text.DateFormat
import java.util.Date

/**
 * Full article view opened by tapping the target.
 *
 * Bottom bar: "Close & dismiss" on the left removes the target from Smartspacer,
 * "Close" simply closes this window.
 */
class NewsActivity : AppCompatActivity() {

    private var newsId: String? = null
    private var smartspacerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        newsId = intent.getStringExtra(EXTRA_NEWS_ID)
        smartspacerId = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)

        val settings = smartspacerId?.let { SettingsRepository(this).forTarget(it) }
        val story = newsId?.let { settings?.getStory(it) }
        if (story == null) {
            finish()
            return
        }

        findViewById<TextView>(R.id.newsTitle).text = story.title
        findViewById<TextView>(R.id.newsMeta).text = listOfNotNull(
            story.source.takeIf { it.isNotBlank() },
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(story.timestamp))
        ).joinToString(" · ")

        val body = findViewById<TextView>(R.id.newsBody)
        body.text = story.body.ifBlank { getString(R.string.no_article_text) }

        val openButton = findViewById<MaterialButton>(R.id.buttonOpen)
        val url = story.url
        if (url.isNullOrBlank()) {
            openButton.visibility = View.GONE
        } else {
            openButton.setOnClickListener {
                runCatching {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                finish()
            }
        }

        // Left button: close and remove the target from the smartspace.
        findViewById<MaterialButton>(R.id.buttonDismiss).setOnClickListener {
            val id = newsId
            val targetId = smartspacerId
            if (settings != null && id != null && targetId != null) {
                settings.dismiss(id)
                SmartspacerTargetProvider.notifyChange(
                    this, NewsTarget::class.java, targetId
                )
            }
            finish()
        }

        // Right button: just close.
        findViewById<MaterialButton>(R.id.buttonClose).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_NEWS_ID = "news_id"
    }
}
