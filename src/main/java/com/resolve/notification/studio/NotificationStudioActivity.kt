package com.resolve.notification.studio

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.resolve.notification.studio.core.NotificationChannelManager
import com.resolve.notification.studio.core.NotificationPermissionHelper
import com.resolve.notification.studio.databinding.ActivityNotificationStudioBinding
import com.resolve.notification.studio.style.NotificationStyle
import com.resolve.notification.studio.style.NotificationStyleApplier
import com.resolve.notification.studio.core.NotificationLayoutFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Notification Tailoring Studio — standalone app for live-previewing,
 * customising, and testing Resolve's custom notification layouts.
 */
class NotificationStudioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationStudioBinding
    private val viewModel: NotificationStudioViewModel by viewModels()
    private lateinit var channelManager: NotificationChannelManager
    private lateinit var debugPanel: NotificationDebugPanel

    private var previewView: View? = null

    private val chipRows =
        mutableListOf<Triple<List<Int>, List<GradientDrawable>, (StudioState) -> Int>>()
    private var chipSizePx = 0
    private var chipGapPx = 0
    private var chipStrokePx = 0

    private var isRefreshing = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        val (message, level) = if (isGranted) {
            "POST_NOTIFICATIONS granted" to LogLevel.INFO
        } else {
            "POST_NOTIFICATIONS denied — notifications will not be delivered" to LogLevel.WARN
        }
        viewModel.logEntry(message, level)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotificationStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelManager = NotificationChannelManager(applicationContext)
        debugPanel = NotificationDebugPanel(binding)

        setupToolbar()
        setupStyleSelector()
        setupFields()
        setupToggles()
        setupPreview()
        setupDebugPanel()
        setupActionButtons()
        setupCustomStyleSection()

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                updatePreview(state)
                updateDebugPanelIfVisible(state)
                binding.llCustomSection.visibility =
                    if (state.selectedStyle == NotificationStyle.CUSTOM) View.VISIBLE else View.GONE
                refreshCustomSection(state)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationPermissionHelper.isPostNotificationsGranted(this)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        viewModel.logEntry("Studio opened (API ${Build.VERSION.SDK_INT})")
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupStyleSelector() {
        binding.rgStyle.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
            val style = when (checkedId) {
                R.id.rb_minimal -> NotificationStyle.MINIMAL
                R.id.rb_system  -> NotificationStyle.SYSTEM
                R.id.rb_premium -> NotificationStyle.RESOLVE_PREMIUM
                R.id.rb_urgent  -> NotificationStyle.URGENT_ALERT
                R.id.rb_custom  -> NotificationStyle.CUSTOM
                else            -> NotificationStyle.SYSTEM
            }
            viewModel.setSelectedStyle(style)
        }
        binding.rbSystem.isChecked = true
    }

    private fun setupFields() {
        val initialState = viewModel.state.value
        binding.etTitle.setText(initialState.title)
        binding.etTimeText.setText(initialState.timeText)
        binding.etLocation.setText(initialState.location)
        binding.etLeaveBy.setText(initialState.leaveBy)
        binding.etPrepItems.setText(initialState.prepItems)

        binding.etTitle.addTextChangedListener(textWatcher { viewModel.setTitle(it) })
        binding.etTimeText.addTextChangedListener(textWatcher { viewModel.setTimeText(it) })
        binding.etLocation.addTextChangedListener(textWatcher { viewModel.setLocation(it) })
        binding.etLeaveBy.addTextChangedListener(textWatcher { viewModel.setLeaveBy(it) })
        binding.etPrepItems.addTextChangedListener(textWatcher { viewModel.setPrepItems(it) })
    }

    private fun setupToggles() {
        val s = viewModel.state.value
        binding.switchShowLocation.isChecked = s.showLocation
        binding.switchShowLeaveBy.isChecked = s.showLeaveBy
        binding.switchShowPrepItems.isChecked = s.showPrepItems
        binding.switchUrgentMode.isChecked = s.urgentMode
        binding.switchForceHeadsUp.isChecked = s.forceHeadsUp
        binding.switchSimulate5min.isChecked = s.simulate5MinLeft
        binding.switchChannelDisabled.isChecked = s.simulateChannelDisabled

        binding.switchShowLocation.setOnCheckedChangeListener { _, c -> viewModel.setShowLocation(c) }
        binding.switchShowLeaveBy.setOnCheckedChangeListener { _, c -> viewModel.setShowLeaveBy(c) }
        binding.switchShowPrepItems.setOnCheckedChangeListener { _, c -> viewModel.setShowPrepItems(c) }
        binding.switchUrgentMode.setOnCheckedChangeListener { _, c -> viewModel.setUrgentMode(c) }
        binding.switchForceHeadsUp.setOnCheckedChangeListener { _, c -> viewModel.setForceHeadsUp(c) }
        binding.switchSimulate5min.setOnCheckedChangeListener { _, c -> viewModel.setSimulate5MinLeft(c) }
        binding.switchChannelDisabled.setOnCheckedChangeListener { _, c -> viewModel.setSimulateChannelDisabled(c) }
    }

    private fun setupPreview() {
        inflatePreviewLayout(expanded = viewModel.state.value.isPreviewExpanded)

        binding.btnTogglePreview.setOnClickListener {
            val nowExpanded = !viewModel.state.value.isPreviewExpanded
            viewModel.setPreviewExpanded(nowExpanded)
            binding.btnTogglePreview.text = if (nowExpanded) "COMPACT" else "EXPANDED"
            binding.tvPreviewModeLabel.text = if (nowExpanded) "EXPANDED" else "COMPACT"
            inflatePreviewLayout(expanded = nowExpanded)
        }
    }

    private fun setupDebugPanel() {
        debugPanel.setToggleListener { viewModel.toggleDebugPanel() }
    }

    private fun setupActionButtons() {
        binding.btnSendNotification.setOnClickListener {
            viewModel.sendTestNotification(
                context = applicationContext,
                channelManager = channelManager,
                permHelper = NotificationPermissionHelper,
            )
        }

        binding.btnExportConfig.setOnClickListener {
            val config = viewModel.exportConfig()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText("NotificationStudio Config", config),
            )
            Toast.makeText(this, "Config copied to clipboard", Toast.LENGTH_SHORT).show()
            viewModel.logEntry("Config exported to clipboard")
        }
    }

    private fun inflatePreviewLayout(expanded: Boolean) {
        binding.previewContainer.removeAllViews()
        val layoutRes = if (expanded) {
            R.layout.notification_event_expanded
        } else {
            R.layout.notification_event_compact
        }
        previewView = LayoutInflater.from(this)
            .inflate(layoutRes, binding.previewContainer, false)
            .also { binding.previewContainer.addView(it) }

        updatePreview(viewModel.state.value)
    }

    private fun updatePreview(state: StudioState) {
        val view = previewView ?: return
        val params = buildNotificationParams(state)
        val styleConfig = NotificationStyleApplier.configFor(state.selectedStyle, state.customStyleConfig)
        NotificationLayoutFactory.bindToView(view, params, styleConfig)
    }

    private fun updateDebugPanelIfVisible(state: StudioState) {
        debugPanel.setExpanded(state.isDebugPanelExpanded)
        if (!state.isDebugPanelExpanded) return

        val channelId = if (state.urgentMode) {
            NotificationChannelManager.STUDIO_CHANNEL_ID_URGENT
        } else {
            NotificationChannelManager.STUDIO_CHANNEL_ID
        }

        debugPanel.updateSystemInfo(
            context = this,
            permHelper = NotificationPermissionHelper,
            channelManager = channelManager,
            channelId = channelId,
            lastNotifId = state.lastNotificationId,
            lastTimestamp = state.lastNotificationTimestamp,
        )
        debugPanel.updateLogs(state.debugLogs)
    }

    private fun buildNotificationParams(state: StudioState): NotificationLayoutFactory.NotificationParams {
        val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        return NotificationLayoutFactory.NotificationParams(
            title = state.title,
            timeText = state.timeText,
            location = state.location,
            leaveBy = state.leaveBy,
            prepItems = state.prepItems,
            showLocation = state.showLocation,
            showLeaveBy = state.showLeaveBy,
            showPrepItems = state.showPrepItems,
            urgentMode = state.urgentMode,
            simulate5MinLeft = state.simulate5MinLeft,
            currentTime = currentTime,
        )
    }

    private fun setupCustomStyleSection() {
        val density = resources.displayMetrics.density
        chipSizePx  = (28 * density).toInt()
        chipGapPx   = (6  * density).toInt()
        chipStrokePx = (2.5f * density).toInt()

        val initial = viewModel.state.value.customStyleConfig

        binding.etCustomHeaderText.setText(initial.headerText)
        binding.etCustomHeaderText.addTextChangedListener(textWatcher { text ->
            if (!isRefreshing) viewModel.setCustomHeaderText(text)
        })

        binding.etCustomBadgeText.setText(initial.urgencyBadgeText)
        binding.etCustomBadgeText.addTextChangedListener(textWatcher { text ->
            if (!isRefreshing) viewModel.setCustomUrgencyBadgeText(text)
        })

        listOf(
            binding.btnIconTriDown  to R.drawable.ic_system_down_triangle,
            binding.btnIconCircle   to R.drawable.ic_system_circle,
            binding.btnIconDiamond  to R.drawable.ic_system_diamond,
            binding.btnIconTriRight to R.drawable.ic_system_right_triangle,
            binding.btnIconStar     to R.drawable.ic_system_star,
        ).forEach { (btn, res) -> btn.setOnClickListener { viewModel.setCustomIconDrawable(res) } }

        buildChipRow(binding.llCustomBgChips,         { it.customStyleConfig.backgroundColor  }) { viewModel.setCustomBgColor(it) }
        buildChipRow(binding.llCustomAccentChips,     { it.customStyleConfig.accentColor       }) { viewModel.setCustomAccentColor(it) }
        buildChipRow(binding.llCustomTitleChips,      { it.customStyleConfig.titleColor        }) { viewModel.setCustomTitleColor(it) }
        buildChipRow(binding.llCustomTimeChips,       { it.customStyleConfig.timeColor         }) { viewModel.setCustomTimeColor(it) }
        buildChipRow(binding.llCustomHeaderTimeChips, { it.customStyleConfig.headerTimeColor   }) { viewModel.setCustomHeaderTimeColor(it) }
        buildChipRow(binding.llCustomLocationChips,   { it.customStyleConfig.locationColor     }) { viewModel.setCustomLocationColor(it) }
        buildChipRow(binding.llCustomLeaveByChips,    { it.customStyleConfig.leaveByColor      }) { viewModel.setCustomLeaveByColor(it) }
        buildChipRow(binding.llCustomPrepChips,       { it.customStyleConfig.prepColor         }) { viewModel.setCustomPrepColor(it) }
        buildChipRow(binding.llCustomDividerChips,    { it.customStyleConfig.dividerColor      }) { viewModel.setCustomDividerColor(it) }
        buildChipRow(binding.llCustomBadgeChips,      { it.customStyleConfig.urgencyBadgeColor }) { viewModel.setCustomUrgencyBadgeColor(it) }

        binding.sbCustomTitleSize.setOnSeekBarChangeListener(seekBarListener { sp ->
            viewModel.setCustomTitleSizeSp(sp.toFloat())
        })

        binding.sbCustomTimeSize.setOnSeekBarChangeListener(seekBarListener { sp ->
            viewModel.setCustomTimeSizeSp(sp.toFloat())
        })

        binding.btnCustomTitleStart.setOnClickListener  { viewModel.setCustomTitleGravity(Gravity.START) }
        binding.btnCustomTitleCenter.setOnClickListener { viewModel.setCustomTitleGravity(Gravity.CENTER_HORIZONTAL) }

        binding.btnCustomTimeStart.setOnClickListener  { viewModel.setCustomTimeGravity(Gravity.START) }
        binding.btnCustomTimeCenter.setOnClickListener { viewModel.setCustomTimeGravity(Gravity.CENTER_HORIZONTAL) }

        binding.switchCustomTitleBold.isChecked = initial.titleBold
        binding.switchCustomTitleBold.setOnCheckedChangeListener { _, checked ->
            if (!isRefreshing) viewModel.setCustomTitleBold(checked)
        }

        binding.btnCustomReset.setOnClickListener { viewModel.resetCustomConfig() }
    }

    private fun buildChipRow(
        container: LinearLayout,
        getColor: (StudioState) -> Int,
        onPick: (Int) -> Unit,
    ) {
        val palette = NotificationStyleApplier.STUDIO_PALETTE
        val drawables = mutableListOf<GradientDrawable>()

        palette.forEachIndexed { _, color ->
            val displayColor = if (color == Color.TRANSPARENT) 0xFF2A2A2AL.toInt() else color

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(displayColor)
            }
            val chip = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(chipSizePx, chipSizePx).apply {
                    marginEnd = chipGapPx
                }
                background = drawable
                contentDescription = if (color == Color.TRANSPARENT) "Transparent"
                else "#${Integer.toHexString(color and 0xFFFFFF).uppercase().padStart(6, '0')}"
                setOnClickListener { onPick(color) }
            }
            container.addView(chip)
            drawables.add(drawable)
        }

        chipRows.add(Triple(palette, drawables, getColor))
    }

    private fun refreshCustomSection(state: StudioState) {
        if (state.selectedStyle != NotificationStyle.CUSTOM) return
        val config = state.customStyleConfig

        isRefreshing = true
        try {
            chipRows.forEach { (palette, drawables, getColor) ->
                val selected = getColor(state)
                palette.forEachIndexed { i, color ->
                    drawables[i].setStroke(
                        if (color == selected) chipStrokePx else 0,
                        Color.WHITE,
                    )
                }
            }

            val titleSp = config.titleTextSizeSp.toInt()
            if (binding.sbCustomTitleSize.progress != titleSp) binding.sbCustomTitleSize.progress = titleSp
            binding.tvCustomTitleSizeValue.text = "${titleSp}sp"

            val timeSp = config.timeTextSizeSp.toInt()
            if (binding.sbCustomTimeSize.progress != timeSp) binding.sbCustomTimeSize.progress = timeSp
            binding.tvCustomTimeSizeValue.text = "${timeSp}sp"

            binding.btnCustomTitleStart.alpha  = if (config.titleGravity == Gravity.START)              1f else 0.35f
            binding.btnCustomTitleCenter.alpha = if (config.titleGravity == Gravity.CENTER_HORIZONTAL)  1f else 0.35f
            binding.btnCustomTimeStart.alpha   = if (config.timeBodyGravity == Gravity.START)           1f else 0.35f
            binding.btnCustomTimeCenter.alpha  = if (config.timeBodyGravity == Gravity.CENTER_HORIZONTAL) 1f else 0.35f

            listOf(
                binding.btnIconTriDown  to R.drawable.ic_system_down_triangle,
                binding.btnIconCircle   to R.drawable.ic_system_circle,
                binding.btnIconDiamond  to R.drawable.ic_system_diamond,
                binding.btnIconTriRight to R.drawable.ic_system_right_triangle,
                binding.btnIconStar     to R.drawable.ic_system_star,
            ).forEach { (btn, res) ->
                btn.alpha = if (config.iconDrawableRes == res) 1f else 0.35f
            }

            if (binding.switchCustomTitleBold.isChecked != config.titleBold) {
                binding.switchCustomTitleBold.isChecked = config.titleBold
            }

            val currentHeader = binding.etCustomHeaderText.text?.toString().orEmpty()
            if (currentHeader != config.headerText) binding.etCustomHeaderText.setText(config.headerText)

            val currentBadge = binding.etCustomBadgeText.text?.toString().orEmpty()
            if (currentBadge != config.urgencyBadgeText) binding.etCustomBadgeText.setText(config.urgencyBadgeText)

        } finally {
            isRefreshing = false
        }
    }

    private fun seekBarListener(onProgress: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onProgress(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) = Unit
            override fun onStopTrackingTouch(sb: SeekBar?) = Unit
        }
    }

    private fun textWatcher(onChanged: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                onChanged(s?.toString().orEmpty())
            }
        }
    }
}
