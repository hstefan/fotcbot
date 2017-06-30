package com.hstefan.fotcbot

import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.TelegramBotsApi
import org.telegram.telegrambots.exceptions.TelegramApiException
import org.telegram.telegrambots.logging.BotLogger
import org.telegram.telegrambots.logging.BotsFileHandler
import java.io.IOException
import java.util.logging.ConsoleHandler
import java.util.logging.Level


fun initFotcLogger(config: Config) {
    BotLogger.setLevel(Level.ALL)
    BotLogger.registerLogger(ConsoleHandler())
    try {
        BotLogger.registerLogger(BotsFileHandler())
    } catch (e: IOException) {
        BotLogger.severe(config.logTag, e)
    }
}

fun main(args: Array<String>) {
    val config = Config()
    try {
        config.load("fotcbot.properties")
    } catch (e: IOException) {
        config.save("fotcbot.properties")
    }

    initFotcLogger(config)

    ApiContextInitializer.init()

    val telegramBotsApi = TelegramBotsApi()
    try {
        telegramBotsApi.registerBot(FotcBot(config))
    } catch(e: TelegramApiException) {
        e.printStackTrace()
    }
}
