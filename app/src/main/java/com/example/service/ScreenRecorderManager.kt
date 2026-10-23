package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.RecordingEntity
import com.example.data.repository.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenRecorderManager {
    private const val TAG = "ScreenRecorderManager"

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _countdownRemaining = MutableStateFlow(0)
    val countdownRemaining: StateFlow<Int> = _countdownRemaining.asStateFlow()

    private val _settings = MutableStateFlow(RecordingSettings())
    val settings: StateFlow<RecordingSettings> = _settings.asStateFlow()

    private val _newlyRecordedEntity = MutableStateFlow<RecordingEntity?>(null)
    val newlyRecordedEntity: StateFlow<RecordingEntity?> = _newlyRecordedEntity.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var countdownJob: Job? = null

    var currentOutputFile: File? = null
    private var recordingStartTime = 0L
    private var totalPausedDuration = 0L
    private var pauseStartTime = 0L

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearNewlyRecorded() {
        _newlyRecordedEntity.value = null
    }

    fun updateSettings(updater: (RecordingSettings) -> RecordingSettings) {
        _settings.value = updater(_settings.value)
    }

    fun initiateRecording(
        context: Context,
        resultCode: Int,
        resultData: Intent,
        onCountdownFinished: () -> Unit
    ) {
        val countdown = _settings.value.countdownSeconds
        if (countdown > 0) {
            _recordingState.value = RecordingState.COUNTDOWN
            _countdownRemaining.value = countdown
            countdownJob?.cancel()
            countdownJob = coroutineScope.launch {
                for (i in countdown downTo 1) {
                    _countdownRemaining.value = i
                    delay(1000)
                }
                _countdownRemaining.value = 0
                startServiceRecording(context, resultCode, resultData)
                onCountdownFinished()
            }
        } else {
            startServiceRecording(context, resultCode, resultData)
            onCountdownFinished()
        }
    }

    private fun startServiceRecording(context: Context, resultCode: Int, resultData: Intent) {
        val serviceIntent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, resultData)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun onServiceRecordingStarted(outputFile: File) {
        currentOutputFile = outputFile
        recordingStartTime = System.currentTimeMillis()
        totalPausedDuration = 0L
        pauseStartTime = 0L
        _durationMs.value = 0L
        _recordingState.value = RecordingState.RECORDING

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (isActive && _recordingState.value == RecordingState.RECORDING) {
                val now = System.currentTimeMillis()
                _durationMs.value = now - recordingStartTime - totalPausedDuration
                delay(200)
            }
        }
    }

    fun pauseRecording(context: Context) {
        if (_recordingState.value == RecordingState.RECORDING) {
            val serviceIntent = Intent(context, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_PAUSE
            }
            context.startService(serviceIntent)
            _recordingState.value = RecordingState.PAUSED
            pauseStartTime = System.currentTimeMillis()
            timerJob?.cancel()
        }
    }

    fun resumeRecording(context: Context) {
        if (_recordingState.value == RecordingState.PAUSED) {
            val serviceIntent = Intent(context, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_RESUME
            }
            context.startService(serviceIntent)
            if (pauseStartTime > 0L) {
                totalPausedDuration += (System.currentTimeMillis() - pauseStartTime)
                pauseStartTime = 0L
            }
            _recordingState.value = RecordingState.RECORDING
            startTimer()
        }
    }

    fun stopRecording(context: Context) {
        if (_recordingState.value == RecordingState.RECORDING || _recordingState.value == RecordingState.PAUSED) {
            timerJob?.cancel()
            val serviceIntent = Intent(context, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_STOP
            }
            context.startService(serviceIntent)
            _recordingState.value = RecordingState.STOPPED
        }
    }

    fun updateAmplitude(amp: Float) {
        _amplitude.value = amp
    }

    fun onServiceRecordingComplete(context: Context, outputFile: File, duration: Long) {
        timerJob?.cancel()
        _recordingState.value = RecordingState.IDLE
        _durationMs.value = 0L
        _amplitude.value = 0f

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val finalDuration = if (duration > 0) duration else (outputFile.length() / 150_000L * 1000L).coerceAtLeast(1000L)
                val thumbPath = extractAndSaveThumbnail(context, outputFile)
                val timeFormat = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
                val currentSetting = _settings.value

                val entity = RecordingEntity(
                    title = "Recording ${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}",
                    filePath = outputFile.absolutePath,
                    thumbnailPath = thumbPath,
                    durationMs = finalDuration,
                    createdAt = System.currentTimeMillis(),
                    fileSizeBytes = outputFile.length(),
                    width = currentSetting.videoQuality.width,
                    height = currentSetting.videoQuality.height,
                    fps = currentSetting.videoQuality.fps,
                    hasAudio = currentSetting.isMicEnabled,
                    hasWebcam = currentSetting.isWebcamEnabled,
                    captureSource = currentSetting.captureSource.name
                )

                val repository = RecordingRepository(AppDatabase.getDatabase(context).recordingDao())
                val id = repository.saveRecording(entity)
                val savedEntity = entity.copy(id = id)
                _newlyRecordedEntity.value = savedEntity
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing recording", e)
                _errorMessage.value = "Failed to save recording: ${e.localizedMessage}"
            }
        }
    }

    private fun extractAndSaveThumbnail(context: Context, videoFile: File): String? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(500000) ?: retriever.frameAtTime
            retriever.release()

            if (bitmap != null) {
                val thumbFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                thumbFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract thumbnail", e)
            null
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _countdownRemaining.value = 0
        _recordingState.value = RecordingState.IDLE
    }
}
