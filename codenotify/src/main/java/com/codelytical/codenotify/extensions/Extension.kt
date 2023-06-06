package com.codelytical.codenotify.extensions

import org.json.JSONArray
import org.json.JSONObject

const val COMPONENT_TYPE_FRAGMENT = "fragment"

fun serializeObjectData(objectData: Any): String {
	return when (objectData) {
		is String -> objectData
		is JSONObject -> objectData.toString()
		is JSONArray -> objectData.toString()
		else -> throw IllegalArgumentException("Unsupported object data type")
	}
}