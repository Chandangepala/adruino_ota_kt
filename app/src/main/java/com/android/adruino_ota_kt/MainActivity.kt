package com.android.adruino_ota_kt

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.adruino_ota_kt.ui.theme.Adruino_ota_ktTheme
import com.android.adruino_ota_kt.view_models.ArduinoViewModel
import com.hoho.android.usbserial.driver.UsbSerialDriver
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.ui.draw.shadow
import com.android.adruino_ota_kt.view_models.ACTION_USB_PERMISSION
import java.io.IOException

class MainActivity : ComponentActivity() {
    var connectString = "Connect"
    var selectedFile = "No_File.hex"
    val viewModel = ArduinoViewModel()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    viewModel.refreshDevices(context)
                    //viewModel.usbConnectionState.value = "Device connected"
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    viewModel.refreshDevices(context)
                    //viewModel.usbConnectionState.value = "Device disconnected"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Adruino_ota_ktTheme {
                Surface {

                    val filter = IntentFilter(ACTION_USB_PERMISSION)
                    registerReceiver(usbReceiver, filter)
                    MainScreen(viewModel)
                }
                /*Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ConnectButton(connectString, this@MainActivity)
                    Spacer(modifier = Modifier.size(40.dp))
                    UploadButton(this@MainActivity)
                    Spacer(modifier = Modifier.size(60.dp))
                    UploadCodeButton(this@MainActivity)
                    Spacer(modifier = Modifier.size(60.dp))
                    Text(selectedFile)
                }*/
            }
        }
    }

    fun pickFile(filePickerLauncher: ActivityResultLauncher<Intent>){
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*" // Or specify a specific MIME type (e.g., "image/*", "application/pdf")
        }
        filePickerLauncher.launch(intent)
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun ConnectButton(connectString: String, context: Context){
    Button(
        elevation = ButtonDefaults.elevatedButtonElevation(),
        onClick = {
            Toast.makeText(context, "Connect Button Clicked", Toast.LENGTH_SHORT).show();
        }) {
        Text(text = connectString, modifier = Modifier.padding(10.0.dp))
    }
}

@Composable
fun UploadButton(context: Context){
    val context = LocalContext.current
    var selectedFileUri by remember{ mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>("")}

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedFileUri = data?.data
            selectedFileName = selectedFileUri?.lastPathSegment

        }
    }
    Spacer(Modifier.height(40.dp))
    Text(text = selectedFileName.toString())
    Spacer(Modifier.height(40.dp))
    IconButton(
        onClick = {
            //pickFile(filePickerLauncher)
            Toast.makeText(context, "selected File: $selectedFileName", Toast.LENGTH_SHORT).show();
         }) {
        Icon(painter = painterResource(id = R.drawable.baseline_upload_file_24), contentDescription = "Upload" ,
            Modifier.size(width = 200.dp, height = 200.dp))
    }
}

@Composable
fun UploadCodeButton(context: Context){
    Button(
        elevation = ButtonDefaults.elevatedButtonElevation(),
        onClick = {
            Toast.makeText(context, "Upload Code Button Clicked", Toast.LENGTH_SHORT).show()
        }) {
        Text(text = "Upload Code", modifier = Modifier.padding(10.0.dp))
    }
}

// 6. Main Screen
@Composable
fun MainScreen(viewModel: ArduinoViewModel) {
    val context = LocalContext.current
    var selectedDevice by remember { mutableStateOf<UsbSerialDriver?>(null) }
    var selectedFile by remember { mutableStateOf<Uri?>(null) }

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedFile = uri }

    LaunchedEffect(Unit) { viewModel.refreshDevices(context) }

    Column(
        Modifier.padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
            ) {
        Text("Connected Arduino Devices", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(30.dp))

        LazyColumn {
            items(viewModel.devices) { device ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .shadow(4.dp)
                        .padding(4.dp)
                        .clickable {
                            selectedDevice = device
                            viewModel.checkAndRequestPermission(context, device.device)
                        }
                ) {
                    Text(
                        text = device.device.deviceName ?: "Unknown Arduino",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        Button(
            elevation = ButtonDefaults.elevatedButtonElevation(),
            onClick =  { filePicker.launch("application/octet-stream") },
            enabled = selectedDevice != null
        ) {
            Text("Select HEX File")
        }

        selectedFile?.let { uri ->
            Text("Selected file: ${uri.lastPathSegment}")
        }

        Spacer(Modifier.height(30.dp))

        Button(
            elevation = ButtonDefaults.elevatedButtonElevation(),
            onClick = {

                selectedDevice?.let { driver ->
                    selectedFile?.let { uri ->
                        viewModel.uploadHexFile(context, driver, uri) { result ->
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                },
                                onFailure = { e ->
                                    errorMessage = when (e) {
                                        is SecurityException -> "USB permission denied!\n" +
                                                "1. Check USB cable connection\n" +
                                                "2. Grant USB permissions in system settings"
                                        is IOException -> "Communication error:\n${e.message}"
                                        else -> "Unexpected error:\n${e.localizedMessage}"
                                    }
                                    showErrorDialog = true
                                }
                            )
                        }
                    }
                }
            },
            enabled = selectedDevice != null && selectedFile != null
        ) {
            Text("Upload to Arduino")
        }

        LinearProgressIndicator(
            progress = viewModel.uploadProgress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
        )

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Upload Error") },
                text = { Text(errorMessage) },
                confirmButton = {
                    Button(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Adruino_ota_ktTheme {
        Column {

        }
    }
}



