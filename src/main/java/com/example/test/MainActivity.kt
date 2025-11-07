package com.example.test

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MenuActivity()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuActivity() {
    val context = LocalContext.current
    var noteTitle by remember { mutableStateOf("") }
    var noteDescription by remember { mutableStateOf("") }
    var notePrice by remember { mutableStateOf("") } // Đã thêm: Biến giá
    var inputImageUrl by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        inputImageUrl = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                val intent = Intent(context, NoteListActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Xem Danh Sách Notes (Xóa/Sửa)")
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = inputImageUrl,
            onValueChange = {
                inputImageUrl = it
                imageUri = null
            },
            label = { Text("URL ảnh bên ngoài (nếu có)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (imageUri != null) "Đã chọn ảnh" else "Hoặc Chọn ảnh/file để upload")
        }

        Spacer(modifier = Modifier.height(16.dp))

        val displayImage: Any? = when {
            inputImageUrl.isNotBlank() -> inputImageUrl
            imageUri != null -> imageUri
            else -> null
        }

        if (displayImage != null) {
            AsyncImage(
                model = displayImage,
                contentDescription = "Selected image",
                modifier = Modifier
                    .size(150.dp)
                    .padding(bottom = 16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Chưa có ảnh/file")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = noteTitle,
            onValueChange = { noteTitle = it },
            label = { Text("Title (Tiêu đề)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = noteDescription,
            onValueChange = { noteDescription = it },
            label = { Text("Description (Mô tả)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = notePrice,
            onValueChange = { notePrice = it },
            label = { Text("Price (Giá tiền)") }, // Đã thêm: Input giá
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (noteTitle.isBlank() || noteDescription.isBlank() || notePrice.isBlank()) { // Kiểm tra giá
                    Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (imageUri == null && inputImageUrl.isBlank()) {
                    Toast.makeText(context, "Vui lòng chọn hoặc nhập URL ảnh/file", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true

                if (inputImageUrl.isNotBlank()) {
                    saveNoteToFirestore(
                        title = noteTitle,
                        description = noteDescription,
                        price = notePrice, // Lưu giá
                        imageUrl = inputImageUrl,
                        context = context
                    )
                    noteTitle = ""
                    noteDescription = ""
                    notePrice = ""
                    inputImageUrl = ""
                    imageUri = null
                    isLoading = false
                } else if (imageUri != null) {
                    uploadFileToFirebaseStorage(
                        uri = imageUri!!,
                        context = context,
                        onSuccess = { downloadUrl ->
                            saveNoteToFirestore(
                                title = noteTitle,
                                description = noteDescription,
                                price = notePrice, // Lưu giá
                                imageUrl = downloadUrl,
                                context = context
                            )
                            noteTitle = ""
                            noteDescription = ""
                            notePrice = ""
                            imageUri = null
                            isLoading = false
                        },
                        onFailure = { e: Exception ->
                            Toast.makeText(context, "Lỗi tải ảnh/file: ${e.message}", Toast.LENGTH_LONG).show()
                            isLoading = false
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Đang tải lên..." else "THÊM NOTE")
        }
    }
}