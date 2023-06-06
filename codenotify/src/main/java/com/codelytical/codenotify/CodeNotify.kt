package com.codelytical.codenotify

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.codelytical.codenotify.config.CodeNotifyChannelConfig
import com.codelytical.codenotify.datastore.AppDataStoreImpl
import com.codelytical.codenotify.extensions.CodeNotifyNotFiredException
import com.codelytical.codenotify.extensions.dataStore
import com.codelytical.codenotify.model.NotificationImportanceLevel
import com.codelytical.codenotify.model.NotificationTypes
import com.codelytical.codenotify.worker.LocalNotificationPostWorker
import java.util.*

object CodeNotify {

	private lateinit var appDataStoreImpl: AppDataStoreImpl

	/**
	 * @param context A context for on-demand initialization.
	 * @param classPathWillBeOpen A string path for on-demand initialization. The activity's
	 *                            path that you want to open when the notification is
	 *                            clicked. Make sure the class path match with your project
	 *                            architecture. Example: "com.notification.man.MainActivity"
	 * @param title Title text of the notification. (optional)
	 * @param desc Description text of the notification. (optional)
	 * @param thumbnailUrl Large and small icon of the notification. (optional)
	 * @param timeInterval Time interval for scheduling the notification. Needs seconds. Default
	 *                     value is 5 seconds. (optional)
	 * @param type Type of the notification. Default one is text. (optional)
	 */
	data class Builder(
		private val context: Context,
		private val classPathWillBeOpen: String,
		private var title: String? = null,
		private var desc: String? = null,
		private var thumbnailUrl: String? = null,
		private var timeInterval: Long? = null,
		private var type: Int = NotificationTypes.TEXT.type,
		private var config: CodeNotifyChannelConfig? = null,
		private var targetComponentType: String? = null,
		private var customObject: String? = null
	) {
		fun setTitle(title: String?) = apply { this.title = title }

		fun setDescription(desc: String?) = apply { this.desc = desc }

		fun setThumbnailUrl(thumbnailUrl: String?) = apply { this.thumbnailUrl = thumbnailUrl }

		fun setTimeInterval(timeInterval: Long?) = apply { this.timeInterval = timeInterval }

		fun setNotificationType(type: Int) = apply { this.type = type }

		fun setNotificationChannelConfig(config: CodeNotifyChannelConfig) = apply {
			this.config = config
		}

		fun setTargetComponentType(targetComponentType: String) = apply {
			this.targetComponentType = targetComponentType
		}
		fun setCustomObject(customObject: String?) = apply { this.customObject = customObject }

		fun trigger() {
			appDataStoreImpl = AppDataStoreImpl(context.dataStore)
			triggerNotification()
		}

		private fun triggerNotification() {
			val data = Data.Builder().apply {
				putString(LocalNotificationPostWorker.CLASS_PATH_KEY, classPathWillBeOpen)
				putString(LocalNotificationPostWorker.TARGET_COMPONENT_TYPE_KEY, targetComponentType)
				putString(LocalNotificationPostWorker.TITLE_KEY, title)
				putString(LocalNotificationPostWorker.DESC_KEY, desc)
				putString(LocalNotificationPostWorker.THUMBNAIL_URL_KEY, thumbnailUrl)
				putLong(
					LocalNotificationPostWorker.TIME_INTERVAL_KEY,
					timeInterval ?: LocalNotificationPostWorker.DEFAULT_TIME_INTERVAL
				)
				putInt(LocalNotificationPostWorker.TYPE_KEY, type)

				putString(
					LocalNotificationPostWorker.NOTIFICATION_CHANNEL_ID_KEY,
					if (config?.channelId.isNullOrBlank().not())
						config?.channelId
					else
						context.getString(R.string.default_notification_channel_id)
				)
				putString(
					LocalNotificationPostWorker.NOTIFICATION_CHANNEL_NAME_KEY,
					if (config?.channelName.isNullOrBlank().not())
						config?.channelName
					else
						context.getString(R.string.default_notification_channel_name)
				)
				putInt(
					LocalNotificationPostWorker.NOTIFICATION_IMPORTANCE_LEVEL_KEY,
					config?.importanceLevel?.level ?: NotificationImportanceLevel.DEFAULT.level
				)
				putBoolean(
					LocalNotificationPostWorker.NOTIFICATION_SHOW_BADGE_KEY,
					config?.showBadge ?: true
				)
				putString(LocalNotificationPostWorker.OBJECT_KEY, customObject)

			}

			val localNotifyPostWorkRequest = OneTimeWorkRequestBuilder<LocalNotificationPostWorker>()
				.setInputData(data.build())
				.build()
			WorkManager.getInstance(context).enqueue(localNotifyPostWorkRequest)
		}
	}

	suspend fun coolDownLatestFire(
		context: Context
	) {
		if (::appDataStoreImpl.isInitialized.not())
			throw CodeNotifyNotFiredException(
				warning = context.getString(R.string.notification_man_not_fired_error_message)
			)
		else {
			appDataStoreImpl.getFirstWorkerId()?.let { id ->
				cancelLastWorker(
					context = context,
					id = id
				)
				appDataStoreImpl.deleteWorkerId(id = id)
			}
		}
	}

	suspend fun coolDownAllFires(context: Context) {
		if (::appDataStoreImpl.isInitialized.not())
			throw CodeNotifyNotFiredException(
				warning = context.getString(R.string.notification_man_not_fired_error_message)
			)
		else {
			cancelAllWorkers(context)
			appDataStoreImpl.deleteAllWorkerIds()
		}
	}

	private fun cancelLastWorker(
		context: Context,
		id: UUID
	) {
		WorkManager
			.getInstance(context)
			.cancelWorkById(id)
	}

	private fun cancelAllWorkers(context: Context) {
		WorkManager
			.getInstance(context)
			.cancelAllWork()
	}
}