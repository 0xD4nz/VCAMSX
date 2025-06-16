package com.wangyiheng.vcamsx.modules.home.controllers

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.wangyiheng.vcamsx.data.models.VideoInfo
import com.wangyiheng.vcamsx.data.models.VideoInfos
import com.wangyiheng.vcamsx.data.models.VideoStatues
import com.wangyiheng.vcamsx.data.services.ApiService
import com.wangyiheng.vcamsx.utils.InfoManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.IOException

class HomeController : ViewModel(), KoinComponent {
    val apiService: ApiService by inject()
    val context by inject<Context>()
    val isVideoEnabled = mutableStateOf(false)
    val isVolumeEnabled = mutableStateOf(false)
    val selector = mutableStateOf(false)
    val videoPlayer = mutableStateOf(1)
    val codecType = mutableStateOf(false)
    val isLiveStreamingEnabled = mutableStateOf(false)

    val infoManager by inject<InfoManager>()
    var ijkMediaPlayer: IjkMediaPlayer? = null
    var mediaPlayer: MediaPlayer? = null
    val isLiveStreamingDisplay = mutableStateOf(false)
    val isVideoDisplay = mutableStateOf(false)

    //    rtmp://ns8.indexforce.com/home/mystream
    var liveURL = mutableStateOf("rtmp://ns8.indexforce.com/home/mystream")

    fun init() {
        getState()
    }

    fun copyVideoToAppDir(context: Context, videoUris: List<Uri>) {
        infoManager.removeVideoInfos()

        val videos = mutableListOf<VideoInfo>()

        // Only take the first two
        for (i in 0 until minOf(videoUris.size, 2)) {
            videos.add(VideoInfo(videoId = i, videoUrl = videoUris[i].toString()))
        }

        val videoInfos = VideoInfos(videos = videos)
        infoManager.saveVideoInfos(videoInfos)

        val conf = infoManager.getVideoInfos()
    }

    fun saveState() {
        infoManager.removeVideoStatus()
        infoManager.saveVideoStatus(
            VideoStatues(
                isVideoEnabled.value,
                isVolumeEnabled.value,
                selector.value,
                videoPlayer.value,
                codecType.value,
                isLiveStreamingEnabled.value,
                liveURL.value
            )
        )
    }

    fun getState() {
        infoManager.getVideoStatus()?.let {
            isVideoEnabled.value = it.isVideoEnable
            isVolumeEnabled.value = it.volume
            selector.value = it.selector
            videoPlayer.value = it.videoPlayer
            codecType.value = it.codecType
            isLiveStreamingEnabled.value = it.isLiveStreamingEnabled
            liveURL.value = it.liveURL
        }
    }

    fun playVideo(holder: SurfaceHolder) {
        val videoUrl = "content://com.wangyiheng.vcamsx.videoprovider"
        val videoPathUri = Uri.parse(videoUrl)
        mediaPlayer = MediaPlayer().apply {
            try {
                isLooping = true
                setSurface(holder.surface) // Use SurfaceHolder's surface
                setDataSource(context, videoPathUri) // Set data source
                prepareAsync() // Prepare MediaPlayer asynchronously

                // Set prepared listener
                setOnPreparedListener {
                    start() // Start playing when prepared
                }

                // Optional: set error listener
                setOnErrorListener { mp, what, extra ->
                    // Handle playback error
                    true
                }
            } catch (e: IOException) {
                e.printStackTrace()
                // Handle exceptions when setting data source or other operations
            }
        }
    }

    fun playRTMPStream(holder: SurfaceHolder, rtmpUrl: String) {
        ijkMediaPlayer = IjkMediaPlayer().apply {
            try {
                // Hardware decoding setting: 0 for software, 1 for hardware
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
                setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER,
                    "mediacodec-handle-resolution-change",
                    1
                )

                // Buffering settings
                setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_mpeg4", 1)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "analyzemaxduration", 100L)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "probesize", 1024L)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "flush_packets", 1L)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1L)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1L)

                // Error listener
                setOnErrorListener { _, what, extra ->
                    Log.e("IjkMediaPlayer", "Error occurred. What: $what, Extra: $extra")
                    Toast.makeText(context, "Live stream receive failed: $what", Toast.LENGTH_SHORT).show()
                    true
                }

                // Info listener
                setOnInfoListener { _, what, extra ->
                    Log.i("IjkMediaPlayer", "Info received. What: $what, Extra: $extra")
                    true
                }

                // Set RTMP stream URL
                dataSource = rtmpUrl

                // Set video output SurfaceHolder
                setDisplay(holder)

                // Prepare the player asynchronously
                prepareAsync()

                // When the player is prepared, start playing
                setOnPreparedListener {
                    Toast.makeText(context, "Live stream received successfully. You can now cast.", Toast.LENGTH_SHORT).show()
                    start()
                }
            } catch (e: Exception) {
                Log.d("vcamsx", "Playback error: $e")
            }
        }
    }

    fun release() {
        ijkMediaPlayer?.stop()
        ijkMediaPlayer?.release()
        ijkMediaPlayer = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun isH264HardwareDecoderSupport(): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecInfos = codecList.codecInfos
        for (codecInfo in codecInfos) {
            if (!codecInfo.isEncoder && codecInfo.name.contains("avc") && !isSoftwareCodec(codecInfo.name)) {
                return true
            }
        }
        return false
    }

    fun isSoftwareCodec(codecName: String): Boolean {
        return when {
            codecName.startsWith("OMX.google.") -> true
            codecName.startsWith("OMX.") -> false
            else -> true
        }
    }
}
