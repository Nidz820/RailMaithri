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

class POI: AppCompatActivity() {

    private lateinit var progressPB:       ProgressBar
    private lateinit var saveBT:           Button
    private lateinit var locationUtil:     LocationUtil
    private lateinit var fileUtil:         FileUtil

    private lateinit var poiCategorySP:    Spinner
    private lateinit var policeStationSP:  Spinner
    private lateinit var districtSP:       Spinner
    private lateinit var nameET:           EditText

    private lateinit var poiCategoryAP:    ArrayAdapter<String>
    private lateinit var policeStationAP:  ArrayAdapter<String>
    private lateinit var districtAP:       ArrayAdapter<String>

    private lateinit var mode:             String
    private lateinit var poiCategories:    JSONArray
    private lateinit var districts:        JSONArray
    private lateinit var policeStations:   JSONArray
    private lateinit var utcTime:          String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.poi)
        supportActionBar!!.hide()

        utcTime         = Helper.getUTC()
        mode            = intent.getStringExtra("mode")!!
        locationUtil    = LocationUtil(this, findViewById(R.id.ly_location))
        fileUtil        = FileUtil(this, findViewById(R.id.ly_file), utcTime)
        progressPB      = findViewById(R.id.progress_bar)
        saveBT          = findViewById(R.id.save)
        poiCategorySP   = findViewById(R.id.poi_category)
        policeStationSP = findViewById(R.id.police_station)
        districtSP      = findViewById(R.id.district)
        nameET          = findViewById(R.id.name)

        policeStations  = JSONArray(Helper.getData(this, Scope.POLICE_STATIONS_LIST)!!)
        policeStationAP = Helper.makeArrayAdapter(policeStations, this)
        policeStationSP.adapter = policeStationAP

        districts  = JSONArray(Helper.getData(this, Scope.DISTRICTS_LIST)!!)
        districtAP = Helper.makeArrayAdapter(districts, this)
        districtSP.adapter = districtAP

        poiCategories = JSONArray(Helper.getData(this, Scope.POI_TYPES)!!)
        poiCategoryAP = Helper.makeArrayAdapter(poiCategories, this)
        poiCategorySP.adapter = poiCategoryAP

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

    private fun populateForm(data: JSONObject) {
        nameET.setText(data.getString("name"))

        val latitude  = data.getDouble("latitude")
        val longitude = data.getDouble("longitude")
        val accuracy  = data.getDouble("accuracy").toFloat()
        locationUtil.importLocation(latitude, longitude, accuracy)

        if (data.has("file_name")) {
            val fileName = data.getString("file_name")
            fileUtil.loadFile(this, fileName)
        }

        val policeStationNumber    = data.getInt("police_station")
        val policeStationName      = Helper.getName(policeStations, policeStationNumber)
        val policeStationNumberPos = policeStationAP.getPosition(policeStationName)
        policeStationSP.setSelection(policeStationNumberPos)

        val districtNumber    = data.getInt("district")
        val districtName      = Helper.getName(districts, districtNumber)
        val districtNumberPos = districtAP.getPosition(districtName)
        districtSP.setSelection(districtNumberPos)

        val poiCategoryNumber    = data.getInt("poi_category")
        val poiCategoryName      = Helper.getName(poiCategories, poiCategoryNumber)
        val poiCategoryNumberPos = poiCategoryAP.getPosition(poiCategoryName)
        poiCategorySP.setSelection(poiCategoryNumberPos)
    }

    private fun validateInput(): JSONObject? {
        val name    = nameET.text.toString()

        val policeStationNumberPos = policeStationSP.selectedItemPosition
        val policeStationNumber    = policeStations.getJSONObject(policeStationNumberPos)
                                        .getString("id").toString()

        val districtNumberPos = districtSP.selectedItemPosition
        val districtNumber    = districts.getJSONObject(districtNumberPos)
                                    .getString("id").toString()

        val poiCategoryNumberPos = poiCategorySP.selectedItemPosition
        val poiCategoryNumber    = poiCategories.getJSONObject(poiCategoryNumberPos)
                                    .getString("id").toString()

        if (name.isEmpty()) {
            Helper.showToast(this, "Name is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("poi_category", poiCategoryNumber)
        formData.put("utc_timestamp", utcTime)
        formData.put("police_station", policeStationNumber)
        formData.put("name", name)
        formData.put("district", districtNumber)
        locationUtil.exportLocation(formData)
        return formData
    }

    private fun sendForm(formData: JSONObject,file: ByteArray?, fileName: String?) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  =
                API.postRequest(token!!, API.POI, formData, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removePoi()
                }
                Helper.showToast(this, "Poi saved", Toast.LENGTH_SHORT)
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
            removePoi()
        }
        val savedStr  = Helper.getObject(this, Scope.POI)!!
        val savedData = JSONObject(savedStr)
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.POI, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removePoi() {
        val savedStr  = Helper.getObject(this, Scope.POI)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.POI, savedData.toString())
    }
}
