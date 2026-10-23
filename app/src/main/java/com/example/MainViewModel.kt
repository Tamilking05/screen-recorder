package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.RecordingEntity
import com.example.data.repository.RecordingRepository
import com.example.service.CaptureSource
import com.example.service.RecordingSettings
import com.example.service.RecordingState
import com.example.service.ScreenRecorderManager
import com.example.util.DemoVideoSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenState {
    DASHBOARD,
    LIBRARY,
    PREVIEW,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecordingRepository(AppDatabase.getDatabase(application).recordingDao())

    val recordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentScreen = MutableStateFlow(ScreenState.DASHBOARD)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    private val _selectedRecording = MutableStateFlow<RecordingEntity?>(null)
    val selectedRecording: StateFlow<RecordingEntity?> = _selectedRecording.asStateFlow()

    val recorderSettings: StateFlow<RecordingSettings> = ScreenRecorderManager.settings
    val recordingState: StateFlow<RecordingState> = ScreenRecorderManager.recordingState
    val recordingDurationMs: StateFlow<Long> = ScreenRecorderManager.durationMs
    val micAmplitude: StateFlow<Float> = ScreenRecorderManager.amplitude
    val countdownRemaining: StateFlow<Int> = ScreenRecorderManager.countdownRemaining

    private val _isVipUnlocked = MutableStateFlow(false)
    val isVipUnlocked: StateFlow<Boolean> = _isVipUnlocked.asStateFlow()

    init {
        // Observe newly completed recording from ScreenRecorderManager
        viewModelScope.launch {
            ScreenRecorderManager.newlyRecordedEntity.collect { entity ->
                if (entity != null) {
                    _selectedRecording.value = entity
                    _currentScreen.value = ScreenState.PREVIEW
                    ScreenRecorderManager.clearNewlyRecorded()
                }
            }
        }
    }

    fun unlockVip() {
        _isVipUnlocked.value = true
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
    }

    fun selectRecordingForPreview(recording: RecordingEntity) {
        _selectedRecording.value = recording
        _currentScreen.value = ScreenState.PREVIEW
    }

    fun renameRecording(recording: RecordingEntity, newTitle: String) {
        viewModelScope.launch {
            repository.updateTitle(recording.id, newTitle)
            if (_selectedRecording.value?.id == recording.id) {
                _selectedRecording.value = _selectedRecording.value?.copy(title = newTitle)
            }
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
            if (_selectedRecording.value?.id == recording.id) {
                _selectedRecording.value = null
                _currentScreen.value = ScreenState.LIBRARY
            }
        }
    }

    fun clearAllRecordings() {
        viewModelScope.launch {
            repository.deleteAll(recordings.value)
            _selectedRecording.value = null
            _currentScreen.value = ScreenState.DASHBOARD
        }
    }

    fun toggleDarkMode() {
        ScreenRecorderManager.updateSettings { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun startRealRecording(context: Context, resultCode: Int, resultData: Intent) {
        ScreenRecorderManager.initiateRecording(context, resultCode, resultData) {
            // Once recording starts, keep user in dashboard or show HUD
        }
    }

    fun pauseRecording(context: Context) {
        ScreenRecorderManager.pauseRecording(context)
    }

    fun resumeRecording(context: Context) {
        ScreenRecorderManager.resumeRecording(context)
    }

    fun stopRecording(context: Context) {
        ScreenRecorderManager.stopRecording(context)
    }

    fun cancelCountdown() {
        ScreenRecorderManager.cancelCountdown()
    }

    fun generateSampleVideo(context: Context, source: CaptureSource) {
        viewModelScope.launch {
            val entity = DemoVideoSynthesizer.createSyntheticRecording(
                context = context,
                source = source,
                titlePrefix = "${source.label} Capture"
            )
            if (entity != null) {
                _selectedRecording.value = entity
                _currentScreen.value = ScreenState.PREVIEW
            }
        }
    }
}
