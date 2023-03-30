package org.keltron.railmaithri

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject

class BeatDiary : AppCompatActivity() {

    private lateinit var progressPB:   ProgressBar
    private lateinit var saveBT:       Button
    private lateinit var noteET:       EditText

    private lateinit var mode:         String
    private lateinit var utcTime:      String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.beat_diary)
        supportActionBar!!.hide()

        utcTime = Helper.getUTC()
        mode = intent.getStringExtra("mode")!!
        progressPB = findViewById(R.id.progress_bar)
        saveBT = findViewById(R.id.save)
        noteET = findViewById(R.id.note)

        progressPB.visibility = View.GONE
        saveBT.setOnClickListener {
            val inputData = validateInput()
            inputData?.let {
                progressPB.visibility = View.VISIBLE
                saveBT.isClickable = false
                CoroutineScope(Dispatchers.IO).launch {
                    sendForm(inputData)
                    Handler(Looper.getMainLooper()).post {
                        saveBT.isClickable = true
                        progressPB.visibility = View.GONE
                    }
                }
            }
        }

        if (mode == Scope.MODE_VIEW_FORM) {
            saveBT.visibility = View.GONE
        } else if (mode == Scope.MODE_UPDATE_FORM) {
            val savedData  = intent.getStringExtra("saved_data")
            val formData   = JSONObject(savedData!!)
            utcTime        = formData.getString("utc_timestamp")
            populateForm(formData)
        }
    }

    private fun populateForm(data: JSONObject) {
        noteET.setText(data.getString("description"))
    }

    private fun validateInput(): JSONObject? {
        val note = noteET.text.toString()

        if (note.isEmpty()) {
            Helper.showToast(this, "Note is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("description", note)
        formData.put("utc_timestamp", utcTime)
        return formData
    }

    private fun sendForm(formData: JSONObject) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token   = Helper.getData(this, Scope.TOKEN)
            val request = API.postRequest(token!!, API.BEAT_DIARY, formData, file = null, fileName = null)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeBeatDiary()
                }
                Helper.showToast(this, "Beat diary saved", Toast.LENGTH_SHORT)
                finish()
            } else {
                val apiResponse = response.body!!.string()
                val errorMessage = Helper.getError(apiResponse)
                Helper.showToast(this, errorMessage, Toast.LENGTH_LONG)
            }
        } catch (e: Exception) {
            Log.d("RailMaithri", e.stackTraceToString())
            saveForm(formData)
        }
    }

    private fun saveForm(formData: JSONObject) {
        if (mode == Scope.MODE_UPDATE_FORM) {
            removeBeatDiary()
        }
        val savedStr  = Helper.getObject(this, Scope.BEAT_DIARY)!!
        val savedData = JSONObject(savedStr)
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.BEAT_DIARY, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }
    
    private fun removeBeatDiary() {
        val savedStr  = Helper.getObject(this, Scope.BEAT_DIARY)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.BEAT_DIARY, savedData.toString())
    }
}
