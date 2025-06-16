import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wangyiheng.vcamsx.components.LivePlayerDialog
import com.wangyiheng.vcamsx.components.SettingRow
import com.wangyiheng.vcamsx.components.VideoPlayerDialog
import com.wangyiheng.vcamsx.modules.home.controllers.HomeController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val homeController = viewModel<HomeController>()
    LaunchedEffect(Unit) {
        homeController.init()
    }

    val selectVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        uris?.let {
            homeController.copyVideoToAppDir(context, it)
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                selectVideoLauncher.launch("video/*")
            } else {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    // On Android 9 (Pie) and below, request READ_EXTERNAL_STORAGE permission
                    Toast.makeText(context, "Please enable permission to read folders in settings.", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    // On Android 10 and above, directly access video files without requesting permission
                    selectVideoLauncher.launch("video/*")
                }
            }
        }
    )

    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        val buttonModifier = Modifier
            .fillMaxWidth()

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = homeController.liveURL.value,
                onValueChange = { homeController.liveURL.value = it },
                label = { Text("RTMP URL:") }
            )

            Button(
                modifier = buttonModifier,
                onClick = {
                    homeController.saveState()
                }
            ) {
                Text("Save RTMP URL")
            }

            Button(
                modifier = buttonModifier,
                onClick = {
                    Toast.makeText(context, "Please select two videos in sequence, do not select more.", Toast.LENGTH_SHORT).show()
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            ) {
                Text("Select Video")
            }

            Button(
                modifier = buttonModifier,
                onClick = {
                    homeController.isVideoDisplay.value = true
                }
            ) {
                Text("View Video")
            }

            Button(
                modifier = buttonModifier,
                onClick = {
                    homeController.isLiveStreamingDisplay.value = true
                }
            ) {
                Text("View Live Stream")
            }

            SettingRow(
                label = "Video Switch",
                checkedState = homeController.isVideoEnabled,
                onCheckedChange = { homeController.saveState() },
                context = context
            )

            SettingRow(
                label = "Main/Secondary Video Switch",
                checkedState = homeController.selector,
                onCheckedChange = {
                    // And switch to new player
                    homeController.saveState()
                },
                context = context
            )

            SettingRow(
                label = "Live Stream Switch",
                checkedState = homeController.isLiveStreamingEnabled,
                onCheckedChange = { homeController.saveState() },
                context = context
            )

            SettingRow(
                label = "Volume Switch",
                checkedState = homeController.isVolumeEnabled,
                onCheckedChange = { homeController.saveState() },
                context = context
            )

            SettingRow(
                label = if (homeController.codecType.value) "Hardware Decoding" else "Software Decoding",
                checkedState = homeController.codecType,
                onCheckedChange = {
                    if (homeController.isH264HardwareDecoderSupport()) {
                        homeController.saveState()
                    } else {
                        homeController.codecType.value = false
                        Toast.makeText(context, "Hardware decoding not supported", Toast.LENGTH_SHORT).show()
                    }
                },
                context = context
            )
        }

        LivePlayerDialog(homeController)
        VideoPlayerDialog(homeController)
    }
}

@Preview
@Composable
fun PreviewMessageCard() {
    HomeScreen()
}
