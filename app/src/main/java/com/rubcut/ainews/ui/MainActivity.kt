package com.rubcut.ainews.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.rubcut.ainews.data.NewsItem
import com.rubcut.ainews.data.NewsRepository
import com.rubcut.ainews.databinding.ActivityMainBinding
import com.rubcut.ainews.providers.NewsTargetProvider

/**
 * Small helper screen: explains the plugin and lets you restore dismissed
 * targets / push a test story for debugging.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonRestore.setOnClickListener {
            NewsRepository.clearDismissed(this)
            notifySmartspacer()
            Toast.makeText(this, "Targets restored", Toast.LENGTH_SHORT).show()
        }

        binding.buttonTest.setOnClickListener {
            val now = System.currentTimeMillis()
            NewsRepository.add(
                this,
                NewsItem(
                    id = "test_$now",
                    shortTitle = "Test story",
                    title = "A test story pushed from the plugin app",
                    body = "This story was generated locally to verify that the " +
                        "Smartspacer target, the complication line and the article " +
                        "screen all work as expected.",
                    source = "Local test",
                    timestamp = now
                )
            )
            notifySmartspacer()
            Toast.makeText(this, "Test story added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun notifySmartspacer() {
        SmartspacerTargetProvider.notifyChange(this, NewsTargetProvider::class.java)
    }
}
