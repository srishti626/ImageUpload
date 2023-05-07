// Import required classes
package com.example.imageupload

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.imageupload.databinding.ActivityMainBinding
import eu.tutorials.myfirstapp.adapter.RetrofitClient
import eu.tutorials.myfirstapp.adapter.models.UploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private  var filePath:File?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.change.setOnClickListener {
            val options = arrayOf("Open Gallery", "Open Camera")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Choose an option")
            builder.setItems(options) {
                    dialog, which ->
                when (which) {
                    0 -> {
                        openGalleryImageChooser()
                    }
                    1 -> {
                        openCameraImageChooser()
                    }
                }
            }
            builder.show()
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val imageUri: Uri? = data?.data
                if (imageUri != null) {
                    binding.imageView.setImageURI(imageUri)
                    filePath = getFileFromUri( this,imageUri)
                    Log.d("path1",filePath.toString())
                }
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val extras = data?.extras
                val imageBitmap = extras?.getParcelable<Bitmap>("data")
                 binding.imageView.setImageBitmap(imageBitmap)
                if (imageBitmap != null) {
                   selectedImageUri=getBitmapUri(imageBitmap, this)
                    filePath=getFileFromUri(this,selectedImageUri!!)
                    Log.d("path2",filePath?.length().toString())
                }
            }
        }
        binding.upload.setOnClickListener {
            filePath?.let { it1 ->
                uploadImage(id="12300",it1)
            }
        }
    }

    private fun getBitmapUri(bitmap: Bitmap, context: Context): Uri? {
        val fileOutputStream: FileOutputStream
        var uri: Uri? = null
        try {
            // Create a temporary file
            val tempFile = File(context.cacheDir, "temp_image.jpg")
            fileOutputStream = FileOutputStream(tempFile)

            // Compress the bitmap to JPEG format
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            // Get the URI of the saved file
            uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".fileprovider", tempFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return uri
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver: ContentResolver = context.contentResolver
        val fileName: String? = getFileNameFromUri(context, uri)
        if (fileName != null) {
            val file = File(context.cacheDir, fileName)
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                inputStream = contentResolver.openInputStream(uri)
                outputStream = FileOutputStream(file)
                if (inputStream != null) {
                    val buffer = ByteArray(4 * 1024) // 4 KB buffer
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    return file
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        }
        return null
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        val contentResolver: ContentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.let {
            if (it.moveToFirst()) {
                val displayName = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                it.close()
                return displayName
            }
            it.close()
        }
        return null
    }


    private fun openGalleryImageChooser() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun openCameraImageChooser() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 2)
        }
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(takePictureIntent)
    }

    private fun uploadImage(id: String, imageFile: File) {
        val Id : RequestBody = id.toRequestBody("text/plain".toMediaTypeOrNull())
        val imageRequestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageRequestBody)

        val apiService = RetrofitClient.apiInterface
        val call = apiService.getImage(Id, imagePart)
        call.enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                Log.d("fetch1", "${response.code()}")

                Log.d("msg1", "${response.body()?.msg}")
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, response.body()?.msg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
                t.message?.let {
                    Log.d("error", it)
                }
            }
        })
    }
}
