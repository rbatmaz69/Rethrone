package com.example.androidlauncher.data

import org.json.JSONArray
import org.json.JSONObject

object FolderSerializer {

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
        }
        return list
    }

    fun serializeFolders(folders: List<FolderInfo>): String {
        val jsonArray = JSONArray()
        for (folder in folders) {
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            val appsArray = JSONArray()
            folder.appPackageNames.forEach { appsArray.put(it) }
            obj.put("apps", appsArray)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}

