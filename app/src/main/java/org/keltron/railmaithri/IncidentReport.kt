package org.keltron.railmaithri

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class FileUtil(_activity: AppCompatActivity, _locationLY: ConstraintLayout) {
    private var file:     ByteArray? = null
    private var fileName: String?    = null
    private var uuid:     String?    = null

    private var selectFileBT: Button
    private var deleteFileBT: Button
    private var fileNameTV:   TextView

    init {
        selectFileBT        = _locationLY.findViewById(R.id.select_file)
        deleteFileBT        = _locationLY.findViewById(R.id.delete_file)
        fileNameTV          = _locationLY.findViewById(R.id.file_name)

        val selectionResult = _activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val inputStream = _activity.contentResolver.openInputStream(uri)
                    file = inputStream.use { it?.readBytes() }!!
                    inputStream?.close()

                    val cursor    = _activity.contentResolver.query(uri, null, null, null, null)
                    val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor?.moveToFirst()
                    fileName = nameIndex?.let { cursor.getString(it) }.toString()
                    cursor?.close()

                    deleteFileBT.isClickable = true
                    fileNameTV.text          = fileName
                }
            }
        }
        selectFileBT.setOnClickListener {
            val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
            selectionResult.launch(Intent.createChooser(intent, "Select a file"))
        }
        deleteFileBT.setOnClickListener { clearSelection() }
    }

    private fun clearSelection() {
        deleteFileBT.isClickable = false
        fileNameTV.text          = "No file selected"
    }

    fun haveFile(): Boolean {
        return !(file == null || fileName == null)
    }

    fun getFile(): ByteArray? {
         return file
    }

    fun getFileName(): String? {
        return fileName
    }

    fun enableUpdation () {
        deleteFileBT.isClickable = true
        selectFileBT.isClickable = true
    }

    fun disableUpdation() {
        deleteFileBT.isClickable = false
        selectFileBT.isClickable = false
    }

    fun loadFile(context: Context, _uuid: String, _fileName: String){
        try{
            fileName        = _fileName
            fileNameTV.text = fileName
            uuid            = _uuid

            val inputStream = context.openFileInput(_uuid)
            file = inputStream.readBytes()
            inputStream.close()
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeFile() {
        if(uuid != null){
            val storedFile = File(uuid!!)
            storedFile.delete()
        }
    }

    fun saveFile(context: Context) {
        try {
            val outputStream = context.openFileOutput(uuid, Context.MODE_PRIVATE)
            outputStream.write(file)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


class IncidentReport : AppCompatActivity() {
    private lateinit var progressPB:   ProgressBar
    private lateinit var saveBT:       Button
    private lateinit var locationUtil: LocationUtil
    private lateinit var fileUtil:     FileUtil

    private lateinit var incidentTypeSP:        Spinner
    private lateinit var railwayStationSP:      Spinner
    private lateinit var platformNumberET:      EditText
    private lateinit var trackLocationET:       EditText
    private lateinit var trainSP:               Spinner
    private lateinit var coachNumberET:         EditText
    private lateinit var contactNumberET:       EditText
    private lateinit var detailsET:             EditText
    private lateinit var railwayStationsAP:     ArrayAdapter<String>
    private lateinit var trainsAP:              ArrayAdapter<String>

    private lateinit var mode:                  String
    private lateinit var railwayStations:       JSONArray
    private lateinit var trains:                JSONArray
    private lateinit var utcTime:               String

    private val PLATFORM     = 0
    private val TRACK        = 1
    private val TRAIN        = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.incident_report)
        supportActionBar!!.hide()

        progressPB         = findViewById(R.id.progress_bar)
        saveBT             = findViewById(R.id.sync)
        locationUtil       = LocationUtil(this, findViewById(R.id.ly_location))
        fileUtil           = FileUtil(this, findViewById(R.id.ly_file))

        incidentTypeSP     = findViewById(R.id.incident_type)
        railwayStationSP   = findViewById(R.id.railway_station)
        platformNumberET   = findViewById(R.id.platform_number)
        trackLocationET    = findViewById(R.id.track_location)
        trainSP            = findViewById(R.id.train)
        coachNumberET      = findViewById(R.id.coach_number)
        contactNumberET    = findViewById(R.id.contact_number)
        detailsET          = findViewById(R.id.details)

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

        utcTime = Helper.getUTC()
        mode    = intent.getStringExtra("mode")!!
        if (mode == Scope.MODE_VIEW_FORM){
            saveBT.visibility = View.GONE
            locationUtil.disableUpdate()
            fileUtil.disableUpdation()
        } else if (mode == Scope.MODE_UPDATE_FORM){
            val savedData = intent.getStringExtra("saved_data")
            val formData  = JSONObject(savedData!!)
            utcTime       = formData.getString("utc_timestamp")
            populateForm(utcTime, formData)
        }
    }

    private fun populateForm(uuid: String, data: JSONObject){
        detailsET.setText(data.getString("incident_details"))
        val latitude  = data.getDouble("latitude")
        val longitude = data.getDouble("longitude")
        val accuracy  = data.getDouble("accuracy").toFloat()
        locationUtil.importLocation(latitude, longitude, accuracy)

        if(data.has("file_name")){
            val fileName = data.getString("file_name")
            fileUtil.loadFile(this, utcTime, fileName)
        }

        when (data.getString("incident_type")) {
            "Platform" -> {
                incidentTypeSP.setSelection(PLATFORM)
                platformNumberET.setText(data.getString("platform_number"))

                val railwayStationID   = data.getInt("railway_station")
                val railwayStationName = Helper.getName(railwayStations, railwayStationID)
                val railwayStationPos  = railwayStationsAP.getPosition(railwayStationName)
                railwayStationSP.setSelection(railwayStationPos)
            }
            "Track" -> {
                incidentTypeSP.setSelection(TRACK)
                trackLocationET.setText(data.getString("track_location"))

                val railwayStationID   = data.getInt("railway_station")
                val railwayStationName = Helper.getName(railwayStations, railwayStationID)
                val railwayStationPos  = railwayStationsAP.getPosition(railwayStationName)
                railwayStationSP.setSelection(railwayStationPos)
            }
            "Train" -> {
                incidentTypeSP.setSelection(TRAIN)
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
            PLATFORM -> {
                railwayStationLY.visibility = View.VISIBLE
                platformNumberLY.visibility = View.VISIBLE
            }
            TRACK -> {
                railwayStationLY.visibility = View.VISIBLE
                trackLocationLY.visibility = View.VISIBLE
            }
            TRAIN -> {
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
            PLATFORM -> {
                if(platformNumber.isEmpty()){
                    Helper.showToast(this, "Platform number is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                formData.put("incident_type","Platform")
                formData.put("railway_station", stationNumber)
                formData.put("platform_number", platformNumber)
            }
            TRACK -> {
                if(trackLocation.isEmpty()){
                    Helper.showToast(this, "Track location is mandatory", Toast.LENGTH_LONG)
                    return null
                }
                formData.put("incident_type","Track")
                formData.put("railway_station", stationNumber)
                formData.put("track_location", trackLocation)
            }
            TRAIN -> {
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
                    removeIncident(utcTime)
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
        if (mode == Scope.MODE_UPDATE_FORM){
           removeIncident(utcTime)
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

    private fun removeIncident(uuid: String){
        try{
            val savedStr  = Helper.getObject(this, Scope.INCIDENT_REPORT)!!
            val savedData = JSONObject(savedStr)
            savedData.remove(utcTime)
            Helper.saveData(this, Scope.INCIDENT_REPORT, savedData.toString())
        }catch (_: Exception){ }
        fileUtil.removeFile()
    }
}