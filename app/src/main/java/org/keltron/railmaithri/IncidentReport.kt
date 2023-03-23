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


class IncidentReport : AppCompatActivity() {
    private lateinit var progressPB:   ProgressBar
    private lateinit var saveBT:       Button
    private lateinit var locationUtil: LocationUtil
    private lateinit var fileUtil:     FileUtil

    private lateinit var incidentTypeSP:    Spinner
    private lateinit var railwayStationSP:  Spinner
    private lateinit var platformNumberET:  EditText
    private lateinit var trackLocationET:   EditText
    private lateinit var trainSP:           Spinner
    private lateinit var coachNumberET:     EditText
    private lateinit var contactNumberET:   EditText
    private lateinit var detailsET:         EditText
    private lateinit var railwayStationsAP: ArrayAdapter<String>
    private lateinit var trainsAP:          ArrayAdapter<String>

    private lateinit var mode:              String
    private lateinit var railwayStations:   JSONArray
    private lateinit var trains:            JSONArray
    private lateinit var utcTime:           String

    private val platform = 0
    private val track    = 1
    private val train    = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.incident_report)
        supportActionBar!!.hide()

        utcTime          = Helper.getUTC()
        mode             = intent.getStringExtra("mode")!!
        locationUtil     = LocationUtil(this, findViewById(R.id.ly_location))
        fileUtil         = FileUtil(this, findViewById(R.id.ly_file), utcTime)
        progressPB       = findViewById(R.id.progress_bar)
        saveBT           = findViewById(R.id.sync)
        incidentTypeSP   = findViewById(R.id.incident_type)
        railwayStationSP = findViewById(R.id.railway_station)
        platformNumberET = findViewById(R.id.platform_number)
        trackLocationET  = findViewById(R.id.track_location)
        trainSP          = findViewById(R.id.train)
        coachNumberET    = findViewById(R.id.coach_number)
        contactNumberET  = findViewById(R.id.contact_number)
        detailsET        = findViewById(R.id.details)

        railwayStations          = JSONArray(Helper.getData(this, Scope.RAILWAY_STATIONS_LIST)!!)
        railwayStationsAP        = Helper.makeArrayAdapter(railwayStations, this)
        railwayStationSP.adapter = railwayStationsAP

        trains          = JSONArray(Helper.getData(this, Scope.TRAINS_LIST)!!)
        trainsAP        = Helper.makeArrayAdapter(trains, this)
        trainSP.adapter = trainsAP

        progressPB.visibility = View.GONE
        ArrayAdapter.createFromResource(
            this,
            R.array.incident_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            incidentTypeSP.adapter = adapter
        }
        incidentTypeSP.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateForm(position)
            }
        }

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
        detailsET.setText(data.getString("incident_details"))
        val latitude  = data.getDouble("latitude")
        val longitude = data.getDouble("longitude")
        val accuracy  = data.getDouble("accuracy").toFloat()
        locationUtil.importLocation(latitude, longitude, accuracy)

        if(data.has("file_name")){
            val fileName = data.getString("file_name")
            fileUtil.loadFile(this, fileName)
        }

        when (data.getString("incident_type")) {
            "Platform" -> {
                incidentTypeSP.setSelection(platform)
                platformNumberET.setText(data.getString("platform_number"))

                val railwayStationID   = data.getInt("railway_station")
                val railwayStationName = Helper.getName(railwayStations, railwayStationID)
                val railwayStationPos  = railwayStationsAP.getPosition(railwayStationName)
                railwayStationSP.setSelection(railwayStationPos)
            }
            "Track" -> {
                incidentTypeSP.setSelection(track)
                trackLocationET.setText(data.getString("track_location"))

                val railwayStationID   = data.getInt("railway_station")
                val railwayStationName = Helper.getName(railwayStations, railwayStationID)
                val railwayStationPos  = railwayStationsAP.getPosition(railwayStationName)
                railwayStationSP.setSelection(railwayStationPos)
            }
            "Train" -> {
                incidentTypeSP.setSelection(train)
                contactNumberET.setText(data.getString("mobile_number"))
                coachNumberET.setText(data.getString("coach"))

                val trainNumber     = data.getInt("train_number")
                val trainName       = Helper.getName(trains, trainNumber)
                val trainNumberPos  = trainsAP.getPosition(trainName)
                trainSP.setSelection(trainNumberPos)
            }
        }
    }

    private fun updateForm(position: Int){
        val railwayStationLY = findViewById<LinearLayout>(R.id.ly_railway_station)
        val platformNumberLY = findViewById<LinearLayout>(R.id.ly_platform_number)
        val trackLocationLY  = findViewById<LinearLayout>(R.id.ly_track_location)
        val trainLY          = findViewById<LinearLayout>(R.id.ly_train)
        val coachNumberLY    = findViewById<LinearLayout>(R.id.ly_coach_number)
        val contactNumberLY  = findViewById<LinearLayout>(R.id.ly_contact_number)

        railwayStationLY.visibility = View.GONE
        platformNumberLY.visibility = View.GONE
        trackLocationLY.visibility = View.GONE
        trainLY.visibility = View.GONE
        coachNumberLY.visibility = View.GONE
        contactNumberLY.visibility = View.GONE

        when (position) {
            platform -> {
                railwayStationLY.visibility = View.VISIBLE
                platformNumberLY.visibility = View.VISIBLE
            }
            track -> {
                railwayStationLY.visibility = View.VISIBLE
                trackLocationLY.visibility = View.VISIBLE
            }
            train -> {
                trainLY.visibility = View.VISIBLE
                coachNumberLY.visibility = View.VISIBLE
                contactNumberLY.visibility = View.VISIBLE
            }
        }
    }

    private fun validateInput(): JSONObject? {
        val coachNumber    = coachNumberET.text.toString()
        val contactNumber  = contactNumberET.text.toString()
        val platformNumber = platformNumberET.text.toString()
        val trackLocation  = trackLocationET.text.toString()
        val details        = detailsET.text.toString()

        val trainNumberPos    = trainSP.selectedItemPosition
        val trainNumber       = trains.getJSONObject(trainNumberPos).getString("id").toString()
        val railwayStationPos = railwayStationSP.selectedItemPosition
        val stationNumber     = railwayStations.getJSONObject(railwayStationPos).getString("id").toString()

        if (!locationUtil.haveLocation()){
            Helper.showToast(this, "Location is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if(details.isEmpty()){
            Helper.showToast(this, "Incident detail is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("status", "1")
        formData.put("data_from", "Beat Officer")
        formData.put("incident_date_time", utcTime)
        formData.put("utc_timestamp", utcTime)
        formData.put("incident_details", details)
        locationUtil.exportLocation(formData)

        when (incidentTypeSP.selectedItemPosition) {
            platform -> {
                if(platformNumber.isEmpty()){
                    Helper.showToast(this, "Platform number is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                formData.put("incident_type","Platform")
                formData.put("railway_station", stationNumber)
                formData.put("platform_number", platformNumber)
            }
            track -> {
                if(trackLocation.isEmpty()){
                    Helper.showToast(this, "Track location is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                formData.put("incident_type","Track")
                formData.put("railway_station", stationNumber)
                formData.put("track_location", trackLocation)
            }
            train -> {
                if(coachNumber.isEmpty()){
                    Helper.showToast(this, "Coach number is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                if(contactNumber.isEmpty()){
                    Helper.showToast(this, "Contact number is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                formData.put("incident_type", "Train")
                formData.put("train_number", trainNumber)
                formData.put("coach", coachNumber)
                formData.put("mobile_number", contactNumber)
            }
        }
        return formData
    }

    private fun sendForm(formData: JSONObject, file: ByteArray?, fileName: String?) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  = API.postRequest(token!!, API.INCIDENT_REPORT, formData, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM){
                    removeIncident()
                }
                Helper.showToast(this, "Incident reported", Toast.LENGTH_SHORT)
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
           removeIncident()
        }
        val savedStr  = Helper.getObject(this, Scope.INCIDENT_REPORT)!!
        val savedData = JSONObject(savedStr)
        if (fileUtil.haveFile()) {
            fileUtil.saveFile(this)
            formData.put("file_name", fileUtil.getFileName())
        }
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.INCIDENT_REPORT, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeIncident() {
        val savedStr  = Helper.getObject(this, Scope.INCIDENT_REPORT)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.INCIDENT_REPORT, savedData.toString())
        fileUtil.removeFile(this)
    }
}