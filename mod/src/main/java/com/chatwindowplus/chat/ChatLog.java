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

/** 已收到的聊天记录（按时间倒序保存），以及各分类的未读数。 */
public final class ChatLog {
    private final Deque<ChatEntry> entries = new ArrayDeque<>();
    private final Map<ChatCategory, Integer> unread = new EnumMap<>(ChatCategory.class);
    private int nextId;

    public void add(Text message) {
        ChatParser.Parsed parsed = ChatParser.parse(message);
        MinecraftClient client = MinecraftClient.getInstance();
        int tick = client.inGameHud != null ? client.inGameHud.getTicks() : 0;
        ChatEntry entry = new ChatEntry(
                this.nextId++,
                parsed.category(),
                parsed.line(),
                parsed.plain(),
                parsed.sender() == null ? "" : parsed.sender(),
                parsed.body() == null ? parsed.plain() : parsed.body(),
                parsed.tags(),
                System.currentTimeMillis(),
                tick);
        this.entries.addFirst(entry);
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
            entry.refreshLine(ChatParser.buildLine(entry.sender(), entry.tags(), entry.body()));
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