package org.no_cyrillic;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class NoCyrillicSpamBot extends TelegramLongPollingBot {
    private static final Logger logger = Logger.getLogger(NoCyrillicSpamBot.class.getName());
    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\u0400-\\u04FF\\u0500-\\u052F\\u2DE0-\\u2DFF\\uA640-\\uA69F]");

    public NoCyrillicSpamBot() {
        super(Dotenv.load().get("BOT_TOKEN"));
    }

    @Override
    public String getBotUsername() {
        return "noCyrillicSpamBot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            try {
                processMessage(update);
            } catch (TelegramApiException e) {
                logger.severe("Error processing message: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    private void processMessage(Update update) throws TelegramApiException {
        String text = update.getMessage().getText();
        if (containsCyrillic(text) || containsEmoji(text)) {
            removeAndBanUser(update);
        }
    }

    private boolean containsCyrillic(String text) {
        return text.codePoints()
                .mapToObj(Character.UnicodeBlock::of)
                .anyMatch(unicodeBlock -> unicodeBlock.equals(Character.UnicodeBlock.CYRILLIC));
    }

    private boolean containsEmoji(String text) {
        return EMOJI_PATTERN.matcher(text).find();
    }

    private void removeAndBanUser(Update update) throws TelegramApiException {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(update.getMessage().getChatId());
        deleteMessage.setMessageId(update.getMessage().getMessageId());
        this.execute(deleteMessage);

        BanChatMember banChatMember = new BanChatMember();
        banChatMember.setChatId(update.getMessage().getChatId());
        banChatMember.setUserId(update.getChatMember().getFrom().getId());
        banChatMember.setRevokeMessages(true);
        this.execute(banChatMember);
    }
}