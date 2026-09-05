package com.rubcut.ainews.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.rubcut.ainews.Constants
import com.rubcut.ainews.NewsUpdater
import com.rubcut.ainews.R
import com.rubcut.ainews.SettingsRepository
import com.rubcut.ainews.TargetSettings
import com.rubcut.ainews.targets.NewsTarget
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/**
 * The plugin has no launcher entry: this screen is opened by Smartspacer when
 * the target is added or its settings are opened, and it configures exactly
 * that one target instance.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: TargetSettings
    private lateinit var smartspacerId: String

    private lateinit var feedField: TextInputEditText
    private lateinit var intervalSlider: Slider
    private lateinit var intervalValue: TextView
    private lateinit var storiesSlider: Slider
    private lateinit var storiesValue: TextView
    private lateinit var status: TextView
    private lateinit var progress: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        smartspacerId = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
            ?: FALLBACK_ID
        settings = SettingsRepository(this).forTarget(smartspacerId)

        feedField = findViewById(R.id.feedField)
        intervalSlider = findViewById(R.id.intervalSlider)
        intervalValue = findViewById(R.id.intervalValue)
        storiesSlider = findViewById(R.id.storiesSlider)
        storiesValue = findViewById(R.id.storiesValue)
        status = findViewById(R.id.statusText)
        progress = findViewById(R.id.progress)

        feedField.setText(settings.feedUrl)

        intervalSlider.valueFrom = Constants.MIN_REFRESH_MINUTES.toFloat()
        intervalSlider.valueTo = Constants.MAX_REFRESH_MINUTES.toFloat()
        intervalSlider.stepSize = 15f
        intervalSlider.value = settings.refreshIntervalMinutes
            .coerceIn(Constants.MIN_REFRESH_MINUTES, Constants.MAX_REFRESH_MINUTES)
            .let { (it / 15) * 15 }.coerceAtLeast(Constants.MIN_REFRESH_MINUTES).toFloat()
        intervalSlider.addOnChangeListener { _, value, _ -> renderInterval(value.roundToInt()) }
        renderInterval(intervalSlider.value.roundToInt())

        storiesSlider.valueFrom = 1f
        storiesSlider.valueTo = 5f
        storiesSlider.stepSize = 1f
        storiesSlider.value = settings.maxStories.toFloat()
        storiesSlider.addOnChangeListener { _, value, _ -> renderStories(value.roundToInt()) }
        renderStories(settings.maxStories)

        findViewById<MaterialButton>(R.id.buttonUseDefaultFeed).setOnClickListener {
            feedField.setText(Constants.DEFAULT_FEED)
        }
        findViewById<MaterialButton>(R.id.buttonRestore).setOnClickListener {
            settings.clearDismissed()
            notifyTarget()
            toast(getString(R.string.toast_restored))
            renderStatus()
        }
        findViewById<MaterialButton>(R.id.buttonSave).setOnClickListener { save(refresh = true) }

        renderStatus()
    }

    private fun renderInterval(minutes: Int) {
        intervalValue.text = if (minutes >= 60 && minutes % 60 == 0) {
            resources.getQuantityString(R.plurals.hours, minutes / 60, minutes / 60)
        } else {
            resources.getQuantityString(R.plurals.minutes, minutes, minutes)
        }
    }

    private fun renderStories(count: Int) {
        storiesValue.text = resources.getQuantityString(R.plurals.stories, count, count)
    }

    private fun renderStatus() {
        val error = settings.lastError
        val updated = settings.lastUpdated
        status.text = when {
            error != null -> error
            updated > 0L -> getString(
                R.string.status_updated,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(updated)),
                settings.getVisibleStories().size
            )
            else -> getString(R.string.status_never_updated)
        }
    }

    private fun save(refresh: Boolean) {
        settings.feedUrl = feedField.text?.toString().orEmpty()
        settings.refreshIntervalMinutes = intervalSlider.value.roundToInt()
        settings.maxStories = storiesSlider.value.roundToInt()

        if (!refresh) {
            notifyTarget()
            finishOk()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            NewsUpdater.refresh(this@SettingsActivity, settings)
            setLoading(false)
            renderStatus()
            notifyTarget()
            val error = settings.lastError
            if (error != null) {
                toast(error)
            } else {
                toast(getString(R.string.toast_saved))
                finishOk()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        findViewById<MaterialButton>(R.id.buttonSave).isEnabled = !loading
    }

    private fun notifyTarget() {
        SmartspacerTargetProvider.notifyChange(this, NewsTarget::class.java, smartspacerId)
    }

    private fun finishOk() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    override fun onPause() {
        super.onPause()
        // Persist edits even if the user leaves with the back gesture.
        if (isFinishing) {
            settings.feedUrl = feedField.text?.toString().orEmpty()
            settings.refreshIntervalMinutes = intervalSlider.value.roundToInt()
            settings.maxStories = storiesSlider.value.roundToInt()
            notifyTarget()
        }
    }

    companion object {
        private const val FALLBACK_ID = "default"
    }
}
