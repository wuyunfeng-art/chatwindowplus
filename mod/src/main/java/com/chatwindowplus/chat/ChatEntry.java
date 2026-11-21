package com.chatwindowplus.chat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/**
 * 一条已经解析、排版好的聊天消息。换行结果按可用宽度缓存，避免每帧重复计算。
 */
public final class ChatEntry {
    private ChatCategory category;
    private Text line;
    private final String plain;
    private final String sender;
    private final String body;
    private final List<String> tags;
    private final long createdAt;
    private final int tick;
    private final int id;

    private List<OrderedText> wrapped;
    private int wrappedWidth = -1;

    public ChatEntry(int id, ChatCategory category, Text line, String plain, String sender, String body,
                     List<String> tags, long createdAt, int tick) {
        this.id = id;
        this.category = category;
        this.line = line;
        this.plain = plain;
        this.sender = sender;
        this.body = body;
        this.tags = tags;
        this.createdAt = createdAt;
        this.tick = tick;
    }

    public int id() {
        return this.id;
    }

    public ChatCategory category() {
        return this.category;
    }

    /** 面板续行回填时用：把已经入队的一行改到别的分类。 */
    public void setCategory(ChatCategory category) {
        if (category != null) {
            this.category = category;
        }
    }

    public Text line() {
        return this.line;
    }

    public String plain() {
        return this.plain;
    }

    public String sender() {
        return this.sender;
    }

    public String body() {
        return this.body;
    }

    public List<String> tags() {
        return this.tags;
    }

    public long createdAt() {
        return this.createdAt;
    }

    public int tick() {
        return this.tick;
    }

    /** 颜色等样式变化后重建这一行的富文本。 */
    public void refreshLine(Text newLine) {
        this.line = newLine;
        this.wrapped = null;
        this.wrappedWidth = -1;
    }

    public List<OrderedText> wrapped(TextRenderer font, int width) {
        int w = Math.max(24, width);
        if (this.wrapped == null || this.wrappedWidth != w) {
            this.wrapped = font.wrapLines(this.line, w);
            this.wrappedWidth = w;
        }
        return this.wrapped;
    }

    /** 该条消息在某宽度下占用的行数。 */
    public int lineCount(TextRenderer font, int width) {
        return this.wrapped(font, width).size();
    }

    /** 第一条可点击/可打开的链接。 */
    public String firstUrl() {
        String[] parts = this.plain.split("\\s+");
        for (String part : parts) {
            if (part.startsWith("http://") || part.startsWith("https://")) {
                return part.replaceAll("[),.;!?，。；！？]+$", "");
            }
        }
        return null;
    }

    public static List<String> emptyTags() {
        return new ArrayList<>();
    }
}