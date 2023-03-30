package org.keltron.railmaithri

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject


class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        supportActionBar!!.hide()

        val profile  = JSONObject(Helper.getData(this, Scope.PROFILE)!!)
        val username = profile.getString("username")
        findViewById<TextView>(R.id.profile_name).text = username

        val logoutButton = findViewById<ImageButton>(R.id.logout)
        logoutButton.setOnClickListener {
            Helper.saveData(this, Scope.TOKEN, "")
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        val addIncidentBT    = findViewById<ImageView>(R.id.add_incident_report)
        val searchIncidentBT = findViewById<Button>(R.id.search_incident_report)
        val savedIncidentBT  = findViewById<Button>(R.id.saved_incident_report)
        addIncidentBT.setOnClickListener {
            val intent = Intent(this, IncidentReport::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        savedIncidentBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.INCIDENT_REPORT)
            startActivity(intent)
        }
        searchIncidentBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.INCIDENT_REPORT)
            startActivity(intent)
        }

        val addPassengerStatisticsBT    = findViewById<ImageView>(R.id.add_passenger_statistics)
        val searchPassengerStatisticsBT = findViewById<Button>(R.id.search_passenger_statistics)
        val savedPassengerStatisticsBT  = findViewById<Button>(R.id.saved_passenger_statistics)
        addPassengerStatisticsBT.setOnClickListener {
            val intent = Intent(this, PassengerStatistics::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        savedPassengerStatisticsBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.PASSENGER_STATISTICS)
            startActivity(intent)
        }
        searchPassengerStatisticsBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.PASSENGER_STATISTICS)
            startActivity(intent)
        }

        val addStrangerCheckBT    = findViewById<ImageView>(R.id.add_stranger_check)
        val searchStrangerCheckBT = findViewById<Button>(R.id.search_stranger_check)
        val savedStrangerCheckBT  = findViewById<Button>(R.id.saved_stranger_check)
        addStrangerCheckBT.setOnClickListener {
            val intent = Intent(this, StrangerCheck::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        searchStrangerCheckBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.STRANGER_CHECK)
            startActivity(intent)
        }
        savedStrangerCheckBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.STRANGER_CHECK)
            startActivity(intent)
        }

        val addBeatDiaryBT    = findViewById<ImageView>(R.id.add_beat_diary)
        val searchBeatDiaryBT = findViewById<Button>(R.id.search_beat_diary)
        val savedBeatDiaryBT  = findViewById<Button>(R.id.saved_beat_diary)
        addBeatDiaryBT.setOnClickListener {
            val intent = Intent(this, BeatDiary::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        searchBeatDiaryBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.BEAT_DIARY)
            startActivity(intent)
        }
        savedBeatDiaryBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.BEAT_DIARY)
            startActivity(intent)
        }

        val addEmergencyContactsBT    = findViewById<ImageView>(R.id.add_emergency_contacts)
        val searchEmergencyContactsBT = findViewById<Button>(R.id.search_emergency_contacts)
        val savedEmergencyContactsBT  = findViewById<Button>(R.id.saved_emergency_contacts)
        addEmergencyContactsBT.setOnClickListener {
            val intent = Intent(this, EmergencyContacts::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        searchEmergencyContactsBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.EMERGENCY_CONTACTS)
            startActivity(intent)
        }
        savedEmergencyContactsBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.EMERGENCY_CONTACTS)
            startActivity(intent)
        }

        val addLostPropertyBT    = findViewById<ImageView>(R.id.add_lost_property)
        val searchLostPropertyBT = findViewById<Button>(R.id.search_lost_property)
        val savedLostPropertyBT  = findViewById<Button>(R.id.saved_lost_property)
        addLostPropertyBT.setOnClickListener {
            val intent = Intent(this, LostProperty::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        searchLostPropertyBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.LOST_PROPERTY)
            startActivity(intent)
        }
        savedLostPropertyBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.LOST_PROPERTY)
            startActivity(intent)
        }

        val addAbandonedPropertyBT    = findViewById<ImageView>(R.id.add_abandoned_property)
        val searchAbandonedPropertyBT = findViewById<Button>(R.id.search_abandoned_property)
        val savedAbandonedPropertyBT  = findViewById<Button>(R.id.saved_abandoned_property)
        addAbandonedPropertyBT.setOnClickListener {
            val intent = Intent(this, AbandonedProperty::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        searchAbandonedPropertyBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.ABANDONED_PROPERTY)
            startActivity(intent)
        }
        savedAbandonedPropertyBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.ABANDONED_PROPERTY)
            startActivity(intent)
        }

        val addReliablePersonBT    = findViewById<ImageView>(R.id.add_reliable_person)
        val searchReliablePersonBT = findViewById<Button>(R.id.search_reliable_person)
        val savedReliablePersonBT  = findViewById<Button>(R.id.saved_reliable_person)
        addReliablePersonBT.setOnClickListener {
            val intent = Intent(this, ReliablePerson::class.java)
            intent.putExtra("mode", Scope.MODE_NEW_FORM)
            startActivity(intent)
        }
        searchReliablePersonBT.setOnClickListener {
            val intent = Intent(this, SearchData::class.java)
            intent.putExtra("scope", Scope.RELIABLE_PERSON)
            startActivity(intent)
        }
        savedReliablePersonBT.setOnClickListener {
            val intent = Intent(this, SavedData::class.java)
            intent.putExtra("scope", Scope.RELIABLE_PERSON)
            startActivity(intent)
        }
    }
}