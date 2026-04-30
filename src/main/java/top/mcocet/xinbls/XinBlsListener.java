package top.mcocet.xinbls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.EventPriority;
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.events.*;

import top.mcocet.xinbls.logger.LogFileManager;

public class XinBlsListener implements Listener {
    private final LogFileManager logFileManager;
    private static final Logger logger = LoggerFactory.getLogger(XinBlsListener.class);

    public XinBlsListener(LogFileManager logFileManager) {
        this.logFileManager = logFileManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String msg = LangManager.get("xinbls.event.player.join",
                event.getPlayerProfile().getName());
        logFileManager.writeLog(msg);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeave(PlayerLeaveEvent event) {
        String msg = LangManager.get("xinbls.event.player.leave",
                event.getPlayerProfile().getName());
        logFileManager.writeLog(msg);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPublicChat(PublicChatEvent event) {
        String msg = LangManager.get("xinbls.event.chat.public",
                event.getSender().getName(), event.getMessage());
        logFileManager.writeLog(msg);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrivateChat(PrivateChatEvent event) {
        String msg = LangManager.get("xinbls.event.chat.private",
                event.getSender().getName(), event.getMessage());
        logFileManager.writeLog(msg);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSystemChat(SystemChatMessageEvent event) {
        if (event.isOverlay()) return;
        String text = event.getText();
        if (text == null || text.isBlank()) return;
        String msg = LangManager.get("xinbls.event.chat.system", text);
        logFileManager.writeLog(msg);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginSuccess(LoginSuccessEvent event) {
        logFileManager.writeLog(LangManager.get("xinbls.event.login.success"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisconnect(DisconnectEvent event) {
        String reason = xin.bbtt.mcbot.Utils.toString(event.getReason());
        logFileManager.writeLog(LangManager.get("xinbls.event.disconnect", reason));
    }
}
