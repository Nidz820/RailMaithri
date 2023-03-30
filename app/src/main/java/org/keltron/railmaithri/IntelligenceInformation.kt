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

class IntelligenceInformation: AppCompatActivity() {

    private lateinit var progressPB:          ProgressBar
    private lateinit var saveBT:              Button
    private lateinit var locationUtil:        LocationUtil
    private lateinit var fileUtil:            FileUtil

    private lateinit var intelligenceTypeSP:  Spinner
    private lateinit var severitySP:          Spinner
    private lateinit var mobileNumberET:      EditText
    private lateinit var informationET:       EditText
    private lateinit var remarksET:           EditText

    private lateinit var intelligenceTypeAP:  ArrayAdapter<String>
    private lateinit var severityAP:          ArrayAdapter<String>

    private lateinit var mode:                String
    private lateinit var intelligenceTypes:   JSONArray
    private lateinit var severities:          JSONArray
    private lateinit var utcTime:             String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.intelligence_information)
        supportActionBar!!.hide()

        utcTime                  = Helper.getUTC()
        mode                     = intent.getStringExtra("mode")!!
        locationUtil             = LocationUtil(this, findViewById(R.id.ly_location))
        fileUtil                 = FileUtil(this, findViewById(R.id.ly_file), utcTime)
        progressPB               = findViewById(R.id.progress_bar)
        saveBT                   = findViewById(R.id.save)
        intelligenceTypeSP       = findViewById(R.id.intelligence_type)
        severitySP               = findViewById(R.id.severity)
        mobileNumberET           = findViewById(R.id.mobile_number)
        informationET            = findViewById(R.id.information)
        remarksET                = findViewById(R.id.remarks)

        intelligenceTypes          = JSONArray(Helper.getData(this, Scope.INTELLIGENCE_TYPES)!!)
        intelligenceTypeAP         = Helper.makeArrayAdapter(intelligenceTypes, this)
        intelligenceTypeSP.adapter = intelligenceTypeAP

        severities          = JSONArray(Helper.getData(this, Scope.INTELLIGENCE_SEVERITY_TYPES)!!)
        severityAP          = Helper.makeArrayAdapter(severities, this)
        severitySP.adapter  = severityAP

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

        if (mode == Scope.MODE_VIEW_FORM){
            saveBT.visibility = View.GONE
            fileUtil.disableUpdate()
            locationUtil.disableUpdate()
        } else if (mode == Scope.MODE_UPDATE_FORM){
            val savedData = intent.getStringExtra("saved_data")
            val formData  = JSONObject(savedData!!)
            utcTime       = formData.getString("utc_timestamp")
            populateForm(formData)
        }
    }

    private fun populateForm(data: JSONObject){
        mobileNumberET.setText(data.getString("mobile_number"))
        informationET.setText(data.getString("information"))
        remarksET.setText(data.getString("remarks"))

        val latitude  = data.getDouble("latitude")
        val longitude = data.getDouble("longitude")
        val accuracy  = data.getDouble("accuracy").toFloat()
        locationUtil.importLocation(latitude, longitude, accuracy)

        if(data.has("file_name")){
            val fileName = data.getString("file_name")
            fileUtil.loadFile(this, fileName)
        }
    }

    private fun validateInput(): JSONObject? {
        val mobileNumber = mobileNumberET.text.toString()
        val information  = informationET.text.toString()
        val remarks      = remarksET.text.toString()

        val intelligenceTypeNumberPos    = intelligenceTypeSP.selectedItemPosition
        val intelligenceTypeNumber       = intelligenceTypes.getJSONObject(intelligenceTypeNumberPos).getString("id").toString()

        val severityNumberPos    = severitySP.selectedItemPosition
        val severityNumber       = severities.getJSONObject(severityNumberPos).getString("id").toString()

        if (!locationUtil.haveLocation()){
            Helper.showToast(this, "Location is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(information.isEmpty()){
            Helper.showToast(this, "Information is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("mobile_number", mobileNumber)
        formData.put("intelligence_type", intelligenceTypeNumber)
        formData.put("severity", severityNumber)
        formData.put("utc_timestamp", utcTime)
        formData.put("mobile_number", mobileNumber)
        formData.put("information", information)
        formData.put("remarks", remarks)
        formData.put("data_from", "Citizen")
        locationUtil.exportLocation(formData)

        return formData
    }

    private fun sendForm(formData: JSONObject, file: ByteArray?, fileName: String?) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  = API.postRequest(token!!, API.INTELLIGENCE_INFORMATION, formData, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeIntelligenceInformation()
                }
                Helper.showToast(this, "Intelligence information saved", Toast.LENGTH_SHORT)
                finish()
            } else {
                val apiResponse  = response.body!!.string()
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
            removeIntelligenceInformation()
        }
        val savedStr  = Helper.getObject(this, Scope.INTELLIGENCE_INFORMATION)!!
        val savedData = JSONObject(savedStr)
        if (fileUtil.haveFile()) {
            fileUtil.saveFile(this)
            formData.put("file_name", fileUtil.getFileName())
        }
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.INTELLIGENCE_INFORMATION, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeIntelligenceInformation() {
        val savedStr  = Helper.getObject(this, Scope.INTELLIGENCE_INFORMATION)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.INTELLIGENCE_INFORMATION, savedData.toString())
        fileUtil.removeFile(this)
    }
}