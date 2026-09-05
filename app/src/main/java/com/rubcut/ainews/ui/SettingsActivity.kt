package com.rubcut.ainews.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.color.DynamicColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.rubcut.ainews.AiClient
import com.rubcut.ainews.AiProvider
import com.rubcut.ainews.BuildConfig
import com.rubcut.ainews.Constants
import com.rubcut.ainews.NewsUpdater
import com.rubcut.ainews.R
import com.rubcut.ainews.SettingsRepository
import com.rubcut.ainews.toClientConfig
import com.rubcut.ainews.StoryLength
import com.rubcut.ainews.TargetSettings
import com.rubcut.ainews.targets.NewsTarget
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/**
 * The plugin has no launcher entry: Smartspacer opens this screen when the
 * target is added or its settings are opened, and it configures that one
 * target instance — topic, AI provider, key, model and refresh interval.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: TargetSettings
    private lateinit var smartspacerId: String

    private lateinit var topicField: TextInputEditText
    private lateinit var apiKeyField: TextInputEditText
    private lateinit var languageField: TextInputEditText
    private lateinit var providerField: MaterialAutoCompleteTextView
    private lateinit var baseUrlField: TextInputEditText
    private lateinit var baseUrlLayout: TextInputLayout
    private lateinit var apiKeyLayout: TextInputLayout
    private lateinit var modelField: MaterialAutoCompleteTextView
    private lateinit var intervalSlider: Slider
    private lateinit var intervalValue: TextView
    private lateinit var storiesSlider: Slider
    private lateinit var storiesValue: TextView
    private lateinit var lengthGroup: MaterialButtonToggleGroup
    private lateinit var status: TextView
    private lateinit var preview: TextView
    private lateinit var progress: LinearProgressIndicator
    private lateinit var saveButton: MaterialButton
    private lateinit var testKeyButton: MaterialButton
    private lateinit var fetchModelsButton: MaterialButton
    private lateinit var getKeyButton: MaterialButton
    private lateinit var keyStatus: TextView
    private var availableModels: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        smartspacerId = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
            ?: FALLBACK_ID
        settings = SettingsRepository(this).forTarget(smartspacerId)

        topicField = findViewById(R.id.topicField)
        apiKeyField = findViewById(R.id.apiKeyField)
        languageField = findViewById(R.id.languageField)
        providerField = findViewById(R.id.providerField)
        baseUrlField = findViewById(R.id.baseUrlField)
        baseUrlLayout = findViewById(R.id.baseUrlLayout)
        apiKeyLayout = findViewById(R.id.apiKeyLayout)
        modelField = findViewById(R.id.modelField)
        intervalSlider = findViewById(R.id.intervalSlider)
        intervalValue = findViewById(R.id.intervalValue)
        storiesSlider = findViewById(R.id.storiesSlider)
        storiesValue = findViewById(R.id.storiesValue)
        lengthGroup = findViewById(R.id.lengthGroup)
        status = findViewById(R.id.statusText)
        preview = findViewById(R.id.previewText)
        progress = findViewById(R.id.progress)
        saveButton = findViewById(R.id.buttonSave)
        testKeyButton = findViewById(R.id.buttonTestKey)
        fetchModelsButton = findViewById(R.id.buttonFetchModels)
        keyStatus = findViewById(R.id.keyStatus)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        applyInsets()

        topicField.setText(settings.topic)
        languageField.setText(settings.language)

        providerField.setSimpleItems(AiProvider.entries.map { it.label }.toTypedArray())
        providerField.setText(settings.aiProvider.label, false)
        providerField.setOnItemClickListener { _, _, position, _ ->
            onProviderPicked(AiProvider.entries[position])
        }
        renderProvider(settings.aiProvider)

        intervalSlider.valueFrom = Constants.MIN_REFRESH_MINUTES.toFloat()
        intervalSlider.valueTo = Constants.MAX_REFRESH_MINUTES.toFloat()
        intervalSlider.stepSize = 15f
        intervalSlider.value = normalisedInterval()
        intervalSlider.addOnChangeListener { _, value, _ -> renderInterval(value.roundToInt()) }
        renderInterval(intervalSlider.value.roundToInt())

        storiesSlider.valueFrom = 1f
        storiesSlider.valueTo = 5f
        storiesSlider.stepSize = 1f
        storiesSlider.value = settings.maxStories.toFloat()
        storiesSlider.addOnChangeListener { _, value, _ -> renderStories(value.roundToInt()) }
        renderStories(settings.maxStories)

        lengthGroup.check(lengthButtonId(settings.storyLength))

        getKeyButton = findViewById(R.id.buttonGetKey)
        getKeyButton.setOnClickListener { openLink(settings.aiProvider.apiKeyUrl) }
        findViewById<MaterialButton>(R.id.buttonRepository).setOnClickListener {
            openLink(AiProvider.PROJECT_URL)
        }
        findViewById<TextView>(R.id.aboutVersion).text =
            getString(R.string.about_version, BuildConfig.VERSION_NAME)
        findViewById<MaterialButton>(R.id.buttonTestKey).setOnClickListener { testKey() }
        findViewById<MaterialButton>(R.id.buttonFetchModels).setOnClickListener { fetchModels() }
        findViewById<MaterialButton>(R.id.buttonRestore).setOnClickListener {
            settings.clearDismissed()
            notifyTarget()
            toast(getString(R.string.toast_restored))
            render()
        }
        saveButton.setOnClickListener { save() }

        render()
    }

    private fun normalisedInterval(): Float {
        val minutes = settings.refreshIntervalMinutes
            .coerceIn(Constants.MIN_REFRESH_MINUTES, Constants.MAX_REFRESH_MINUTES)
        return ((minutes / 15) * 15).coerceAtLeast(Constants.MIN_REFRESH_MINUTES).toFloat()
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

    private fun render() {
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
            else -> getString(R.string.status_never_generated)
        }

        val stories = settings.getVisibleStories()
        if (stories.isEmpty()) {
            preview.visibility = View.GONE
        } else {
            preview.visibility = View.VISIBLE
            preview.text = stories.joinToString("\n") { "• ${it.shortTitle}" }
        }
    }

    private fun persistFields() {
        settings.topic = topicField.text?.toString().orEmpty()
        settings.language = languageField.text?.toString()
            ?.takeIf { it.isNotBlank() } ?: settings.language
        // The provider itself is committed by onProviderPicked, so these always
        // land in the namespace of the provider currently on screen.
        settings.apiKey = apiKeyField.text?.toString().orEmpty()
        settings.baseUrl = baseUrlField.text?.toString()
            ?.takeIf { it.isNotBlank() } ?: settings.aiProvider.defaultBaseUrl
        settings.model = modelField.text?.toString()
            ?.takeIf { it.isNotBlank() } ?: settings.aiProvider.defaultModel
        settings.refreshIntervalMinutes = intervalSlider.value.roundToInt()
        settings.maxStories = storiesSlider.value.roundToInt()
        settings.storyLength = checkedLength()
    }

    private fun lengthButtonId(length: StoryLength) = when (length) {
        StoryLength.SHORT -> R.id.lengthShort
        StoryLength.MEDIUM -> R.id.lengthMedium
        StoryLength.LONG -> R.id.lengthLong
    }

    private fun checkedLength() = when (lengthGroup.checkedButtonId) {
        R.id.lengthShort -> StoryLength.SHORT
        R.id.lengthLong -> StoryLength.LONG
        else -> StoryLength.MEDIUM
    }

    /**
     * Switching backend swaps in that backend's own key, model and base URL,
     * so nothing typed for the previous one is lost.
     */
    private fun onProviderPicked(provider: AiProvider) {
        if (provider == settings.aiProvider) return
        // Persist the fields of the provider we are leaving first.
        persistFields()
        settings.aiProvider = provider
        renderProvider(provider)
        render()
    }

    /** Fills every provider dependent field and label. */
    private fun renderProvider(provider: AiProvider) {
        apiKeyField.setText(settings.apiKey)
        baseUrlField.setText(settings.baseUrl)

        apiKeyLayout.hint = getString(R.string.settings_key_label, provider.label)
        apiKeyLayout.helperText = if (provider.requiresKey) null else {
            getString(R.string.settings_key_optional)
        }
        baseUrlLayout.isVisible = provider.editableBaseUrl
        getKeyButton.text = getString(R.string.settings_get_key, provider.label)
        getKeyButton.isVisible = provider.apiKeyUrl.isNotBlank()

        availableModels = settings.cachedModels.ifEmpty { provider.fallbackModels }
        modelField.setSimpleItems(availableModels.toTypedArray())
        modelField.setText(settings.model, false)

        keyStatus.isVisible = false
    }

    /** Verifies the key by listing the models it can reach. */
    private fun testKey() {
        persistFields()
        if (!hasEndpoint()) return

        setLoading(true, R.string.settings_testing)
        lifecycleScope.launch {
            val result = AiClient.listModels(settings.toClientConfig())
            setLoading(false)
            result.fold(
                onSuccess = { models ->
                    if (models.isNotEmpty()) settings.cachedModels = models
                    showKeyStatus(getString(R.string.key_ok, models.size), isError = false)
                },
                onFailure = { error ->
                    showKeyStatus(
                        getString(R.string.key_failed, error.readableMessage()),
                        isError = true
                    )
                }
            )
        }
    }

    /** Pulls the real model list for this key into the dropdown. */
    private fun fetchModels() {
        persistFields()
        if (!hasEndpoint()) return

        setLoading(true, R.string.settings_fetching_models)
        lifecycleScope.launch {
            val result = AiClient.listModels(settings.toClientConfig())
            setLoading(false)
            result.fold(
                onSuccess = { models ->
                    if (models.isEmpty()) {
                        snack(getString(R.string.models_empty))
                        return@fold
                    }
                    settings.cachedModels = models
                    availableModels = models
                    modelField.setSimpleItems(models.toTypedArray())
                    val current = modelField.text?.toString()
                    if (current.isNullOrBlank() || !models.contains(current)) {
                        val preferred = models.firstOrNull { it == settings.aiProvider.defaultModel }
                            ?: models.firstOrNull { it.contains("flash") || it.contains("mini") }
                            ?: models.first()
                        modelField.setText(preferred, false)
                        settings.model = preferred
                    }
                    snack(getString(R.string.models_loaded, models.size))
                },
                onFailure = { error ->
                    showKeyStatus(
                        getString(R.string.key_failed, error.readableMessage()),
                        isError = true
                    )
                }
            )
        }
    }

    /** Guards the network buttons against an obviously incomplete setup. */
    private fun hasEndpoint(): Boolean {
        val provider = settings.aiProvider
        if (provider.requiresKey && settings.apiKey.isBlank()) {
            snack(getString(R.string.error_no_key))
            return false
        }
        if (settings.baseUrl.isBlank()) {
            snack(getString(R.string.error_no_base_url))
            return false
        }
        return true
    }

    private fun showKeyStatus(text: String, isError: Boolean) {
        keyStatus.isVisible = true
        keyStatus.text = text
        keyStatus.setTextColor(
            MaterialColors.getColor(
                keyStatus,
                if (isError) androidx.appcompat.R.attr.colorError
                else androidx.appcompat.R.attr.colorPrimary
            )
        )
        snack(text)
    }

    private fun Throwable.readableMessage() =
        message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName

    private fun save() {
        persistFields()
        if (settings.topic.isBlank()) {
            snack(getString(R.string.error_no_topic))
            return
        }
        if (!settings.isConfigured) {
            snack(
                if (settings.model.isBlank()) getString(R.string.error_no_model)
                else if (settings.baseUrl.isBlank()) getString(R.string.error_no_base_url)
                else getString(R.string.error_no_key)
            )
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            NewsUpdater.refresh(this@SettingsActivity, settings)
            setLoading(false)
            render()
            notifyTarget()
            val error = settings.lastError
            if (error != null) {
                toast(error)
            } else {
                toast(getString(R.string.toast_generated))
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun setLoading(loading: Boolean, labelRes: Int = R.string.settings_generating) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        saveButton.isEnabled = !loading
        testKeyButton.isEnabled = !loading
        fetchModelsButton.isEnabled = !loading
        saveButton.setText(if (loading) labelRes else R.string.settings_save)
    }

    private fun openLink(url: String) {
        if (url.isBlank()) return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { snack(getString(R.string.error_no_browser)) }
    }

    private fun snack(text: String) {
        Snackbar.make(findViewById(R.id.root), text, Snackbar.LENGTH_LONG).show()
    }

    private fun notifyTarget() {
        SmartspacerTargetProvider.notifyChange(this, NewsTarget::class.java, smartspacerId)
    }

    private fun toast(text: String) = snack(text)

    override fun onPause() {
        super.onPause()
        // Keep edits when the user leaves with the back gesture.
        if (isFinishing) {
            persistFields()
            notifyTarget()
        }
    }

    private fun applyInsets() {
        val appBar = findViewById<View>(R.id.appBar)
        val content = findViewById<View>(R.id.contentScroll)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            appBar.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            content.updatePadding(
                left = bars.left,
                right = bars.right,
                bottom = bars.bottom + (24 * resources.displayMetrics.density).toInt()
            )
            insets
        }
    }

    companion object {
        private const val FALLBACK_ID = "default"
    }
}
