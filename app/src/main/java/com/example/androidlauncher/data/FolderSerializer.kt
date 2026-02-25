package com.example.androidlauncher.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Helper object to serialize and deserialize the list of FolderInfo objects to and from a JSON string.
 */
object FolderSerializer {

    /**
     * Parses a JSON string into a list of FolderInfo objects.
     * Expects a JSON Array string.
     */
    fun parseFolders(jsonString: String): List<FolderInfo> {
        val list = mutableListOf<FolderInfo>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val appsArray = obj.getJSONArray("apps")
                val apps = mutableListOf<String>()
                for (j in 0 until appsArray.length()) {
                    apps.add(appsArray.getString(j))
                }
                list.add(
                    FolderInfo(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        appPackageNames = apps
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of error (corrupted data), return empty list or partial list
        }
        return list
    }

    /**
     * Serializes a list of FolderInfo objects into a JSON string.
     */
    fun serializeFolders(folders: List<FolderInfo>): String {
        val jsonArray = JSONArray()
        folders.forEach { folder ->
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            val appsArray = JSONArray()
            folder.appPackageNames.forEach { pkg ->
                appsArray.put(pkg)
            }
            obj.put("apps", appsArray)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
