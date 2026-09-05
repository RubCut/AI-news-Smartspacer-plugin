package com.rubcut.ainews.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.rubcut.ainews.NewsItem
import com.rubcut.ainews.R
import com.rubcut.ainews.SettingsRepository
import com.rubcut.ainews.TargetSettings
import com.rubcut.ainews.targets.NewsTarget

/**
 * Full screen article reader. Every unread story is a page: swipe left/right
 * to move between them, with a dots overlay above the bottom buttons.
 *
 * "Close & dismiss" removes the story you are reading (and the whole target
 * once the last one is gone), "Close" just closes the reader.
 */
class NewsActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var dots: TabLayout
    private lateinit var counter: TextView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var adapter: StoryPagerAdapter

    private var settings: TargetSettings? = null
    private var smartspacerId: String? = null
    private var stories: List<NewsItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        smartspacerId = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
        settings = smartspacerId?.let { SettingsRepository(this).forTarget(it) }
        stories = settings?.getVisibleStories().orEmpty()
        if (stories.isEmpty()) {
            finish()
            return
        }

        pager = findViewById(R.id.pager)
        dots = findViewById(R.id.dots)
        counter = findViewById(R.id.pageCounter)
        toolbar = findViewById(R.id.toolbar)

        applyInsets()
        toolbar.setNavigationOnClickListener { finish() }

        adapter = StoryPagerAdapter(stories)
        pager.adapter = adapter
        pager.offscreenPageLimit = 1
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = renderPageState(position)
        })

        // Dots are driven by the pager and can also be tapped to jump.
        TabLayoutMediator(dots, pager) { _, _ -> }.attach()

        // The target opens on a specific story; land on that page.
        val requestedId = intent.getStringExtra(EXTRA_NEWS_ID)
        val startPage = stories.indexOfFirst { it.id == requestedId }.coerceAtLeast(0)
        pager.setCurrentItem(startPage, false)
        renderPageState(startPage)

        findViewById<MaterialButton>(R.id.buttonDismiss).setOnClickListener { dismissCurrent() }
        findViewById<MaterialButton>(R.id.buttonClose).setOnClickListener { finish() }
    }

    private fun renderPageState(position: Int) {
        val single = stories.size <= 1
        dots.visibility = if (single) View.GONE else View.VISIBLE
        counter.visibility = if (single) View.GONE else View.VISIBLE
        if (!single) {
            counter.text = getString(R.string.page_counter, position + 1, stories.size)
        }
        toolbar.title = if (single) {
            getString(R.string.app_name)
        } else {
            resources.getQuantityString(
                R.plurals.stories_to_read, stories.size, stories.size
            )
        }
    }

    /** Hides the story being read; closes the reader when none are left. */
    private fun dismissCurrent() {
        val currentSettings = settings ?: return finish()
        val targetId = smartspacerId ?: return finish()
        val position = pager.currentItem
        val story = stories.getOrNull(position) ?: return finish()

        currentSettings.dismiss(story.id)
        SmartspacerTargetProvider.notifyChange(this, NewsTarget::class.java, targetId)

        val remaining = stories.filterNot { it.id == story.id }
        if (remaining.isEmpty()) {
            finish()
            return
        }

        stories = remaining
        adapter.submit(remaining)
        val next = position.coerceAtMost(remaining.lastIndex)
        pager.setCurrentItem(next, false)
        renderPageState(next)
    }

    private fun applyInsets() {
        val appBar = findViewById<View>(R.id.appBar)
        val bottomBar = findViewById<View>(R.id.bottomBar)
        val padding = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            appBar.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            bottomBar.updatePadding(
                left = bars.left + padding,
                right = bars.right + padding,
                bottom = bars.bottom + padding
            )
            insets
        }
    }

    companion object {
        const val EXTRA_NEWS_ID = "news_id"
    }
}
