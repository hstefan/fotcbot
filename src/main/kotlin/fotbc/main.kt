package fotbc

import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.TelegramBotsApi
import org.telegram.telegrambots.api.methods.send.SendMessage
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


private val LOGTAG = "FOTC"

class GreetCommand : BotCommand("greet", "Greet a user.") {
    override fun execute(absSender: AbsSender?, user: User?, chat: Chat?, arguments: Array<out String>?) {
        val m = SendMessage()
        m.text = "Hello, %s!".format(user?.firstName)
        m.setChatId(chat?.id)
        try {
            absSender?.sendMessage(m)
        } catch(e: TelegramApiException) {
            BotLogger.error(LOGTAG, e)
        }
    }
}

class MeCommand : BotCommand("me", "Replaces \"/me\" with the users first name.") {
    override fun execute(absSender: AbsSender?, user: User?, chat: Chat?, arguments: Array<out String>?) {
        val m = SendMessage()
        m.text = "%s %s".format(user?.firstName, arguments?.joinToString(" "))
        m.setChatId(chat?.id)
        try {
            absSender?.sendMessage(m)
        } catch(e: TelegramApiException) {
            BotLogger.error(LOGTAG, e)
        }
    }

}


class FotcBot : TelegramLongPollingCommandBot("FollowerOfTemercalypsersBot") {

    override fun getBotToken() = ""

    override fun processNonCommandUpdate(update: Update?) {
        BotLogger.info(LOGTAG, "Received non-command update.")
    }

    override fun filter(message: Message?): Boolean {
        return message?.isChannelMessage!!
    }

    init {
        register(GreetCommand())
        register(MeCommand())
    }
}

fun initFotcLogger() {
    BotLogger.setLevel(Level.ALL)
    BotLogger.registerLogger(ConsoleHandler())
    try {
        BotLogger.registerLogger(BotsFileHandler())
    } catch (e: IOException) {
        BotLogger.severe(LOGTAG, e)
    }
}

fun main(args: Array<String>) {
    initFotcLogger()

    ApiContextInitializer.init()

    val telegramBotsApi = TelegramBotsApi()
    try {
        telegramBotsApi.registerBot(FotcBot())
    } catch(e: TelegramApiException) {
        e.printStackTrace()
    }
}
