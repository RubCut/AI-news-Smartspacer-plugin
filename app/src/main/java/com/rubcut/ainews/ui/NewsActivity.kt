package com.rubcut.ainews.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.rubcut.ainews.MarkdownRenderer
import com.rubcut.ainews.R
import com.rubcut.ainews.SettingsRepository
import com.rubcut.ainews.targets.NewsTarget
import java.text.DateFormat
import java.util.Date

/**
 * Full screen article opened by tapping the target: a large flexible app bar
 * with the full headline, the story below and a bottom bar with
 * "Close & dismiss" (left, removes the target) and "Close" (right).
 */
class NewsActivity : AppCompatActivity() {

    private var newsId: String? = null
    private var smartspacerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
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

        applyInsets()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar).title = story.title

        findViewById<TextView>(R.id.newsMeta).text = listOfNotNull(
            story.source.takeIf { it.isNotBlank() },
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(story.timestamp))
        ).joinToString(" · ")

        val bodyView = findViewById<TextView>(R.id.newsBody)
        if (story.body.isBlank()) {
            bodyView.text = getString(R.string.no_article_text)
        } else {
            // The model writes GitHub flavoured Markdown; render it as spans.
            bodyView.text = MarkdownRenderer.render(story.body)
            bodyView.movementMethod = LinkMovementMethod.getInstance()
        }

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

        // Left: close and remove the target from the smartspace.
        findViewById<MaterialButton>(R.id.buttonDismiss).setOnClickListener {
            val id = newsId
            val targetId = smartspacerId
            if (settings != null && id != null && targetId != null) {
                settings.dismiss(id)
                SmartspacerTargetProvider.notifyChange(this, NewsTarget::class.java, targetId)
            }
            finish()
        }

        // Right: just close.
        findViewById<MaterialButton>(R.id.buttonClose).setOnClickListener { finish() }
    }

    private fun applyInsets() {
        val appBar = findViewById<View>(R.id.appBar)
        val content = findViewById<View>(R.id.contentScroll)
        val buttonBar = findViewById<View>(R.id.buttonBar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            appBar.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            content.updatePadding(left = bars.left, right = bars.right)
            buttonBar.updatePadding(
                left = bars.left + BAR_PADDING_DP.dp(),
                right = bars.right + BAR_PADDING_DP.dp(),
                bottom = bars.bottom + BAR_PADDING_DP.dp()
            )
            insets
        }
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_NEWS_ID = "news_id"
        private const val BAR_PADDING_DP = 16
    }
}
