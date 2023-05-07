package eu.tutorials.myfirstapp.adapter


import eu.tutorials.myfirstapp.adapter.models.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiInterface {
    @Multipart
    @POST("demo_profile_upload.php")
    fun getImage(
        @Part("id") id:RequestBody,
        @Part image: MultipartBody.Part
    ): Call<UploadResponse>
}


