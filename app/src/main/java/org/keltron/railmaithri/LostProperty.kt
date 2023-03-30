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

class LostProperty : AppCompatActivity() {

    private lateinit var progressPB:             ProgressBar
    private lateinit var saveBT:                 Button
    private lateinit var fileUtil:               FileUtil

    private lateinit var lostPropertyCategorySP: Spinner
    private lateinit var policeStationSP:        Spinner
    private lateinit var foundInSP:              Spinner
    private lateinit var descriptionET:          EditText
    private lateinit var foundOnET:              EditText
    private lateinit var remarksET:              EditText

    private lateinit var lostPropertyCategoryAP: ArrayAdapter<String>
    private lateinit var policeStationAP:        ArrayAdapter<String>
    private lateinit var foundInAP:              ArrayAdapter<String>

    private lateinit var mode:                   String
    private lateinit var lostPropertyCategories: JSONArray
    private lateinit var policeStations:         JSONArray
    private lateinit var foundInTypes:           JSONArray
    private lateinit var utcTime:                String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lost_property)
        supportActionBar!!.hide()

        utcTime                = Helper.getUTC()
        mode                   = intent.getStringExtra("mode")!!
        fileUtil               = FileUtil(this, findViewById(R.id.ly_file), utcTime)
        progressPB             = findViewById(R.id.progress_bar)
        saveBT                 = findViewById(R.id.save)
        lostPropertyCategorySP = findViewById(R.id.lost_property_category)
        policeStationSP        = findViewById(R.id.kept_in_police_station)
        foundInSP              = findViewById(R.id.found_in)
        descriptionET          = findViewById(R.id.description)
        foundOnET              = findViewById(R.id.found_on)
        remarksET              = findViewById(R.id.remarks)

        lostPropertyCategories = JSONArray(Helper.getData(this, Scope.LOST_PROPERTY_TYPES)!!)
        lostPropertyCategoryAP = Helper.makeArrayAdapter(lostPropertyCategories, this)
        lostPropertyCategorySP.adapter = lostPropertyCategoryAP

        policeStations  = JSONArray(Helper.getData(this, Scope.POLICE_STATIONS_LIST)!!)
        policeStationAP = Helper.makeArrayAdapter(policeStations, this)
        policeStationSP.adapter = policeStationAP


        foundInTypes = JSONArray(Helper.getData(this, Scope.FOUND_IN_TYPES)!!)
        foundInAP    = Helper.makeArrayAdapter(foundInTypes, this)
        foundInSP.adapter = foundInAP

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
        descriptionET.setText(data.getString("description"))
        foundOnET.setText(data.getString("found_on"))
        remarksET.setText(data.getString("return_remarks"))

        if (data.has("file_name")) {
            val fileName = data.getString("file_name")
            fileUtil.loadFile(this, fileName)
        }
        val lostPropertyCategoryNumber = data.getInt("lost_property_category")
        val lostPropertyCategoryName =
            Helper.getName(lostPropertyCategories, lostPropertyCategoryNumber)

        val lostPropertyCategoryNumberPos =
            lostPropertyCategoryAP.getPosition(lostPropertyCategoryName)
        lostPropertyCategorySP.setSelection(lostPropertyCategoryNumberPos)

        val policeStationNumber = data.getInt("kept_in_police_station")
        val policeStationName   = Helper.getName(policeStations, policeStationNumber)
        val policeStationNumberPos = policeStationAP.getPosition(policeStationName)
        policeStationSP.setSelection(policeStationNumberPos)
    }

    private fun validateInput(): JSONObject? {
        val description = descriptionET.text.toString()
        val foundOn     = foundOnET.text.toString()
        val remarks     = remarksET.text.toString()

        val lostPropertyCategoryNumberPos = lostPropertyCategorySP.selectedItemPosition
        val lostPropertyCategoryNumber =
            lostPropertyCategories.getJSONObject(lostPropertyCategoryNumberPos).getString("id")
                .toString()

        val policeStationNumberPos = policeStationSP.selectedItemPosition
        val policeStationNumber =
            policeStations.getJSONObject(policeStationNumberPos).getString("id").toString()

        val foundInNumberPos = foundInSP.selectedItemPosition
        val foundInNumber    = foundInTypes.getJSONObject(foundInNumberPos).getString("id").toString()

        if (description.isEmpty()) {
            Helper.showToast(this, "Description is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (foundOn.isEmpty()) {
            Helper.showToast(this, "Found on is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("description", description)
        formData.put("lost_property_category", lostPropertyCategoryNumber)
        formData.put("kept_in_police_station", policeStationNumber)
        formData.put("utc_timestamp", utcTime)
        formData.put("found_in", foundInNumber)
        formData.put("found_on", foundOn)
        formData.put("return_remarks", remarks)

        return formData
    }

    private fun sendForm(formData: JSONObject, file: ByteArray?, fileName: String?) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  = API.postRequest(token!!, API.LOST_PROPERTY, formData, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeLostProperty()
                }
                Helper.showToast(this, "Lost property reported", Toast.LENGTH_SHORT)
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
            removeLostProperty()
        }
        val savedStr  = Helper.getObject(this, Scope.LOST_PROPERTY)!!
        val savedData = JSONObject(savedStr)
        if (fileUtil.haveFile()) {
            fileUtil.saveFile(this)
            formData.put("file_name", fileUtil.getFileName())
        }
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.LOST_PROPERTY, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }
    private fun removeLostProperty() {
        val savedStr  = Helper.getObject(this, Scope.LOST_PROPERTY)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.LOST_PROPERTY, savedData.toString())
        fileUtil.removeFile(this)
    }
}