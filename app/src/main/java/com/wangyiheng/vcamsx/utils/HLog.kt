package cn.dianbobo.dbb.util

import android.content.Context
import android.util.Log
import de.robv.android.xposed.XposedBridge
import java.io.*
import java.util.*
import com.bigkoo.pickerview.view.WheelTime.dateFormat

object HLog {
    var lastTransitionTime: Long = 0 // Initialize as 0
    val logBuffer = mutableListOf<String>()
    val MAX_LOG_ENTRIES = 5

    fun d(logtype: String? = "Virtual Camera", msg: String) {
        XposedBridge.log("$logtype:$msg")
    }

    fun localeLog(context: Context, msg: String) {
        val currentTimeMillis = System.currentTimeMillis()
        val formattedDate = dateFormat.format(Date(currentTimeMillis))

        val timeInterval = if (lastTransitionTime != 0L) {
            (currentTimeMillis - lastTransitionTime)  // Milliseconds between logs
        } else {
            0L
        }
        // Update last switch time
        lastTransitionTime = currentTimeMillis
        val logMessage = "Time: $formattedDate\n$msg \nInterval since last log: ${timeInterval} ms"
        Log.d("dbb", logMessage)

        // Add log message to buffer
        logBuffer.add(logMessage)

        // If the buffer reaches max entries, save to file and clear buffer
        if (logBuffer.size >= MAX_LOG_ENTRIES) {
            saveLogsToFile(context)
        }
    }

    private fun saveLogsToFile(context: Context) {
        val logFileDir = context.getExternalFilesDir(null)!!.absolutePath
        val logFilePath = File(logFileDir, "log.txt")

        try {
            // Write buffered log messages to file
            logBuffer.forEach { logMessage ->
                logFilePath.appendText(logMessage + "\n\n")
            }
            // Clear buffer
            logBuffer.clear()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
