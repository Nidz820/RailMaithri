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

class CrimeMemo : AppCompatActivity(){

    private lateinit var progressPB:            ProgressBar
    private lateinit var saveBT:                Button
    private lateinit var fileUtil:              FileUtil

    private lateinit var crimeMemoCategorySP:   Spinner
    private lateinit var policeStationSP:       Spinner
    private lateinit var memoDetailsET:         EditText

    private lateinit var crimeMemoCategoryAP:   ArrayAdapter<String>
    private lateinit var policeStationAP:       ArrayAdapter<String>

    private lateinit var mode:                  String
    private lateinit var crimeMemoCategories:   JSONArray
    private lateinit var policeStations:        JSONArray
    private lateinit var utcTime:               String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crime_memo)
        supportActionBar!!.hide()

        utcTime             = Helper.getUTC()
        mode                = intent.getStringExtra("mode")!!
        fileUtil            = FileUtil(this, findViewById(R.id.ly_file), utcTime)
        progressPB          = findViewById(R.id.progress_bar)
        saveBT              = findViewById(R.id.save)
        crimeMemoCategorySP = findViewById(R.id.crime_memo_category)
        policeStationSP     = findViewById(R.id.police_station)
        memoDetailsET       = findViewById(R.id.memo_details)

        crimeMemoCategories = JSONArray(Helper.getData(this, Scope.CRIME_MEMO_TYPES)!!)
        crimeMemoCategoryAP = Helper.makeArrayAdapter(crimeMemoCategories, this)
        crimeMemoCategorySP.adapter = crimeMemoCategoryAP

        policeStations  = JSONArray(Helper.getData(this, Scope.POLICE_STATIONS_LIST)!!)
        policeStationAP = Helper.makeArrayAdapter(policeStations, this)
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
        memoDetailsET.setText(data.getString("memo_details"))

        if (data.has("file_name")) {
            val fileName = data.getString("file_name")
            fileUtil.loadFile(this, fileName)
        }
        val crimeMemoCategoryNumber = data.getInt("crime_memo_category")
        val crimeMemoCategoryName =
            Helper.getName(crimeMemoCategories, crimeMemoCategoryNumber)

        val crimeMemoCategoryNumberPos =
            crimeMemoCategoryAP.getPosition(crimeMemoCategoryName)
        crimeMemoCategorySP.setSelection(crimeMemoCategoryNumberPos)

        val policeStationNumber    = data.getInt("police_station")
        val policeStationName      = Helper.getName(policeStations, policeStationNumber)
        val policeStationNumberPos = policeStationAP.getPosition(policeStationName)
        policeStationSP.setSelection(policeStationNumberPos)
    }
    private fun validateInput(): JSONObject? {
        val memoDetails = memoDetailsET.text.toString()

        val crimeMemoCategoryNumberPos = crimeMemoCategorySP.selectedItemPosition
        val crimeMemoCategoryNumber =
            crimeMemoCategories.getJSONObject(crimeMemoCategoryNumberPos).getString("id")
                .toString()

        val policeStationNumberPos = policeStationSP.selectedItemPosition
        val policeStationNumber =
            policeStations.getJSONObject(policeStationNumberPos).getString("id").toString()

        if (memoDetails.isEmpty()) {
            Helper.showToast(this, "Memo details is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        val formData = JSONObject()
        formData.put("crime_memo_category", crimeMemoCategoryNumber)
        formData.put("memo_details", memoDetails)
        formData.put("police_station", policeStationNumber)
        formData.put("utc_timestamp", utcTime)

        return formData
    }

    private fun sendForm(formData: JSONObject, file: ByteArray?, fileName: String?) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token    = Helper.getData(this, Scope.TOKEN)
            val request  = API.postRequest(token!!, API.CRIME_MEMO, formData, file, fileName)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeCrimeMemo()
                }
                Helper.showToast(this, "Crime memo saved", Toast.LENGTH_SHORT)
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
            removeCrimeMemo()
        }
        val savedStr  = Helper.getObject(this, Scope.CRIME_MEMO)!!
        val savedData = JSONObject(savedStr)
        if (fileUtil.haveFile()) {
            fileUtil.saveFile(this)
            formData.put("file_name", fileUtil.getFileName())
        }
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.CRIME_MEMO, savedData.toString())

        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeCrimeMemo() {
        val savedStr  = Helper.getObject(this, Scope.CRIME_MEMO)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.CRIME_MEMO, savedData.toString())
        fileUtil.removeFile(this)
    }
}