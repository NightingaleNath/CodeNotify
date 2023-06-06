package com.codelytical.codenotify

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.codelytical.codenotify.config.CodeNotifyChannelConfig
import com.codelytical.codenotify.model.NotificationImportanceLevel
import com.codelytical.codenotify.model.NotificationTypes
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar

class MainActivity : AppCompatActivity() {

	private lateinit var notificationPreference: SharedPreferences

	private val notificationPermissionLauncher =
		registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		requestNotificationPermission()

		notificationPreference = PreferenceManager.getDefaultSharedPreferences(this)

		val hymn = Hymn(
			title = "Notification Man",
			lyrics = "I want to share my “Notification Man” 3rd party Android library with you. This library’s superpower is firing scheduled local notifications.\n" +
					"\n",
			author = "Nathaniel Nkrumah"
		)

		if (shouldTriggerNotificationToday()) {
			triggerCodeNotify(
				title = "Match update",
				desc = "Arsenal goal in added time, score is now 3-0",
				4,
				targetComponentType = "activity", myHymn = hymn
			)
		}

		handleNotificationIntent(intent)

	}

	private fun requestNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			notificationPermissionLauncher.launch(
				android.Manifest.permission.POST_NOTIFICATIONS
			)
	}

	private fun triggerCodeNotify(
		title: String?,
		desc: String?,
		timeInterval: Long?,
		targetComponentType: String,
		myHymn: Hymn
	) {
		val serializedHymn = serializeHymn(myHymn)

		CodeNotify
			.Builder(
				context = this,
				classPathWillBeOpen = "com.codelytical.codenotify.MainActivity"
			)
			.setTitle(title = title)
			.setDescription(desc = desc)
			//.setThumbnailUrl(thumbnailUrl = THUMBNAIL_URL)
			.setTargetComponentType(targetComponentType = targetComponentType)
			.setCustomObject(customObject = serializedHymn)
			.setTimeInterval(timeInterval = timeInterval)
			.setNotificationType(type = NotificationTypes.TEXT.type)
			.setNotificationChannelConfig(config = createNotificationManChannelConfig())
			.trigger()

		updateLastNotificationDate()
	}

	private fun createNotificationManChannelConfig() =
		CodeNotifyChannelConfig
			.Builder()
			.setChannelId(id = "notification-man-channel")
			.setChannelName(name = "custom-channel-name")
			.setImportanceLevel(level = NotificationImportanceLevel.HIGH)
			.setShowBadge(shouldShow = true)
			.build()

	private fun shouldTriggerNotificationToday(): Boolean {
		val lastNotificationDate = notificationPreference.getString("last_notification_date", null)
		val currentDate = Calendar.getInstance()

		val lastDate = if (lastNotificationDate != null) {
			val dateParts = lastNotificationDate.split("-")
			val year = dateParts[0].toInt()
			val month = dateParts[1].toInt() - 1 // Calendar months are zero-based
			val day = dateParts[2].toInt()

			Calendar.getInstance().apply {
				set(Calendar.YEAR, year)
				set(Calendar.MONTH, month)
				set(Calendar.DAY_OF_MONTH, day)
			}
		} else {
			null
		}

		return lastDate == null || !isSameDay(currentDate, lastDate)
	}

	private fun isSameDay(calendar1: Calendar, calendar2: Calendar): Boolean {
		return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
				calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH) &&
				calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH)
	}

	private fun updateLastNotificationDate() {
		val currentDate = Calendar.getInstance()
		val formattedDate =
			"${currentDate.get(Calendar.YEAR)}-${currentDate.get(Calendar.MONTH) + 1}-${
				currentDate.get(Calendar.DAY_OF_MONTH)
			}"
		notificationPreference.edit()
			.putString("last_notification_date", formattedDate)
			.apply()
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		handleNotificationIntent(intent)
	}

	private fun handleNotificationIntent(intent: Intent?) {
		val value = intent?.getBooleanExtra("fragment_target", false)
		val notificationString = intent?.getStringExtra("customObject")

		if (notificationString != null) {
			val notificationData = deserializeHymn(notificationString)

			if (value == true) {
				Toast.makeText(this, "${notificationData.title}", Toast.LENGTH_LONG).show()
			} else {
				Toast.makeText(this, "${notificationData.title}\n ${notificationData.lyrics}", Toast.LENGTH_LONG).show()
			}
		} else {
			// Handle the case where notificationString is null
			// For example, show a default toast message or perform alternative logic
		}
	}


	companion object {
		private var THUMBNAIL_URL = R.mipmap.ic_launcher_round
//			"https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Android_robot.max-200x200.png"
	}

	private fun deserializeHymn(serializedHymn: String): Hymn {
		return try {
			val hymnObject = JSONObject(serializedHymn)
			val title = hymnObject.getString("title")
			val lyrics = hymnObject.getString("lyrics")
			val author = hymnObject.getString("author")

			Hymn(title, lyrics, author)
		} catch (e: JSONException) {
			throw IllegalArgumentException("Invalid serialized Hymn data")
		}
	}


	private fun serializeHymn(hymn: Hymn): String {
		val hymnObject = JSONObject()
		hymnObject.put("title", hymn.title)
		hymnObject.put("lyrics", hymn.lyrics)
		hymnObject.put("author", hymn.author)
		// Add other properties of Hymn class

		return hymnObject.toString()
	}

}