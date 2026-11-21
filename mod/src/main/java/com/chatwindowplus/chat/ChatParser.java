package com.chatwindowplus.chat;

import com.chatwindowplus.ChatWindowPlus;
import com.chatwindowplus.ui.Theme;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * 把服务器发来的原始聊天文本拆成：分类、称号/区域标签、说话人、正文，
 * 再重新拼成统一风格的富文本。
 */
public final class ChatParser {
    private static final Pattern REGION = Pattern.compile("^\\s*\\[([^\\]]{1,14})]\\s*(?=\\[(?:G|世界|全球|附近|本地|交易|经济|私聊|系统|战斗)])");
    private static final Pattern CHANNEL = Pattern.compile(
            "^\\s*\\[(G|L|R|世界|全球|附近|本地|周边|交易|经济|市场|私聊|系统|公告|战斗|PVP)]\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG = Pattern.compile("^\\s*\\[([^\\]]{1,24})]\\s*");
    private static final Pattern NAME_ASCII = Pattern.compile("^\\s*[^\\sA-Za-z0-9_]{0,4}\\s*([A-Za-z0-9_]{2,16})\\s*[:：>]\\s*(.*)$");
    private static final Pattern NAME_ANY = Pattern.compile("^\\s*([^\\s:：>]{1,16})\\s*[:：>]\\s*(.*)$");
    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private static final Pattern DEATH = Pattern.compile(
            "被.{1,24}杀(死|害)|试图在熔岩里游泳|从高处摔了下来|刺死|炸死|烧死|淹死|饿死|凋零|摔死|死于|被.{0,20}击杀");
    private static final Pattern JOIN_LEAVE = Pattern.compile("加入了游戏|退出了游戏|加入了服务器|离开了服务器|加入了世界|离开了世界");

    private ChatParser() {
    }

    public record Parsed(ChatCategory category, String sender, List<String> tags, String body, Text line, String plain) {
    }

    public static Parsed parse(Text message) {
        String plain = message.getString();
        ChatCategory category = categorize(plain);
        String rest = plain;
        List<String> tags = new ArrayList<>(2);

        Matcher region = REGION.matcher(rest);
        if (region.find()) {
            tags.add(region.group(1));
            rest = rest.substring(region.end());
        }

        Matcher channel = CHANNEL.matcher(rest);
        if (channel.find()) {
            rest = rest.substring(channel.end());
        }

        while (tags.size() < 3) {
            Matcher tag = TAG.matcher(rest);
            if (!tag.find()) {
                break;
            }
            String token = tag.group(1).trim();
            if (token.isEmpty() || token.length() > 24) {
                break;
            }
            tags.add(token);
            rest = rest.substring(tag.end());
        }

        rest = rest.trim();
        if (category == ChatCategory.SYSTEM && rest.startsWith("隐秘的声音")) {
            int arrow = rest.indexOf(">>");
            rest = arrow >= 0 ? rest.substring(arrow + 2) : rest.substring("隐秘的声音".length());
        }
        if (rest.startsWith("|") || rest.startsWith("｜")) {
            rest = rest.substring(1);
        }

        String sender = null;
        String body = rest.trim();
        Matcher name = NAME_ASCII.matcher(rest);
        if (!name.matches()) {
            name = NAME_ANY.matcher(rest);
            if (!name.matches()) {
                name = null;
            }
        }
        if (name != null) {
            sender = name.group(1);
            body = name.group(2).trim();
            if (sender.length() < 2 || sender.chars().allMatch(Character::isDigit)) {
                sender = null;
                body = rest.trim();
            }
        }

        MutableText line = buildLine(sender, tags, body);
        return new Parsed(category, sender, tags, body, line, plain);
    }

    public static MutableText buildLine(String sender, List<String> tags, String body) {
        MutableText out = Text.empty();
        if (ChatWindowPlus.config().showTags) {
            for (String tag : tags) {
                out.append(Text.literal("[" + tag + "] ").styled(style -> style.withColor(Theme.TAG)));
            }
        }
        if (sender == null) {
            out.append(styleBody(body, false));
            return out;
        }
        out.append(Text.literal(sender).styled(style -> style.withColor(Theme.NAME)));
        out.append(Text.literal("：").styled(style -> style.withColor(Theme.TEXT_FAINT)));
        out.append(styleBody(body, true));
        return out;
    }

    /** 高亮链接、@ 和玩家自己的名字。 */
    private static MutableText styleBody(String body, boolean mention) {
        MutableText out = Text.empty();
        Matcher url = URL.matcher(body);
        int index = 0;
        while (url.find()) {
            if (url.start() > index) {
                out.append(mentionPart(body.substring(index, url.start()), mention));
            }
            String raw = url.group();
            String cleaned = raw.replaceAll("[),.;!?，。；！？]+$", "");
            String trailing = raw.substring(cleaned.length());
            MutableText link = Text.literal(cleaned).styled(style -> style
                    .withColor(Theme.LINK)
                    .withUnderline(true)
                    .withClickEvent(openUrl(cleaned)));
            out.append(link);
            if (!trailing.isEmpty()) {
                out.append(Text.literal(trailing).styled(style -> style.withColor(Theme.TEXT)));
            }
            index = url.end();
        }
        if (index < body.length()) {
            out.append(mentionPart(body.substring(index), mention));
        }
        return out;
    }

    private static ClickEvent openUrl(String url) {
        try {
            return new ClickEvent.OpenUrl(new URI(url));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static MutableText mentionPart(String text, boolean mention) {
        if (!mention || text.isEmpty()) {
            return Text.literal(text).styled(style -> style.withColor(Theme.TEXT));
        }
        String self = ChatWindowPlus.selfName();
        if (self == null || self.isEmpty()) {
            return Text.literal(text).styled(style -> style.withColor(Theme.TEXT));
        }
        MutableText out = Text.empty();
        int index = 0;
        String lower = text.toLowerCase(Locale.ROOT);
        String needle = self.toLowerCase(Locale.ROOT);
        int found;
        while ((found = lower.indexOf(needle, index)) >= 0) {
            if (found > index) {
                out.append(Text.literal(text.substring(index, found)).styled(style -> style.withColor(Theme.TEXT)));
            }
            out.append(Text.literal(text.substring(found, found + needle.length()))
                    .styled(style -> style.withColor(Theme.MENTION)));
            index = found + needle.length();
        }
        if (index < text.length()) {
            out.append(Text.literal(text.substring(index)).styled(style -> style.withColor(Theme.TEXT)));
        }
        return out;
    }

    public static ChatCategory categorize(String plain) {
        String s = plain.trim();
        if (s.isEmpty()) {
            return ChatCategory.OTHER;
        }
        ChatCategory custom = matchCustomRule(s);
        if (custom != null) {
            return custom;
        }
        String head = s.length() > 40 ? s.substring(0, 40) : s;
        if (s.startsWith("隐秘的声音") || s.startsWith("【系统】") || s.startsWith("[系统]") || s.startsWith("[公告]")) {
            return ChatCategory.SYSTEM;
        }
        if (head.contains("[交易]") || head.contains("[经济]") || head.contains("[市场]")) {
            return ChatCategory.ECONOMY;
        }
        if (s.startsWith("[私聊]") || s.startsWith("私聊") || s.contains("悄悄地对你说") || s.contains("悄悄对你说")) {
            return ChatCategory.PRIVATE;
        }
        if (head.contains("[G]") || s.startsWith("[世界]") || s.startsWith("[全球]")) {
            return ChatCategory.WORLD;
        }
        if (s.startsWith("[附近]") || s.startsWith("[本地]") || s.startsWith("[周边]")) {
            return ChatCategory.NEARBY;
        }
        if (DEATH.matcher(s).find()) {
            return ChatCategory.BATTLE;
        }
        if (JOIN_LEAVE.matcher(s).find()) {
            return ChatCategory.SYSTEM;
        }
        return ChatCategory.OTHER;
    }

    private static final java.util.Map<String, Pattern> RULE_CACHE = new java.util.HashMap<>();

    private static ChatCategory matchCustomRule(String text) {
        java.util.List<com.chatwindowplus.config.ChatWindowConfig.Rule> rules =
                com.chatwindowplus.ChatWindowPlus.config().rules;
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        for (com.chatwindowplus.config.ChatWindowConfig.Rule rule : rules) {
            if (rule == null || rule.pattern == null || rule.pattern.isBlank()) {
                continue;
            }
            try {
                Pattern pattern = RULE_CACHE.computeIfAbsent(rule.pattern,
                        p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE));
                if (pattern.matcher(text).find()) {
                    return ChatWindowPlus.parseFilter(rule.category);
                }
            } catch (RuntimeException ignored) {
                // 用户正则写错时忽略该条，不影响其它规则
            }
        }
        return null;
    }
}