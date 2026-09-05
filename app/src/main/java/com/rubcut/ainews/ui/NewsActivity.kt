package com.rubcut.ainews.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.rubcut.ainews.data.NewsRepository
import com.rubcut.ainews.databinding.ActivityNewsBinding
import com.rubcut.ainews.providers.NewsTargetProvider
import java.text.DateFormat
import java.util.Date

/**
 * Full article view opened when the Smartspacer target is tapped.
 * Bottom bar: "Close & dismiss" (left) removes the target, "Close" just closes.
 */
class NewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsBinding
    private var newsId: String? = null
    private var smartspacerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        newsId = intent.getStringExtra(EXTRA_NEWS_ID)
        smartspacerId = intent.getStringExtra(EXTRA_SMARTSPACER_ID)

        val item = newsId?.let { NewsRepository.get(this, it) }
        if (item == null) {
            finish()
            return
        }

        binding.newsTitle.text = item.title
        binding.newsBody.text = item.body
        binding.newsMeta.text = listOfNotNull(
            item.source.takeIf { it.isNotBlank() },
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(item.timestamp))
        ).joinToString(" · ")

        binding.buttonDismiss.setOnClickListener {
            newsId?.let { id -> NewsRepository.dismiss(this, id) }
            SmartspacerTargetProvider.notifyChange(this, NewsTargetProvider::class.java)
            finish()
        }

        binding.buttonClose.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_NEWS_ID = "news_id"
        const val EXTRA_SMARTSPACER_ID = "smartspacer_id"
    }
}
