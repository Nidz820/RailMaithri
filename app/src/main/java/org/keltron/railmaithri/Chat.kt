package org.keltron.railmaithri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject


class Chat : AppCompatActivity() {
    private lateinit var officersSP  : Spinner
    private lateinit var officers    : JSONArray
    private lateinit var messageET   : EditText
    private lateinit var messageList : LinearLayout
    private lateinit var clientNT    : OkHttpClient
    private lateinit var token       : String
    private lateinit var sendBT      : Button

    private var receiverID           : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat)
        supportActionBar!!.hide()

        clientNT    = OkHttpClient().newBuilder().build()
        token       = Helper.getData(this, Scope.TOKEN)!!
        messageList = findViewById(R.id.message_list)
        officersSP  = findViewById(R.id.receiver)
        messageET   = findViewById(R.id.message)
        officersSP.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                messageList.removeAllViews()
                receiverID = officers.getJSONObject(position).getInt("id")
                CoroutineScope(Dispatchers.IO).launch {  fetchMessages(receiverID)  }
            }
        }
        sendBT = findViewById(R.id.send)
        sendBT.setOnClickListener {
            if(messageET.text.isNotEmpty()){
                sendBT.isClickable = false
                CoroutineScope(Dispatchers.IO).launch {
                    sendMessage(messageET.text.toString())
                    Handler(Looper.getMainLooper()).post { sendBT.isClickable = true }
                    fetchMessages(receiverID)
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {  fetchOfficersList()  }
    }

    private fun sendMessage(message: String) {

    }

    private fun fetchOfficersList(){
        val request  = API.getRequest(API.OFFICERS_IN_PS, token)
        val response = clientNT.newCall(request).execute()
        if (response.isSuccessful) {
            officers = JSONArray(response.body!!.string())
            Handler(Looper.getMainLooper()).post {
                officersSP.adapter = Helper.makeArrayAdapter(officers, this)
            }
        } else {
            Log.d("RailMaithri", "Failed to fetch officers list")
        }
    }

    private fun fetchMessages(receiverID: Int){
        val url       = API.OFFICER_MESSAGES + "?id=&receiver=$receiverID"
        val request   = API.getRequest(url, token)
        val response  = clientNT.newCall(request).execute()

        if (response.isSuccessful) {
            val messagesObj = JSONObject(response.body!!.string())
            val messages    = messagesObj.getJSONArray("results")

            for (i in 0 until messages.length()) {
                val message   = messages.getJSONObject(i).getString("message_label")
                val messageTV = TextView(this)
                messageTV.textSize = 20f
                messageTV.text     = message
                Handler(Looper.getMainLooper()).post {
                    messageList.addView(messageTV)
                }
            }
        } else {
            Log.d("RailMaithri", "Failed to fetch messages")
        }
    }
}