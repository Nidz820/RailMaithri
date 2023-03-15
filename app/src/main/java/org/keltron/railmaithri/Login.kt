package org.keltron.railmaithri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar

class Login : AppCompatActivity() {
    lateinit var progressPB:    ProgressBar
    lateinit var usernameET:    EditText
    lateinit var passwordET:    EditText
    lateinit var loginBT:       Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        supportActionBar!!.hide()

        progressPB = findViewById(R.id.progressBar)
        usernameET = findViewById(R.id.username)
        passwordET = findViewById(R.id.password)
        loginBT    = findViewById(R.id.login)

        progressPB.visibility = View.GONE
    }
}