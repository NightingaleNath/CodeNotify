package com.codelytical.codenotify

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

class NotificationActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_notification)

		handleNotificationIntent(intent)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		handleNotificationIntent(intent)
	}

	private fun handleNotificationIntent(intent: Intent?) {
		if (intent?.getBooleanExtra("fragment_target", false) == true) {
			val value = intent.getBooleanExtra("fragment_target", false)

			Toast.makeText(this, "$value", Toast.LENGTH_LONG).show()
		}
	}
}