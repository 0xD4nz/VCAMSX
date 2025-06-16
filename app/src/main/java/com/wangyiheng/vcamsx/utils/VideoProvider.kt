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
        // By default, open the first video
        var index = 0
        if (videoStatus!!.selector) {
            index = 1
        }
        val url = videoInfos!!.videos[index].videoUrl
        val fixedUri = Uri.parse(url)

        return try {
            // Directly use ContentResolver to open the file pointed to by the fixed URI and get its ParcelFileDescriptor
            context!!.contentResolver.openFileDescriptor(fixedUri, mode)
        } catch (e: Exception) { // Catch all exceptions, including FileNotFoundException
            Log.e("Error", "Failed to open file: ${e.message}")
            null
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val videoInfos = infoManager.getVideoInfos()
        val videoStatus = infoManager.getVideoStatus()
        // By default, open the first video
        var index = 0
        if (videoStatus!!.selector) {
            index = 1
        }
        val url = videoInfos!!.videos[index].videoUrl
        val fixedUri = Uri.parse(url)

        return try {
            context!!.contentResolver.openAssetFileDescriptor(fixedUri, mode)
        } catch (e: Exception) { // Catch all exceptions, including FileNotFoundException
            Log.e("Error", "Failed to open file: ${e.message}")
            null
        }
    }

    override fun onCreate(): Boolean {
        // Initialize content provider
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
        // Create MatrixCursor
        val cursor = MatrixCursor(arrayOf("_id", "display_name", "size", "date_modified", "file"))
        val path = context?.getExternalFilesDir(null)!!.absolutePath
        val file = File(path, "advancedModeMovies/654e1835b70883406c4640c3/caibi_60.mp4")
        // Get the video folder path
        cursor.addRow(arrayOf(0, file.name, file.length(), file.lastModified(), file))

        return cursor
    }

    // Other methods can be implemented as needed; left empty here for simplicity
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
