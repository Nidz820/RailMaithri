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

class UnauthorizedPeople: AppCompatActivity()  {
    private lateinit var progressPB:              ProgressBar
    private lateinit var saveBT:                  Button
    private lateinit var locationUtil:            LocationUtil
    private lateinit var fileUtil:                FileUtil

    private lateinit var unauthorizedCategorySP:  Spinner
    private lateinit var policeStationSP:         Spinner
    private lateinit var descriptionET:           EditText
    private lateinit var placeOfCheckET:          EditText

    private lateinit var unauthorizedCategoryAP:  ArrayAdapter<String>
    private lateinit var policeStationAP:         ArrayAdapter<String>

    private lateinit var mode:                    String
    private lateinit var unauthorizedCategories:  JSONArray
    private lateinit var policeStations:          JSONArray
    private lateinit var utcTime:                 String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.unauthorized_people)
        supportActionBar!!.hide()

        utcTime                  = Helper.getUTC()
        mode                     = intent.getStringExtra("mode")!!
        locationUtil             = LocationUtil(this, findViewById(R.id.ly_location))
        fileUtil                 = FileUtil(this, findViewById(R.id.ly_file), utcTime)
        progressPB               = findViewById(R.id.progress_bar)
        saveBT                   = findViewById(R.id.save)
        unauthorizedCategorySP   = findViewById(R.id.category)
        policeStationSP          = findViewById(R.id.police_station)
        descriptionET            = findViewById(R.id.description)
        placeOfCheckET           = findViewById(R.id.place_of_check)

        unauthorizedCategories         = JSONArray(Helper.getData(this, Scope.VENDOR_TYPES)!!)
        unauthorizedCategoryAP         = Helper.makeArrayAdapter(unauthorizedCategories, this)
        unauthorizedCategorySP.adapter = unauthorizedCategoryAP

        policeStations          = JSONArray(Helper.getData(this, Scope.POLICE_STATIONS_LIST)!!)
        policeStationAP         = Helper.makeArrayAdapter(policeStations, this)
        policeStationSP.adapter = policeStationAP

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
        descriptionET.setText(data.getString("description"))
        placeOfCheckET.setText(data.getString("place_of_check"))

        val latitude  = data.getDouble("latitude")
        val longitude = data.getDouble("longitude")
        val accuracy  = data.getDouble("accuracy").toFloat()
        locationUtil.importLocation(latitude, longitude, accuracy)

        if(data.has("file_name")){
            val fileName = data.getString("file_name")
            fileUtil.loadFile(this, fileName)
        }
        val unauthorizedCategoryNumber     = data.getInt("category")
        val unauthorizedCategoryName       = Helper.getName(unauthorizedCategories, unauthorizedCategoryNumber)
        val unauthorizedCategoryNumberPos  = unauthorizedCategoryAP.getPosition(unauthorizedCategoryName)
        unauthorizedCategorySP.setSelection(unauthorizedCategoryNumberPos)

        val policeStationNumber    = data.getInt("police_station")
        val policeStationName      = Helper.getName(policeStations, policeStationNumber)
        val policeStationNumberPos = policeStationAP.getPosition(policeStationName)
        policeStationSP.setSelection(policeStationNumberPos)
    }

    private fun validateInput(): JSONObject? {
        val description    = descriptionET.text.toString()
        val placeOfCheck   = placeOfCheckET.text.toString()

        val unauthorizedCategoryNumberPos    = unauthorizedCategorySP.selectedItemPosition
        val unauthorizedCategoryNumber       = unauthorizedCategories.getJSONObject(unauthorizedCategoryNumberPos).getString("id").toString()

        val policeStationNumberPos = policeStationSP.selectedItemPosition
        val policeStationNumber =
            policeStations.getJSONObject(policeStationNumberPos).getString("id").toString()

        if (!locationUtil.haveLocation()){
            Helper.showToast(this, "Location is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(description.isEmpty()){
            Helper.showToast(this, "Description is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(placeOfCheck.isEmpty()){
            Helper.showToast(this, "Place of check is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("category", unauthorizedCategoryNumber)
        formData.put("description", description)
        formData.put("police_station", policeStationNumber)
        formData.put("utc_timestamp", utcTime)
        formData.put("place_of_check", placeOfCheck)
        formData.put("added_by", 1)
        locationUtil.exportLocation(formData)

        return formData
    }

    private fun sendForm(formData: JSONObject, file: ByteArray?, fileName: String?) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  = API.postRequest(token!!, API.UNAUTHORIZED_PEOPLE, formData, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeUnauthorizedPeople()
                }
                Helper.showToast(this, "Stranger check reported", Toast.LENGTH_SHORT)
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
            removeUnauthorizedPeople()
        }
        val savedStr  = Helper.getObject(this, Scope.UNAUTHORIZED_PEOPLE)!!
        val savedData = JSONObject(savedStr)
        if (fileUtil.haveFile()) {
            fileUtil.saveFile(this)
            formData.put("file_name", fileUtil.getFileName())
        }
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.UNAUTHORIZED_PEOPLE, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeUnauthorizedPeople() {
        val savedStr  = Helper.getObject(this, Scope.UNAUTHORIZED_PEOPLE)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.UNAUTHORIZED_PEOPLE, savedData.toString())
        fileUtil.removeFile(this)
    }
}