package com.wangyiheng.vcamsx.utils

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.widget.Toast
import com.wangyiheng.vcamsx.MainHook.Companion.c2_reader_Surfcae
import com.wangyiheng.vcamsx.MainHook.Companion.context
import com.wangyiheng.vcamsx.MainHook.Companion.oriHolder
import com.wangyiheng.vcamsx.MainHook.Companion.original_c1_preview_SurfaceTexture
import com.wangyiheng.vcamsx.MainHook.Companion.original_preview_Surface
import com.wangyiheng.vcamsx.utils.InfoProcesser.videoStatus
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object VideoPlayer {
    var c2_hw_decode_obj: VideoToFrames? = null
    var ijkMediaPlayer: IjkMediaPlayer? = null
    var mediaPlayer: MediaPlayer? = null
    var c3_player: MediaPlayer? = null
    var copyReaderSurface: Surface? = null
    var currentRunningSurface: Surface? = null
    private val scheduledExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    init {
        // Initialization code...
        startTimerTask()
    }

    // Start scheduled task
    private fun startTimerTask() {
        scheduledExecutor.scheduleWithFixedDelay({
            // Code executed every five seconds
            performTask()
        }, 10, 10, TimeUnit.SECONDS)
    }

    // Actual task executed
    private fun performTask() {
        restartMediaPlayer()
    }

    fun restartMediaPlayer() {
        if (videoStatus?.isVideoEnable == true || videoStatus?.isLiveStreamingEnabled == true) return
        if (currentRunningSurface == null || currentRunningSurface?.isValid == false) return
        releaseMediaPlayer()
    }

    // Common configuration method
    private fun configureMediaPlayer(mediaPlayer: IjkMediaPlayer) {
        mediaPlayer.apply {
            // Common error listener
            setOnErrorListener { _, what, extra ->
                Toast.makeText(context, "Playback error: $what", Toast.LENGTH_SHORT).show()
                true
            }

            // Common info listener
            setOnInfoListener { _, what, extra ->
                true
            }
        }
    }

    // RTMP stream player initialization
    fun initRTMPStreamPlayer() {
        ijkMediaPlayer = IjkMediaPlayer().apply {
            // Hardware decoding settings
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1)

            // Buffering settings
            setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_mpeg4", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "analyzemaxduration", 5000L)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "probesize", 2048L)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "flush_packets", 1L)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0L)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1L)

            Toast.makeText(context, videoStatus!!.liveURL, Toast.LENGTH_SHORT).show()

            // Apply common configuration
            configureMediaPlayer(this)

            // Set RTMP stream URL
            dataSource = videoStatus!!.liveURL

            // Prepare player asynchronously
            prepareAsync()

            // Operation after preparation
            setOnPreparedListener {
                original_preview_Surface?.let { setSurface(it) }
                Toast.makeText(context, "Live stream received successfully", Toast.LENGTH_SHORT).show()
                start()
            }
        }
    }


    fun initMediaPlayer(surface: Surface) {
        val volume = if (videoStatus?.volume == true) 1F else 0F
        val videoUrl = "content://com.wangyiheng.vcamsx.videoprovider"
        val videoPathUri = Uri.parse(videoUrl)
        mediaPlayer = MediaPlayer().apply {
            try {
                isLooping = true
                if (surface.isValid) {
                    setSurface(surface)
                } else {
                    Toast.makeText(context!!, "Initialization error: surface", Toast.LENGTH_LONG).show()
                }
                setDataSource(context!!, videoPathUri)
                prepareAsync()
                setVolume(volume, volume)
                setOnPreparedListener {
                    start()
                }
                setOnPreparedListener { start() }
                setOnErrorListener { mp, what, extra ->
                    // Handle playback error
                    true
                }
            } catch (e: Exception) {
                Toast.makeText(context!!, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun initializeTheStateAsWellAsThePlayer() {
        InfoProcesser.initStatus()

        if (ijkMediaPlayer == null) {
            if (videoStatus?.isLiveStreamingEnabled == true) {
                initRTMPStreamPlayer()
            }
        }
    }

    // Pass surface for playback
    private fun handleMediaPlayer(surface: Surface) {
        try {
            // Data initialization
            InfoProcesser.initStatus()
            // Player initialization
            // Destroy current player
            releaseMediaPlayer()

            videoStatus?.also { status ->
                if (!status.isVideoEnable && !status.isLiveStreamingEnabled) return

                val volume = if (status.volume) 1F else 0F

                when {
                    status.isLiveStreamingEnabled -> {
                        ijkMediaPlayer?.let {
                            it.setVolume(volume, volume)
                            it.setSurface(surface)
                        }
                    }

                    else -> {
                        mediaPlayer?.also {
                            if (it.isPlaying) {
                                it.setVolume(volume, volume)
                                it.setSurface(surface)
                            } else {
                                releaseMediaPlayer()
                                initMediaPlayer(surface)
                            }
                        } ?: run {
                            releaseMediaPlayer()
                            initMediaPlayer(surface)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // You can add more detailed exception handling or logging here
            logError("MediaPlayer Error", e)
        }
    }

    private fun logError(message: String, e: Exception) {
        // Implement logging logic, such as using Android's Log.e function
        Log.e("MediaPlayerHandler", "$message: ${e.message}")
    }

    fun releaseMediaPlayer() {
        if (mediaPlayer == null) return
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun camera2Play() {
        // Surface with name
        original_preview_Surface?.let { surface ->
            handleMediaPlayer(surface)
        }

        // Surface with name = null
        c2_reader_Surfcae?.let { surface ->
            c2_reader_play(surface)
        }
    }

    fun c1_camera_play() {
        if (original_c1_preview_SurfaceTexture != null) {
            original_preview_Surface = Surface(original_c1_preview_SurfaceTexture)
            if (original_preview_Surface!!.isValid == true) {
                handleMediaPlayer(original_preview_Surface!!)
            }
        }

        if (oriHolder?.surface != null) {
            original_preview_Surface = oriHolder?.surface
            if (original_preview_Surface!!.isValid == true) {
                handleMediaPlayer(original_preview_Surface!!)
            }
        }

        c2_reader_Surfcae?.let { surface ->
            c2_reader_play(surface)
        }
    }

    fun c2_reader_play(c2_reader_Surfcae: Surface) {
        if (c2_reader_Surfcae == copyReaderSurface) {
            return
        }

        copyReaderSurface = c2_reader_Surfcae

        if (c2_hw_decode_obj != null) {
            c2_hw_decode_obj!!.stopDecode()
            c2_hw_decode_obj = null
        }

        c2_hw_decode_obj = VideoToFrames()
        try {
            val videoUrl = "content://com.wangyiheng.vcamsx.videoprovider"
            val videoPathUri = Uri.parse(videoUrl)
            c2_hw_decode_obj!!.setSaveFrames(OutputImageFormat.NV21)
            c2_hw_decode_obj!!.set_surface(c2_reader_Surfcae)
            c2_hw_decode_obj!!.decode(videoPathUri)
        } catch (e: Exception) {
            Log.d("dbb", e.toString())
        }
    }

}
