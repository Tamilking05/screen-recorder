package com.example.service

enum class CaptureSource(val label: String, val description: String) {
    FULL_SCREEN("Entire Screen", "Record full device display and apps"),
    BROWSER_TAB("Browser Tab", "Record interactive web view and embedded tabs")
}

enum class VideoQuality(val label: String, val width: Int, val height: Int, val bitrate: Int, val fps: Int) {
    FHD_1080P("1080p FHD", 1920, 1080, 8_000_000, 60),
    HD_720P("720p HD", 1280, 720, 4_000_000, 30),
    SD_480P("480p SD", 854, 480, 2_000_000, 30)
}

enum class AudioSourceOption(val label: String) {
    MIC("Microphone", ),
    SYSTEM("System Audio"),
    MIC_AND_SYSTEM("Mic + System"),
    MUTE("Mute Audio")
}

enum class WebcamShape {
    CIRCLE,
    ROUNDED_RECT
}

enum class WebcamSize(val sizeDp: Int) {
    SMALL(100),
    MEDIUM(140),
    LARGE(180)
}

data class RecordingSettings(
    val captureSource: CaptureSource = CaptureSource.FULL_SCREEN,
    val videoQuality: VideoQuality = VideoQuality.HD_720P,
    val audioOption: AudioSourceOption = AudioSourceOption.MIC,
    val isMicEnabled: Boolean = true,
    val isSystemAudioEnabled: Boolean = false,
    val isWebcamEnabled: Boolean = false,
    val webcamShape: WebcamShape = WebcamShape.CIRCLE,
    val webcamSize: WebcamSize = WebcamSize.MEDIUM,
    val countdownSeconds: Int = 3,
    val isDarkMode: Boolean = true
)

enum class RecordingState {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    STOPPED
}
