package com.hstefan.fotcbot

import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class Config {
    val name: String
        get() = props.getProperty("name")
    val apiKey: String
        get() = props.getProperty("apikey")
    val logTag: String
        get() = props.getProperty("logtag")

    private var props: Properties = Properties()

    init {
        props.setProperty("name", "devfotcbot")
        props.setProperty("apikey", "REDACTED")
        props.setProperty("logtag", "devfotcbot")
    }

    fun save(path: String) {
        val outFile = FileOutputStream(path)
        props.store(outFile, null)
    }

    fun load(path: String) {
        val inFile = FileInputStream(path)
        props.load(inFile)
    }
}
