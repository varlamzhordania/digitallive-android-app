package com.digitallive.leddisplay.ui

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.digitallive.leddisplay.MainApplication.Companion.appContext
import com.digitallive.leddisplay.R
import com.digitallive.leddisplay.api.DisplayWebSocketListener
import com.digitallive.leddisplay.data.Constants.getDisplayUrl
import com.digitallive.leddisplay.databinding.ActivityVideoBinding
import com.digitallive.leddisplay.models.Ticker
import com.digitallive.leddisplay.models.VideoData
import com.digitallive.leddisplay.utils.DataManager
import com.digitallive.leddisplay.utils.FileUtils.deleteOtherVideoFiles
import com.digitallive.leddisplay.utils.FileUtils.getUniqueFileName
import com.digitallive.leddisplay.utils.JsonUtils.extractVideoDataFromJson
import com.digitallive.leddisplay.utils.LogUtils.LogLevel
import com.digitallive.leddisplay.utils.LogUtils.sendLog
import com.digitallive.leddisplay.utils.NetworkMonitor
import com.digitallive.leddisplay.utils.TextUtils.getProgressDisplayLine
import com.digitallive.leddisplay.utils.TimeUtils.isWithinTimeRange
import com.downloader.OnDownloadListener
import com.downloader.OnProgressListener
import com.downloader.PRDownloader
import com.downloader.Progress
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONObject
import java.io.File
import java.net.SocketException

@Suppress("DEPRECATION")
class VideoActivity : BaseActivity() {

    private lateinit var binding: ActivityVideoBinding
    private var isActivityPaused = false
    private var backPressedTime: Long = 0
    private val backPressThreshold = 1000
    private val handler = Handler(Looper.getMainLooper())
    private var backToastRunnable: Runnable? = null

    private var networkMonitor = NetworkMonitor()
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var updateJob: Job? = null
    private var socketDisconnected = false

    private var player: ExoPlayer? = null
    private var videoFile = File(appContext.filesDir, DataManager.getVideoFileName())
    private var currentVideoData: VideoData? = DataManager.getVideoData()
    private var repeatCount = 0
    private var maxRepeats = 3

    private var retrySocketJob: Job? = null
    private val retrySocketIntervalMillis = DataManager.getRetryInterval() * 60 * 1000L
    private val retrySocketCoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var retryDownloadJob: Job? = null
    private val retryDownloadIntervalMillis = DataManager.getRetryInterval() * 60 * 1000L
    private val retryDownloadCoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var tickerJob: Job? = null
    private val tickerIntervalMillis = 10_000L
    private val tickerCoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val showError = DataManager.getShowError()

    private var previousValidHeadlines: List<String> = emptyList()
    private val tickerDivider = "  •  "

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActivity()

        setNetworkMonitor()

        DataManager.getDisplaySecretKey()?.let { key ->
            initializeWebSocket(key)
        }

        currentVideoData?.let {
            showPlayerAndHideOthers()
            initializePlayer(it.loop, it.paused, it.zoomToFill)
            startTickerUpdater()
        }
    }

    private fun setupActivity() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        //        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.tickerContainer.background = DataManager.getTickerBackgroundColor().toDrawable()
        binding.tickerView.background = Color.TRANSPARENT.toDrawable()
    }

    private fun setNetworkMonitor() {
        networkMonitor.setOnNetworkAvailable {
            DataManager.getDisplaySecretKey()?.let { key ->
                initializeWebSocket(key)
            }
        }
    }

    private fun initializeWebSocket(key: String) {
        if (key.isEmpty()) return

        webSocket?.close(1000, "Reinitializing connection")
        webSocket = null

        val request = Request.Builder().url(getDisplayUrl()).build()

        val listener = DisplayWebSocketListener(
            onConnectionOpened = { webSocket, response ->
                Log.i("WebSocket", "Connection successful")
                sendLog(LogLevel.INFO, "WebSocket connection successful")
            },
            onMessageReceived = { webSocket, text ->
                retrySocketJob?.cancel()
                retrySocketJob = null

                val jsonResponse = JSONObject(text)
                val action = jsonResponse.optString("action")

                if (action == "get_display_data" || action == "display_update") {
                    val message = jsonResponse.optJSONObject("message")

                    Log.d("WebSocket", "Received message: ${message?.toString(4)}")
                    sendLog(LogLevel.DEBUG, "Received message:\n${message?.toString(4)}")

                    message?.let {
                        val videoData = extractVideoDataFromJson(it)

                        runOnUiThread {
                            binding.noVideo.visibility = View.GONE
                            updatePlayerSettings(videoData)
                        }
                    }
                } else {
                    Log.w("WebSocket", "Received unknown action: $action")
                    sendLog(LogLevel.WARNING, "Received unknown action: $action")
                }
            },
            onConnectionFailure = { webSocket, throwable, response ->
                if (!(throwable is SocketException && throwable.message?.contains(
                        "Software caused connection abort",
                        ignoreCase = true
                    ) == true)
                ) {
                    if (!videoFile.exists()) {
                        runOnUiThread {
                            binding.playerContainer.visibility = View.GONE
                            binding.progressContainer.visibility = View.VISIBLE
                            binding.errorText.text = getString(
                                R.string.connection_error,
                                throwable.message
                            )
                            binding.errorText.visibility = View.VISIBLE
                        }
                    }

                    Log.e("WebSocket", "Connection Failure", throwable)
                    sendLog(LogLevel.ERROR, "WebSocket connection error: ${throwable.message}")

                    if (showError) {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                getString(R.string.connection_error, throwable.message),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    startWebSocketRetry(key)
                } else {
                    socketDisconnected = true
                }
            }
        )

        client = OkHttpClient()
        webSocket = client?.newWebSocket(request, listener)
    }

    private fun updatePlayerSettings(videoData: VideoData) {
        var lastUpdateTime = 0L

        PRDownloader.cancelAll()
        updateJob?.cancel()

        fun updateViewFromVideoData() {
            currentVideoData = videoData
            DataManager.saveVideoData(videoData)

            startTickerUpdater()

            runOnUiThread {
                if (videoData.isActive) {
                    showPlayerAndHideOthers()
                    initializePlayer(
                        videoData.loop,
                        videoData.paused,
                        videoData.zoomToFill
                    )
                } else {
                    setDisplayInactive()
                }
            }
        }

        updateJob = CoroutineScope(Dispatchers.IO).launch {
            if (currentVideoData?.currentVideo != videoData.currentVideo || !videoFile.exists()) {
                Log.i("MainActivity", "Updating video: ${videoData.currentVideo}")
                sendLog(LogLevel.INFO, "Updating video: ${videoData.currentVideo}")

                val uri = videoData.currentVideo.toUri()
                var fileName = getUniqueFileName(
                    filesDir,
                    uri.lastPathSegment ?: videoData.currentVideo.substringAfterLast("/")
                )

                if (!videoFile.exists()) {
                    runOnUiThread {
                        binding.playerContainer.visibility = View.GONE
                        binding.progressContainer.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.VISIBLE
                        binding.errorText.visibility = View.VISIBLE
                        binding.progressBar.progress = 0
                        binding.errorText.text = getString(R.string.downloading_video)
                    }
                }

                PRDownloader.download(
                    videoData.currentVideo,
                    filesDir.absolutePath,
                    fileName
                )
                    .build()
                    .setOnProgressListener(OnProgressListener { progress: Progress ->
                        val currentTime = SystemClock.elapsedRealtime()
                        if (currentTime - lastUpdateTime >= 1000) {
                            val progressPercent =
                                progress.currentBytes * 100 / progress.totalBytes
                            val progressText: String = getProgressDisplayLine(
                                progress.currentBytes, progress.totalBytes
                            )

                            runOnUiThread {
                                binding.progressBar.progress = progressPercent.toInt()
                                binding.errorText.text = progressText
                            }

                            Log.i("MainActivity", "Progress: $progressText")
                            sendLog(LogLevel.INFO, "Download progress: $progressText")

                            lastUpdateTime = currentTime
                        }
                    })
                    .start(object : OnDownloadListener {
                        override fun onDownloadComplete() {
                            retryDownloadJob?.cancel()
                            retryDownloadJob = null

                            player?.release()
                            player = null

                            videoFile = File(filesDir, fileName)
                            DataManager.saveVideoFileName(fileName)

                            deleteOtherVideoFiles(this@VideoActivity, fileName)

                            updateViewFromVideoData()
                        }

                        override fun onError(error: com.downloader.Error) {
                            if (!videoFile.exists()) {
                                runOnUiThread {
                                    player?.release()
                                    player = null

                                    binding.playerContainer.visibility = View.GONE
                                    binding.progressContainer.visibility = View.VISIBLE
                                    binding.errorText.text = getString(
                                        R.string.download_error,
                                        error.connectionException
                                    )
                                    binding.errorText.visibility = View.VISIBLE
                                }
                            }

                            startDownloadRetry(videoData)

                            Log.e("MainActivity", "Download error: ${error.connectionException}")
                            sendLog(
                                LogLevel.ERROR, "Response code: ${error.responseCode}\n" +
                                        if (error.isServerError) {
                                            "Server error: ${error.serverErrorMessage}"
                                        } else if (error.isConnectionError) {
                                            "Connection error: ${error.connectionException?.message}"
                                        } else {
                                            "Download error: ${error.connectionException}"
                                        }
                            )

                            if (showError) {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@VideoActivity,
                                        getString(
                                            R.string.download_error,
                                            error.connectionException
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    })
            } else {
                updateViewFromVideoData()
            }
        }
    }

    private fun showPlayerAndHideOthers() {
        runOnUiThread {
            binding.playerContainer.visibility = View.VISIBLE
            binding.progressContainer.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            binding.errorText.visibility = View.GONE
        }
    }

    private fun setDisplayInactive() {
        player?.release()
        player = null

        binding.playerContainer.visibility = View.GONE
        binding.progressContainer.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.errorText.text = getString(R.string.display_is_inactive)
        binding.errorText.visibility = View.VISIBLE
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                repeatCount++

                if ((maxRepeats == -1 || repeatCount <= maxRepeats) &&
                    currentVideoData?.paused == false &&
                    !isActivityPaused
                ) {
                    player?.seekTo(0)
                }
            }
        }
    }

    private fun initializePlayer(loop: Int, paused: Boolean, zoom: Boolean) {
        runOnUiThread {
            if (player == null ||
                binding.playerView.player == null ||
                binding.playerView.player!!.currentMediaItem != MediaItem.fromUri(videoFile.toUri())
            ) {
                player = ExoPlayer.Builder(this).build().also {
                    binding.playerView.player = it
                }
            }

            binding.playerView.player?.also {
                if (videoFile.exists() && currentVideoData?.isActive == true) {
                    if (it.currentMediaItem != MediaItem.fromUri(videoFile.toUri())) {
                        it.stop()
                        it.clearMediaItems()
                        it.setMediaItem(MediaItem.fromUri(videoFile.toUri()))
                        it.prepare()
                    } else if (it.playbackState == Player.STATE_ENDED) {
                        it.seekTo(0)
                    }

                    it.repeatMode = Player.REPEAT_MODE_OFF
                    binding.playerView.resizeMode =
                        if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT

                    if (!paused && !isActivityPaused) {
                        it.play()
                    } else {
                        it.pause()
                    }

                    try {
                        player?.removeListener(playerListener)
                    } catch (_: Exception) {
                    }
                    player?.addListener(playerListener)

                    repeatCount = 0
                    maxRepeats = loop
                } else {
                    player?.release()
                    player = null

                    if (currentVideoData?.isActive == false) {
                        setDisplayInactive()
                    } else {
                        binding.playerContainer.visibility = View.GONE
                        binding.progressContainer.visibility = View.GONE
                        binding.noVideo.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun startTickerMovement(headlines: List<String>) {
        binding.tickerView.removeAllViews()

        val tickerChildViews = mutableListOf<View?>()
        for (index in 0 until headlines.size) {
            tickerChildViews.add(createTickerText(headlines[index]))
            if (index < headlines.size - 1) {
                tickerChildViews.add(createTickerText(tickerDivider))
            }
        }
        binding.tickerView.setChildViews(tickerChildViews)

        binding.tickerView.setDisplacement(100)
        binding.tickerView.showTickers()
        binding.tickerView.resetScrollPosition()
    }

    private fun createTickerText(text: String): TextView {
        return TextView(this).apply {
            setLayoutParams(
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
            this.text = text
            setTextColor(DataManager.getTickerTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
    }

    private fun startWebSocketRetry(key: String) {
        retrySocketJob?.cancel()
        retrySocketJob = retrySocketCoroutineScope.launch {
            while (isActive) {
                delay(retrySocketIntervalMillis)
                Log.d("WebSocket", "Retrying WebSocket connection...")
                sendLog(LogLevel.DEBUG, "Retrying WebSocket connection...")
                initializeWebSocket(key)
            }
        }
    }

    private fun startDownloadRetry(videoData: VideoData) {
        retryDownloadJob?.cancel()
        retryDownloadJob = retryDownloadCoroutineScope.launch {
            while (isActive) {
                delay(retryDownloadIntervalMillis)
                Log.d("WebSocket", "Retrying video download...")
                sendLog(LogLevel.DEBUG, "Retrying video download...")
                updatePlayerSettings(videoData)
            }
        }
    }

    private fun startTickerUpdater() {
        tickerJob?.cancel()

        tickerJob = tickerCoroutineScope.launch {
            while (isActive) {
                currentVideoData?.let {
                    updateTickerView(it.tickers)
                }
                delay(tickerIntervalMillis)
            }
        }
    }

    private fun stopTickerUpdater() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateTickerView(tickers: List<Ticker>) {
        val validHeadlines = tickers
            .filter { isWithinTimeRange(it.startTime, it.endTime) }
            .flatMap { it.items }
            .filter { it.isActive && it.content.isNotEmpty() }
            .sortedBy { it.order }
            .map { it.content }

        if (validHeadlines != previousValidHeadlines) {
            previousValidHeadlines = validHeadlines

            runOnUiThread {
                if (validHeadlines.isNotEmpty()) {
                    binding.tickerContainer.visibility = View.VISIBLE
                    startTickerMovement(validHeadlines)
                } else {
                    binding.tickerContainer.visibility = View.GONE
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        binding.tickerView.post {
            binding.tickerView.showTickers()
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - backPressedTime < backPressThreshold) {
            backToastRunnable?.let { handler.removeCallbacks(it) }
            super.onBackPressed()
        } else {
            backPressedTime = currentTime
            backToastRunnable = Runnable {
                Toast.makeText(
                    this,
                    getString(R.string.press_back_twice_to_exit), Toast.LENGTH_SHORT
                ).show()
            }
            handler.postDelayed(backToastRunnable!!, 800)
        }
    }

    override fun onPause() {
        super.onPause()

        isActivityPaused = true
        player?.pause()
        stopTickerUpdater()
    }

    override fun onResume() {
        super.onResume()

        isActivityPaused = false
        networkMonitor.register()

        currentVideoData?.let {
            startTickerUpdater()
        }

        if (currentVideoData?.paused == false) {
            player?.play()
        }

        if (socketDisconnected) {
            socketDisconnected = false
            DataManager.getDisplaySecretKey()?.let { key ->
                initializeWebSocket(key)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        player?.release()
        webSocket?.close(1000, "Goodbye!")
        client?.dispatcher?.executorService?.shutdown()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        networkMonitor.unregister()
        stopTickerUpdater()
    }
}