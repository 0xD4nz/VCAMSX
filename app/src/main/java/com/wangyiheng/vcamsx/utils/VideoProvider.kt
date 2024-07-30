package com.wangyiheng.vcamsx.utils

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class VideoProvider : ContentProvider(), KoinComponent {
    val infoManager by inject<InfoManager>()

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val videoInfos = infoManager.getVideoInfos()
        val videoStatus = infoManager.getVideoStatus()
        // 默认打开第一个
        var index = 0
        if (videoStatus!!.selector) {
            index = 1
        }
        val url = videoInfos!!.videos[index].videoUrl
        val fixedUri = Uri.parse(url)

        return try {
            // 直接使用ContentResolver打开固定URI指向的文件的ParcelFileDescriptor
            context!!.contentResolver.openFileDescriptor(fixedUri, mode)
        } catch (e: Exception) { // 捕获所有异常，包括FileNotFoundException
            Log.e("Error", "打开文件失败: ${e.message}")
            null
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val videoInfos = infoManager.getVideoInfos()
        val videoStatus = infoManager.getVideoStatus()
        // 默认打开第一个
        var index = 0
        if (videoStatus!!.selector) {
            index = 1
        }
        val url = videoInfos!!.videos[index].videoUrl
        val fixedUri = Uri.parse(url)

        return try {
            context!!.contentResolver.openAssetFileDescriptor(fixedUri, mode)
        } catch (e: Exception) { // 捕获所有异常，包括FileNotFoundException
            Log.e("Error", "打开文件失败: ${e.message}")
            null
        }
    }


    override fun onCreate(): Boolean {
        // 初始化内容提供器
        return true
    }

    fun extractContent(url: String): String {
        val prefix = "com.wangyiheng.vcamsx.videoprovider/"
        val index = url.indexOf(prefix)

        return if (index != -1) {
            url.substring(index + prefix.length)
        } else {
            ""
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        // 创建MatrixCursor
        val cursor = MatrixCursor(arrayOf("_id", "display_name", "size", "date_modified", "file"))
        val path = context?.getExternalFilesDir(null)!!.absolutePath
        val file = File(path, "advancedModeMovies/654e1835b70883406c4640c3/caibi_60.mp4")
        // 获取视频文件夹路径
        cursor.addRow(arrayOf(0, file.name, file.length(), file.lastModified(), file))

        return cursor
    }

    // 其他方法根据需要实现，这里为了简单起见，我们留空
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }
}
