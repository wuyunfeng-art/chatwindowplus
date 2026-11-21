package com.chatwindowplus.chat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * 聊天分类。界面左侧的彩色小标签、底部的筛选页签都来自这里。
 */
public enum ChatCategory {
    ALL("chatwindowplus.category.all", 0xFFE8EEF5, false),
    WORLD("chatwindowplus.category.world", 0xFF5BC8F5, true),
    NEARBY("chatwindowplus.category.nearby", 0xFF7ED88F, true),
    PRIVATE("chatwindowplus.category.private", 0xFFFF9AD5, true),
    SYSTEM("chatwindowplus.category.system", 0xFF9AA8FF, true),
    ECONOMY("chatwindowplus.category.economy", 0xFFFFC266, true),
    BATTLE("chatwindowplus.category.battle", 0xFFFF7A7A, true),
    OTHER("chatwindowplus.category.other", 0xFF9DB0C4, true);

    private final String translationKey;
    private final int color;
    private final boolean filterable;

    ChatCategory(String translationKey, int color, boolean filterable) {
        this.translationKey = translationKey;
        this.color = color;
        this.filterable = filterable;
    }

    public int color() {
        return this.color;
    }

    public boolean filterable() {
        return this.filterable;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public MutableText label() {
        return Text.translatable(this.translationKey);
    }

    /** 页签上显示的分类（不含“全部”）。 */
    public static ChatCategory[] tabs() {
        return new ChatCategory[] {WORLD, NEARBY, PRIVATE, SYSTEM, ECONOMY, BATTLE, OTHER};
    }
}