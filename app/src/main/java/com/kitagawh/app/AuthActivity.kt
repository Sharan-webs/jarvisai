package com.kitagawh.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Simple, honest sign-in: name + mobile number + age group to create an
 * account; just the number to sign in. The number is checked against the
 * SIM in this phone whenever Android lets us read it.
 */
class AuthActivity : BaseActivity() {

    private lateinit var message: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        setTitle("Welcome")

        message = findViewById(R.id.authMessage)

        val ages = listOf(
            "Below 13", "13 - 17", "18 - 24", "25 - 34", "35 - 49", "50 - 64", "65+"
        )
        val spinner = findViewById<Spinner>(R.id.ageSpinner)
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, ages
        )

        findViewById<MaterialButton>(R.id.createAccountButton).setOnClickListener {
            createAccount(spinner)
        }
        findViewById<MaterialButton>(R.id.signInButton).setOnClickListener {
            signIn()
        }
    }

    private fun createAccount(spinner: Spinner) {
        val name = findViewById<TextInputEditText>(R.id.nameInput).text?.toString()?.trim().orEmpty()
        val number = findViewById<TextInputEditText>(R.id.numberInput).text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            showMessage("Please type your name.")
            return
        }
        if (number.filter { it.isDigit() }.length < 10) {
            showMessage("Please type a proper mobile number.")
            return
        }
        val sim = SimHelper.simNumber(this)
        if (sim != null && !SimHelper.numbersMatch(sim, number)) {
            showMessage(
                "That number does not match the SIM in this phone (SIM ends with " +
                    sim.takeLast(4) + "). Type the number of this phone, or put that SIM in."
            )
            return
        }
        Prefs.putString(this, Prefs.KEY_NAME, name)
        Prefs.putString(this, Prefs.KEY_NUMBER, number)
        Prefs.putString(this, Prefs.KEY_AGE, spinner.selectedItem?.toString() ?: "")
        showSuccess("Account created. Welcome, $name.")
        navigate(PermissionsActivity::class.java)
        finish()
    }

    private fun signIn() {
        val number = findViewById<TextInputEditText>(R.id.signInNumberInput)
            .text?.toString()?.trim().orEmpty()
        if (number.isEmpty()) {
            showMessage("Type your mobile number to sign in.")
            return
        }
        val saved = Prefs.getString(this, Prefs.KEY_NUMBER)
        if (saved.isBlank()) {
            showMessage("No account exists on this phone yet. Create an account first, above.")
            return
        }
        if (!SimHelper.numbersMatch(saved, number)) {
            showMessage("That number does not match the account on this phone. Check the number and try again.")
            return
        }
        val sim = SimHelper.simNumber(this)
        if (sim != null && !SimHelper.numbersMatch(sim, number)) {
            showMessage(
                "This number is the account number, but the SIM in this phone is different (SIM ends with " +
                    sim.takeLast(4) + "). Put the account SIM in this phone to sign in."
            )
            return
        }
        val name = Prefs.getString(this, Prefs.KEY_NAME)
        showSuccess("Signed in. Welcome back, $name.")
        navigate(PermissionsActivity::class.java)
        finish()
    }

    private fun showMessage(text: String) {
        message.setTextColor(getColor(R.color.error_red))
        message.text = text
    }

    private fun showSuccess(text: String) {
        message.setTextColor(getColor(R.color.success_green))
        message.text = text
    }
}
