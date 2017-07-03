package com.hstefan.fotcbot

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.methods.send.SendPhoto
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.api.objects.Message
import org.telegram.telegrambots.api.objects.Update
import org.telegram.telegrambots.api.objects.User
import org.telegram.telegrambots.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.exceptions.TelegramApiException
import org.telegram.telegrambots.logging.BotLogger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.regex.Pattern
import javax.imageio.ImageIO


class FotcBot(private val config: Config) : TelegramLongPollingBot() {
    private val commandPattern = Pattern.compile("""/(\w*)@?\w*\s*(.*)$""")
    private val argPattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*")

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
            "meme" -> handleMemeCommand(message, args)
        }
    }

    private fun handleMemeCommand(message: Message, args: String) {
        val argList = ArrayList<String>()

        val matcher = argPattern.matcher(args)
        while (matcher.find())
            argList.add(matcher.group(1))

        when (argList.size) {
            3 -> makeMemeTopAndBottom(message, argList[0], argList[1], argList[2])
            2 -> makeMemeTopOnly(message, argList[0], argList[1])
            else ->
                replyWithErrorMessage(message, "At least two parameters must be given to /meme.")
        }
    }

    private fun makeMemeTopOnly(message: Message, memeId: String, topText: String) {
        makeMemeTopAndBottom(message, memeId, topText, "_")
    }

    private fun makeMemeTopAndBottom(message: Message, memeId: String, topText: String,
                                     bottomText: String) {
        val memegenStr = { s: String -> s.trim('\"').replace(' ', '_')}

        val idEnc = URLEncoder.encode(memegenStr(memeId), "UTF-8")
        val topEnc = URLEncoder.encode(memegenStr(topText), "UTF-8")
        val bottomEnc = URLEncoder.encode(memegenStr(bottomText), "UTF-8")

        val httpClient = HttpClients.createDefault()
        val httpGet = HttpGet("https://memegen.link/%s/%s/%s.jpg".format(idEnc, topEnc, bottomEnc))
        val httpResp = httpClient.execute(httpGet)
        try {
            val image = ImageIO.read(httpResp.entity.content)
            val os = ByteArrayOutputStream()
            ImageIO.write(image, "jpeg", os)
            val memePhoto = SendPhoto()
            memePhoto.chatId = message.chatId.toString()
            memePhoto.setNewPhoto("meme", ByteArrayInputStream(os.toByteArray()))
            memePhoto.caption = makeMemeCaption(message.from, idEnc)
            sendPhoto(memePhoto)
            deleteMessage(DeleteMessage(message.chatId, message.messageId))
        } catch (connEx: IOException) {
            BotLogger.error(config.logTag, connEx)
            replyWithErrorMessage(message, connEx.message.toString())
        } catch (telegramEx: TelegramApiException) {
            BotLogger.error(config.logTag, telegramEx)
            replyWithErrorMessage(message, telegramEx.message.toString())
        } finally {
            httpResp.close()
        }
    }

    private fun makeMemeCaption(user: User, memeId: String): String {
        if (user.userName != null)
            return "$memeId by @${user.userName}"
        else
            return "$memeId by ${user.firstName} ${user.lastName}"
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
