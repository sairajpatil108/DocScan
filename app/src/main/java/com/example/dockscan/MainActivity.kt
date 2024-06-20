package com.example.dockscan


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.compose.AppTheme
import com.example.compose.seed
import com.google.android.datatransport.BuildConfig
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()
       val scanner = GmsDocumentScanning.getClient(options)


        setContent {
            AppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                  var imageUris by  remember{
                        mutableStateOf< List<Uri>>(emptyList())
                    }
                   val scannerLauncher = rememberLauncherForActivityResult(
                       contract = ActivityResultContracts.StartIntentSenderForResult(),
                       onResult = {
                           if (it.resultCode == RESULT_OK){
                             val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                               imageUris = result?.pages?.map {it.imageUri}?: emptyList()

                               result?.pdf?.let { pdf ->
                                   val fos = FileOutputStream(File(filesDir, "scan.pdf"))
                                   contentResolver.openInputStream(pdf.uri)?.use{
                                        it.copyTo(fos)
                                   }
                               }
                           }
                       }
                   )
                   
                    Column (
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                     imageUris.forEach{uri ->
                         AsyncImage(model =uri , contentDescription = null,
                             contentScale = ContentScale.FillWidth,
                             modifier = Modifier.fillMaxWidth())

                     }
                        Row (modifier = Modifier.padding(top = 20.dp)){
                            Button(onClick = {
                                scanner.getStartScanIntent(this@MainActivity)
                                    .addOnSuccessListener {
                                        scannerLauncher.launch(
                                            IntentSenderRequest.Builder(it).build()
                                        )
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            applicationContext,
                                            it.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }, modifier = Modifier, colors = ButtonDefaults.buttonColors(seed) ) {
                                Text(text = "Scan PDF")
                            }
                            Box (Modifier.width(10.dp)){

                            }
                            Button(modifier = Modifier, colors = ButtonDefaults.buttonColors(seed),onClick = {
                                // Create an intent to share the PDF file
                                val fileUri = FileProvider.getUriForFile(
                                    this@MainActivity,
                                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                                    File(filesDir, "scan.pdf")
                                )
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(30.dp),

                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

