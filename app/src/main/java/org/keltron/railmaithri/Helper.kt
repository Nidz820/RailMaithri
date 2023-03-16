package org.keltron.railmaithri

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONObject

class Helper {
    companion object{
        fun showToast(context: Context, message: String, duration: Int){
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, duration).show()
            }
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

        fun getError(response: String): String {
            var errorMessage = "Error in API response"
            try {
                val apiResponse   = JSONObject(response)
                val errorMessages = apiResponse.getJSONArray("non_field_errors")
                errorMessage      = errorMessages.getString(0)
            }catch (_: Exception) {
            }
            return errorMessage
        }
    }
}