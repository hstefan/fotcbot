package com.hstefan.fotcbot

import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.api.objects.Message
import org.telegram.telegrambots.api.objects.Update
import org.telegram.telegrambots.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.exceptions.TelegramApiException
import org.telegram.telegrambots.logging.BotLogger
import java.util.regex.Pattern


class FotcBot(private val config: Config) : TelegramLongPollingBot() {
    private val commandPattern = Pattern.compile("""/(\w*)@?\w*\s*(.*)$""")

    override fun getBotUsername() = config.name

    override fun getBotToken() = config.apiKey

    override fun onUpdateReceived(update: Update) {
        when {
            update.hasInlineQuery() -> onInlineQuery(update.inlineQuery)
            update.hasMessage() -> onMessage(update.message)
        }
    }

    private fun onMessage(message: Message) {
        val matcher = commandPattern.matcher(message.text)
        if (!matcher.matches())
            return

        val args = if (matcher.groupCount() > 1) matcher.group(2) else ""
        when (matcher.group(1)) {
            "me" -> handleMeCommand(message, args)
            "greet" -> handleGreetCommand(message)
        }
    }

    private fun handleGreetCommand(message: Message) {
        val m = SendMessage()
        m.text = "Hello, %s!".format(message.from.firstName)
        m.setChatId(message.chatId)
        try {
            sendMessage(m)
        } catch(e: TelegramApiException) {
            BotLogger.error(config.logTag, e)
        }
    }

    private fun handleMeCommand(message: Message, args: String) {
        val content = args.trim()
        if (content.isEmpty()) {
            replyWithErrorMessage(message,
                "At least one argument must be provided to the /me command.")
            return
        }

        val replacingMessage = SendMessage()
        replacingMessage.enableMarkdown(true)
        replacingMessage.setChatId(message.chatId)
        replacingMessage.text = "_%s %s_".format(message.from.firstName, content)
        try {
            sendMessage(replacingMessage)
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

    private fun onInlineQuery(inlineQuery: InlineQuery) {
        TODO("dem memes!")
    }
}
