package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdManager {
    private const val TAG = "AdManager"

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private val _isAppOpenReady = MutableStateFlow(false)
    val isAppOpenReady: StateFlow<Boolean> = _isAppOpenReady.asStateFlow()

    private val _adStatusMessage = MutableStateFlow<String?>(null)
    val adStatusMessage: StateFlow<String?> = _adStatusMessage.asStateFlow()

    fun initialize(context: Context, onInitialized: (() -> Unit)? = null) {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "MobileAds initialized: ${status.adapterStatusMap}")
            // Preload ads once initialized
            loadInterstitialAd(context)
            loadRewardedAd(context)
            loadAppOpenAd(context)
            onInitialized?.invoke()
        }
    }

    // --- Interstitial Ad ---
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdsConfig.currentInterstitialAdId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "InterstitialAd loaded successfully")
                    interstitialAd = ad
                    isInterstitialLoading = false
                    _isInterstitialReady.value = true
                    _adStatusMessage.value = "Interstitial Ad Ready"
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "InterstitialAd failed to load: ${error.message}")
                    interstitialAd = null
                    isInterstitialLoading = false
                    _isInterstitialReady.value = false
                    _adStatusMessage.value = "Interstitial: ${error.message}"
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    loadInterstitialAd(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    loadInterstitialAd(activity)
                    onDismissed()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitialAd(activity)
            onDismissed()
        }
    }

    // --- Rewarded Ad ---
    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AdsConfig.currentRewardedAdId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "RewardedAd loaded successfully")
                    rewardedAd = ad
                    isRewardedLoading = false
                    _isRewardedReady.value = true
                    _adStatusMessage.value = "Rewarded Ad Ready"
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "RewardedAd failed to load: ${error.message}")
                    rewardedAd = null
                    isRewardedLoading = false
                    _isRewardedReady.value = false
                    _adStatusMessage.value = "Rewarded: ${error.message}"
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (RewardItem) -> Unit,
        onDismissed: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed")
                    rewardedAd = null
                    _isRewardedReady.value = false
                    loadRewardedAd(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                    rewardedAd = null
                    _isRewardedReady.value = false
                    loadRewardedAd(activity)
                    onDismissed()
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onUserEarnedReward(rewardItem)
            }
        } else {
            loadRewardedAd(activity)
            onDismissed()
        }
    }

    // --- App Open Ad ---
    fun loadAppOpenAd(context: Context) {
        if (appOpenAd != null || isAppOpenLoading) return
        isAppOpenLoading = true

        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AdsConfig.currentAppOpenAdId,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "AppOpenAd loaded successfully")
                    appOpenAd = ad
                    isAppOpenLoading = false
                    _isAppOpenReady.value = true
                    _adStatusMessage.value = "App Open Ad Ready"
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "AppOpenAd failed to load: ${error.message}")
                    appOpenAd = null
                    isAppOpenLoading = false
                    _isAppOpenReady.value = false
                    _adStatusMessage.value = "App Open: ${error.message}"
                }
            }
        )
    }

    fun showAppOpenAd(activity: Activity, onDismissed: () -> Unit) {
        val ad = appOpenAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "App open ad dismissed")
                    appOpenAd = null
                    _isAppOpenReady.value = false
                    loadAppOpenAd(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "App open ad failed to show: ${adError.message}")
                    appOpenAd = null
                    _isAppOpenReady.value = false
                    loadAppOpenAd(activity)
                    onDismissed()
                }
            }
            ad.show(activity)
        } else {
            loadAppOpenAd(activity)
            onDismissed()
        }
    }
}
