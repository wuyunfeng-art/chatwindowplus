package com.chatwindowplus.chat;

import com.chatwindowplus.ChatWindowPlus;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 已收到的聊天记录（按时间倒序保存），以及各分类的未读数。 */
public final class ChatLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("chatwindowplus");

    private final Deque<ChatEntry> entries = new ArrayDeque<>();
    private final Map<ChatCategory, Integer> unread = new EnumMap<>(ChatCategory.class);
    private int nextId;

    public void add(Text message) {
        ChatParser.Parsed parsed = ChatParser.parse(message);
        ChatCategory effective = parsed.category();
        long idleBefore = ChatWindowPlus.idleMillis();
        ChatEntry previous = this.entries.peekFirst();

        // 商店控制面板这类多行区块：字段行跟着上一行走；框线只在上一行不是系统提示时才跟随，
// 这样面板顶部的框线会留给下面的标题行来回填。
        if (effective == ChatCategory.OTHER && previous != null) {
            ChatCategory prev = previous.category();
            if (prev != ChatCategory.OTHER) {
                if (ChatParser.isPanelLabel(parsed.plain())) {
                    effective = prev;
                } else if (ChatParser.isPanelSeparator(parsed.plain()) && prev != ChatCategory.SYSTEM) {
                    effective = prev;
                }
            }
        }
        if (ChatWindowPlus.config().debugChat) {
            LOGGER.info("[CWP] {} | {}", effective, parsed.plain());
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int tick = client.inGameHud != null ? client.inGameHud.getTicks() : 0;
        ChatWindowPlus.noteActivity();
        com.chatwindowplus.ui.ChatWindowRenderer.onChatMessage();
        if (idleBefore > 5000L) {
            LOGGER.info("[CWP] 新消息（此前空闲 {} ms）", idleBefore);
        }
        Text source = message.copy();
        boolean interactive = ChatParser.hasClickEvent(source);
        ChatEntry entry = new ChatEntry(
                this.nextId++,
                effective,
                interactive ? ChatParser.themedLine(source) : parsed.line(),
                source,
                parsed.plain(),
                parsed.sender() == null ? "" : parsed.sender(),
                parsed.body() == null ? parsed.plain() : parsed.body(),
                parsed.tags(),
                System.currentTimeMillis(),
                tick);
        this.entries.addFirst(entry);

        // 面板顶部的框线在标题出现时还不知道属于哪个分类，这里回填一下。
        if (effective != ChatCategory.OTHER && previous != null && previous.category() == ChatCategory.OTHER
                && ChatParser.isPanelSeparator(previous.plain())) {
            previous.setCategory(effective);
        }
        int limit = Math.max(50, ChatWindowPlus.config().maxMessages);
        while (this.entries.size() > limit) {
            this.entries.removeLast();
        }
        for (ChatCategory category : ChatCategory.tabs()) {
            this.unread.merge(category, 1, Integer::sum);
        }
    }

    /** 重新生成所有消息的富文本（改文字颜色后调用）。 */
    public void rebuildAll() {
        for (ChatEntry entry : this.entries) {
            if (entry.source() != null && ChatParser.hasClickEvent(entry.source())) {
                entry.refreshLine(ChatParser.themedLine(entry.source()));
            } else {
                entry.refreshLine(ChatParser.buildLine(entry.sender(), entry.tags(), entry.body()));
            }
        }
    }

    public List<ChatEntry> all() {
        return new ArrayList<>(this.entries);
    }

    public List<ChatEntry> filtered(ChatCategory category) {
        if (category == null || category == ChatCategory.ALL) {
            return this.all();
        }
        List<ChatEntry> out = new ArrayList<>();
        for (ChatEntry entry : this.entries) {
            if (entry.category() == category) {
                out.add(entry);
            }
        }
        return out;
    }

    public int unread(ChatCategory category) {
        return this.unread.getOrDefault(category, 0);
    }

    public void markRead(ChatCategory category) {
        if (category == ChatCategory.ALL) {
            this.unread.clear();
        } else {
            this.unread.put(category, 0);
        }
    }

    public void clear() {
        this.entries.clear();
        this.unread.clear();
    }

    public int size() {
        return this.entries.size();
    }

    public List<ChatEntry> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(this.entries));
    }
}