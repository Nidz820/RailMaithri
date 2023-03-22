package org.keltron.railmaithri

import android.Manifest.permission
import android.R.layout
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class Helper {
    companion object{
        fun showToast(context: Context, message: String, duration: Int){
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, duration).show()
            }
        }

        fun saveFile(context: Context, file: ByteArray, fileName: String) {
            try {
                val outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
                outputStream.write(file)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getFile(context: Context, fileName: String): ByteArray? {
            var file: ByteArray? = null
            try {
                val inputStream = context.openFileInput(fileName)
                file = inputStream.readBytes()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return file
        }

        fun saveData(context: Context, key: String, value: String) {
            val sharedPref = context.getSharedPreferences("app_store", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString(key, value)
            editor.apply()
        }

        fun getData(context: Context, key: String): String? {
            val sharedPref = context.getSharedPreferences("app_store", Context.MODE_PRIVATE)
            return sharedPref.getString(key, "")
        }

        fun getObject(context: Context, key: String): String? {
            val sharedPref = context.getSharedPreferences("app_store", Context.MODE_PRIVATE)
            return sharedPref.getString(key, "{}")
        }

        fun getError(response: String): String {
            return try {
                val apiResponse   = JSONObject(response)
                val errorMessages = apiResponse.getJSONArray("non_field_errors")
                errorMessages.getString(0)
            }catch (_: Exception) {
                var errorMessage = response.replace("[^A-Za-z0-9: ]".toRegex(), " ").trim()
                errorMessage = errorMessage.replace("\\s+".toRegex()) { it.value[0].toString() }
                errorMessage.toLowerCase().capitalize()
            }
        }

        fun makeArrayAdapter(jsonArray: JSONArray, context: Context): ArrayAdapter<String> {
            val arrayList = ArrayList<String>()
            for (i in 0 until jsonArray.length()) {
                val arrayElement = jsonArray.getJSONObject(i)
                arrayList.add(arrayElement.getString("name"))
            }
            val arrayAdapter = ArrayAdapter(context, layout.simple_spinner_item, arrayList)
            arrayAdapter.setDropDownViewResource(layout.simple_spinner_dropdown_item)
            return arrayAdapter
        }

        fun getName(jsonArray: JSONArray, id: Int): String {
            var name = ""
            for (i in 0 until jsonArray.length()) {
                val arrayElement = jsonArray.getJSONObject(i)
                if (arrayElement.getInt("id") == id) {
                    name = arrayElement.getString("name")
                    break
                }
            }
            return name
        }

        fun haveLocationPermission(context: Context): Boolean {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isEnabled       = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val permission      = permission.ACCESS_FINE_LOCATION
            val havePermission  = (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
            return havePermission && isEnabled
        }

        @SuppressLint("MissingPermission")
        fun getLocation(context: Context, callback: (location: Location?) -> Unit) {
            val cToken          = CancellationTokenSource().token
            val fusedLocation   = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cToken)

            locationRequest.addOnSuccessListener {
                fusedLocation.lastLocation.addOnSuccessListener { location : Location? ->
                    callback(location)
                }
            }
        }

        fun openMap(context: Context, latitude: Double, longitude: Double){
            val mapUri = Uri.parse("geo:0,0?q=${latitude},${longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                context.startActivity(mapIntent)
            } catch (e: ActivityNotFoundException) {
                showToast(context, "Failed to open map", Toast.LENGTH_SHORT)
            }
        }

        fun getFileName(context: Context, uri: Uri): String {
            val cursor    = context.contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor?.moveToFirst()
            val fileName = nameIndex?.let { cursor.getString(it) }.toString()
            cursor?.close()
            return fileName
        }

        fun getUTC() : String{
            return TimeZone.getTimeZone("UTC").let {
                val calendar  = Calendar.getInstance(it)
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                formatter.format(calendar.time)
            }
        }

        fun prettify(string: String): String {
            return string.replace("_", " ").toLowerCase()
                .split(" ").joinToString(" ") { it.capitalize() }
        }
    }
}