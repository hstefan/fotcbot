package com.hstefan.fotcbot

import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.TelegramBotsApi
import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.api.objects.Chat
import org.telegram.telegrambots.api.objects.Message
import org.telegram.telegrambots.api.objects.Update
import org.telegram.telegrambots.api.objects.User
import org.telegram.telegrambots.bots.AbsSender
import org.telegram.telegrambots.bots.commandbot.TelegramLongPollingCommandBot
import org.telegram.telegrambots.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.exceptions.TelegramApiException
import org.telegram.telegrambots.logging.BotLogger
import org.telegram.telegrambots.logging.BotsFileHandler
import java.io.IOException
import java.util.logging.ConsoleHandler
import java.util.logging.Level


class GreetCommand(private var config: Config) : BotCommand("greet", "Greet a user.") {

    override fun execute(absSender: AbsSender?, user: User?, chat: Chat?, arguments: Array<out String>?) {
        val m = SendMessage()
        m.text = "Hello, %s!".format(user?.firstName)
        m.setChatId(chat?.id)
        try {
            absSender?.sendMessage(m)
        } catch(e: TelegramApiException) {
            BotLogger.error(config.logTag, e)
        }
    }
}

class FotcBot(private var config: Config) : TelegramLongPollingCommandBot(config.name) {

    override fun getBotToken() = config.apiKey

    override fun processNonCommandUpdate(update: Update?) {
        if (update?.hasMessage()!!) {
            val msgText = update.message.text
            if (msgText.startsWith("/me")) {
                BotLogger.info(config.logTag, "Handling /me")
                handleMeCommand(update.message)
            }
        }
    }

    override fun filter(message: Message?): Boolean {
        return message?.isChannelMessage!!
    }

    private fun handleMeCommand(message: Message) {
        val argsIndex = message.text.indexOf(' ')
        if (argsIndex < 0) {
            replyWithErrorMessage(message,
                "At least one argument must be provided to the /me command.")
            return
        }

        val msgText = message.text.substring(argsIndex).trim()
        val replacingMesssage = SendMessage()
        replacingMesssage.enableMarkdown(true)
        replacingMesssage.setChatId(message.chatId)
        replacingMesssage.text = "_%s %s_".format(message.from.firstName, msgText)
        try {
            sendMessage(replacingMesssage)
        } catch (e: TelegramApiException) {
            BotLogger.error(config.logTag, e)
        }

        val deletedMessage = DeleteMessage(message.chatId, message.messageId)
        try {
            deleteMessage(deletedMessage)
        } catch (e: TelegramApiException) {
            BotLogger.error(config.logTag, e)
        }
    }

    private fun replyWithErrorMessage(message: Message, text: String) {
        val errorMessage = SendMessage(message.chatId, text)
        errorMessage.replyToMessageId = message.messageId
        try {
            sendMessage(errorMessage)
        } catch (e: TelegramApiException) {
            BotLogger.error(config.logTag, e)
        }
    }

    init {
        register(GreetCommand(this.config))
    }
}

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
