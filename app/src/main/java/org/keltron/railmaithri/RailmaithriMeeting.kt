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

class RailmaithriMeeting: AppCompatActivity() {

    private lateinit var progressPB:            ProgressBar
    private lateinit var saveBT:                Button
    private lateinit var participantsET:        EditText
    private lateinit var gistOfDecisionTakenET: EditText
    private lateinit var meetingDateET:         EditText
    private lateinit var nextMeetingDateET:     EditText
    private lateinit var meetingTypeSP:         Spinner

    private lateinit var meetingTypeAP:         ArrayAdapter<String>

    private lateinit var mode:                  String
    private lateinit var meetingTypes:          JSONArray
    private lateinit var utcTime:               String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.railmaithri_meeting)
        supportActionBar!!.hide()
        utcTime               = Helper.getUTC()
        mode                  = intent.getStringExtra("mode")!!
        progressPB            = findViewById(R.id.progress_bar)
        saveBT                = findViewById(R.id.save)
        meetingTypeSP         = findViewById(R.id.meeting_type)
        participantsET        = findViewById(R.id.participants)
        gistOfDecisionTakenET = findViewById(R.id.decision_taken)
        meetingDateET         = findViewById(R.id.meeting_date)
        nextMeetingDateET     = findViewById(R.id.next_meeting_date)

        meetingTypes  = JSONArray(Helper.getData(this, Scope.MEETING_TYPES)!!)
        meetingTypeAP = Helper.makeArrayAdapter(meetingTypes, this)
        meetingTypeSP.adapter = meetingTypeAP

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
            val savedData = intent.getStringExtra("saved_data")
            val formData  = JSONObject(savedData!!)
            utcTime = formData.getString("utc_timestamp")
            populateForm(formData)
        }
    }

    private fun populateForm(data: JSONObject) {
        meetingDateET.setText(data.getString("meeting_date"))
        nextMeetingDateET.setText(data.getString("next_meeting_date"))
        participantsET.setText(data.getString("participants"))
        gistOfDecisionTakenET.setText(data.getString("gist_of_decisions_taken"))

        val meetingTypeID   = data.getInt("meeting_type")
        val meetingTypeName = Helper.getName(meetingTypes, meetingTypeID)
        val meetingTypePos  = meetingTypeAP.getPosition(meetingTypeName)
        meetingTypeSP.setSelection(meetingTypePos)
    }

    private fun validateInput(): JSONObject? {
        val meetingDate           = meetingDateET.text.toString()
        val participants          = participantsET.text.toString()
        val gistOfDecisionTaken   = gistOfDecisionTakenET.text.toString()
        val nextMeetingDate       = nextMeetingDateET.text.toString()

        val meetingTypePos    = meetingTypeSP.selectedItemPosition
        val meetingTypeNumber = meetingTypes.getJSONObject(meetingTypePos).getString("id").toString()
        if (meetingDate.isEmpty()) {
            Helper.showToast(this, "Meeting date is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (participants.isEmpty()) {
            Helper.showToast(this, "Participants is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (gistOfDecisionTaken.isEmpty()) {
            Helper.showToast(this, "gist of decision taken is mandatory", Toast.LENGTH_SHORT)
            return null
        }
        if (nextMeetingDate.isEmpty()) {
            Helper.showToast(this, "Next meeting date is mandatory", Toast.LENGTH_SHORT)
            return null
        }

        val formData = JSONObject()
        formData.put("meeting_date", meetingDate)
        formData.put("participants", participants)
        formData.put("gist_of_decisions_taken", gistOfDecisionTaken)
        formData.put("next_meeting_date", nextMeetingDate)
        formData.put("utc_timestamp", utcTime)
        formData.put("meeting_type", meetingTypeNumber)
        return formData
    }

    private fun sendForm(formData: JSONObject) {
        try {
            val clientNT = OkHttpClient().newBuilder().build()
            val token = Helper.getData(this, Scope.TOKEN)
            val request  = API.postRequest(token!!, API.RAILMAITHRI_MEETING, formData, file = null, fileName = null)
            val response = clientNT.newCall(request).execute()
            if (response.isSuccessful) {
                if (mode == Scope.MODE_UPDATE_FORM) {
                    removeRailmaithriMeeting()
                }
                Helper.showToast(this, "Railmaithri meeting saved", Toast.LENGTH_SHORT)
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
            removeRailmaithriMeeting()
        }
        val savedStr  = Helper.getObject(this, Scope.RAILMAITHRI_MEETING)!!
        val savedData = JSONObject(savedStr)
        savedData.put(utcTime, formData)
        Helper.saveData(this, Scope.RAILMAITHRI_MEETING, savedData.toString())
        val message = "Server unreachable, data saved in phone memory"
        Helper.showToast(this, message, Toast.LENGTH_LONG)
        finish()
    }

    private fun removeRailmaithriMeeting() {
        val savedStr  = Helper.getObject(this, Scope.RAILMAITHRI_MEETING)!!
        val savedData = JSONObject(savedStr)
        savedData.remove(utcTime)
        Helper.saveData(this, Scope.RAILMAITHRI_MEETING, savedData.toString())
    }
}