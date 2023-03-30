package org.keltron.railmaithri

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

class AbandonedProperty : AppCompatActivity() {

    private lateinit var progressPB:                  ProgressBar
    private lateinit var saveBT:                      Button
    private lateinit var fileUtil:                    FileUtil
    private lateinit var abandonedPropertyCategorySP: Spinner

    private lateinit var foundByET:                   EditText
    private lateinit var whetherSeizedET:             EditText
    private lateinit var crimeDetailsET:              EditText
    private lateinit var remarksET:                   EditText
    private lateinit var abandonedPropertyCategoryAP: ArrayAdapter<String>

    private lateinit var mode:                        String
    private lateinit var abandonedPropertyCategories: JSONArray
    private lateinit var utcTime:                     String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.abandoned_property)
        supportActionBar!!.hide()

        utcTime                     = Helper.getUTC()
        mode                        = intent.getStringExtra("mode")!!
        fileUtil                    = FileUtil(this, findViewById(R.id.ly_file), utcTime)
        progressPB                  = findViewById(R.id.progress_bar)
        saveBT                      = findViewById(R.id.save)
        abandonedPropertyCategorySP = findViewById(R.id.abandoned_property_category)
        foundByET                   = findViewById(R.id.found_by)
        whetherSeizedET             = findViewById(R.id.whether_seized)
        crimeDetailsET              = findViewById(R.id.crime_details)
        remarksET                   = findViewById(R.id.remarks)

        abandonedPropertyCategories = JSONArray(Helper.getData(this, Scope.ABANDONED_PROPERTY_TYPES)!!)
        abandonedPropertyCategoryAP = Helper.makeArrayAdapter(abandonedPropertyCategories, this)
        abandonedPropertyCategorySP.adapter = abandonedPropertyCategoryAP

        progressPB.visibility = View.GONE
        saveBT.setOnClickListener {
            val inputData = validateInput()
            inputData?.let {
                progressPB.visibility = View.VISIBLE
                saveBT.isClickable = false
                CoroutineScope(Dispatchers.IO).launch {
                    val file     = fileUtil.getFile()
                    val fileName = fileUtil.getFileName()
                    sendForm(inputData, file, fileName)
                    Handler(Looper.getMainLooper()).post {
                        saveBT.isClickable = true
                        progressPB.visibility = View.GONE
                    }
                }
            }
        }

        if (mode == Scope.MODE_VIEW_FORM) {
            saveBT.visibility = View.GONE
            fileUtil.disableUpdate()
        } else if (mode == Scope.MODE_UPDATE_FORM) {
            val savedData = intent.getStringExtra("saved_data")
            val formData  = JSONObject(savedData!!)
            utcTime = formData.getString("utc_timestamp")
            populateForm(formData)
        }
    }

    private fun populateForm(data: JSONObject) {
        foundByET.setText(data.getString("detected_by"))
        whetherSeizedET.setText(data.getString("seized_or_not"))
        remarksET.setText(data.getString("remarks"))
        crimeDetailsET.setText(data.getString("crime_case_details"))

        if (data.has("file_name")) {
            val fileName = data.getString("file_name")
            fileUtil.loadFile(this, fileName)
        }

        val abandonedPropertyCategoryNumber = data.getInt("abandoned_property_category")
        val abandonedPropertyCategoryName   = Helper.getName(abandonedPropertyCategories, abandonedPropertyCategoryNumber)
        val abandonedPropertyCategoryPos    = abandonedPropertyCategoryAP.getPosition(abandonedPropertyCategoryName)
        abandonedPropertyCategorySP.setSelection(abandonedPropertyCategoryPos)
    }

    private fun validateInput(): JSONObject? {
        val foundBy       = foundByET.text.toString()
        val whetherSeized = whetherSeizedET.text.toString()
        val crimeDetails  = crimeDetailsET.text.toString()
        val remarks       = remarksET.text.toString()

        val abandonedPropertyCategoryNumberPos = abandonedPropertyCategorySP.selectedItemPosition
        val abandonedPropertyCategoryNumber    = abandonedPropertyCategories.getJSONObject(abandonedPropertyCategoryNumberPos)
                                                    .getString("id").toString()
                                                    
        val formData = JSONObject()
        formData.put("abandoned_property_category", abandonedPropertyCategoryNumber)
        formData.put("detected_by", foundBy)
        formData.put("crime_case_details", crimeDetails)
        formData.put("utc_timestamp", utcTime)
        formData.put("seized_or_not", whetherSeized)
        formData.put("remarks", remarks)
        return formData
    }

    private fun sendForm(formData: JSONObject, file: ByteArray?, fileName: String?) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  = API.postRequest(token!!, API.ABANDONED_PROPERTY, formData, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeAbandonedProperty()
                }
                Helper.showToast(this, "Abandoned property reported", Toast.LENGTH_SHORT)
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
            removeAbandonedProperty()
        }
        val savedStr  = Helper.getObject(this, Scope.LOST_PROPERTY)!!
        val savedData = JSONObject(savedStr)
        if (fileUtil.haveFile()) {
            fileUtil.saveFile(this)
            formData.put("file_name", fileUtil.getFileName())
        }
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.ABANDONED_PROPERTY, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeAbandonedProperty() {
        val savedStr  = Helper.getObject(this, Scope.ABANDONED_PROPERTY)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.ABANDONED_PROPERTY, savedData.toString())
        fileUtil.removeFile(this)
    }
}
