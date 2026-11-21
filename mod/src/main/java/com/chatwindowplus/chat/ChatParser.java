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
            "^\\s*[\\[【『]\\s*(G|L|R|世界|全球|全服|附近|本地|周边|周围|身边|交易|经济|市场|商店|私聊|私信|密语|系统|公告|战斗|战报|PVP|MSG|PM|DM|TELL)\\s*[\\]】』]\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG = Pattern.compile("^\\s*\\[([^\\]]{1,24})]\\s*");
    private static final Pattern NAME_ASCII = Pattern.compile("^\\s*[^\\sA-Za-z0-9_]{0,4}\\s*([A-Za-z0-9_]{2,16})\\s*[:：>]\\s*(.*)$");
    private static final Pattern NAME_ANY = Pattern.compile("^\\s*([^\\s:：>]{1,16})\\s*[:：>]\\s*(.*)$");
    private static final Pattern URL = Pattern.compile("https?://\\S+");

    /** 私聊强标记：不管出现在哪一段都当成私聊。覆盖原版“悄悄地说”和英文 whisper。 */
    private static final Pattern PRIVATE_STRONG = Pattern.compile(
            "悄悄|密语|私聊你|私信你|whispers? to you|you whisper",
            Pattern.CASE_INSENSITIVE);
    /** 私聊标签：`[私聊]` `【私信】` `[MSG]` `[PM]` `[DM]` `[tell]` 等写法。 */
    private static final Pattern PRIVATE_TAG = Pattern.compile(
            "^\\s*[\\[【『]\\s*(私聊|私信|密语|msg|pm|dm|tell|whisper|w)\\s*[\\]】』]",
            Pattern.CASE_INSENSITIVE);
    /** 定向箭头：`A → 你` `A -> 你` `你 → A` 这类私聊格式。 */
    private static final Pattern PRIVATE_ARROW = Pattern.compile(
            "(?:->|→|»|〉)\\s*你\\s*(?=[\\]】』:：,，]|$)|你\\s*(?:->|→|»|〉)\\s*[^\\s:：]{1,16}");
    /** 公共频道前缀：带这些前缀的消息是公开频道，不再按“私聊”字样判定。 */
    private static final Pattern PUBLIC_PREFIX = Pattern.compile(
            "^\\s*(?:\\[[^\\]]{1,8}]\\s*)*\\[(?:G|世界|全球|附近|本地|周边|交易|经济|市场)]");

    /** 私聊固定格式：`小明 → 你: 内容` / `小明 悄悄地对你说：内容`，拆成发送者 + 正文。 */
    private static final Pattern PRIVATE_FROM = Pattern.compile(
            "^(.{1,16}?)\\s*(?:->|→|»|〉)\\s*你\\s*[:：]?\\s*(.*)$");
    private static final Pattern PRIVATE_SAY = Pattern.compile(
            "^(.{1,16}?)\\s*(?:悄悄地对你说|悄悄对你说|对你说)\\s*[:：]?\\s*(.*)$");

    /**
     * 死亡信息。各服务端会自定义死法，这里分三类兜住：
     * <ol>
     *   <li>“被…杀死 / 击杀 / 干掉”，名字和称号可能很长，所以放宽到 60 字</li>
     *   <li>“用[武器]杀…”，武器名再长也能命中</li>
     *   <li>“战斗时被…”“被烤得 / 烧得 / 冻成 …”这类自定义死法</li>
     * </ol>
     */
    private static final Pattern DEATH = Pattern.compile(
            "被[^。！？!?]{0,60}?(?:杀死|杀害|击杀|干掉)"
                    + "|用\\[[^\\]]{1,24}]杀"
                    + "|战斗时被|在与[^。！？!?]{0,40}战斗"
                    + "|试图在熔岩里游泳|从高处摔了下来|刺死|炸死|烧死|淹死|饿死|凋零|摔死|死于"
                    + "|被[^。！？!?]{0,40}(?:烤得|烧得|冻成|电死|毒死|窒息|压成|绞死|劈死|咬死|扎死|捅死)");
    private static final Pattern JOIN_LEAVE = Pattern.compile("加入了游戏|退出了游戏|加入了服务器|离开了服务器|加入了世界|离开了世界");
    /** 系统播报：`来自地牢的声音：…` 这类 NPC / 剧情提示。 */
    private static final Pattern SYSTEM_BROADCAST = Pattern.compile("^来自[^：:]{1,12}的声音");

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

        // 私聊的固定句式（“小明 → 你: 内容”“小明 悄悄地对你说：内容”）再补一次拆分，
        // 上面两条通用规则认不出箭头/口语句式。
        if (category == ChatCategory.PRIVATE) {
            Matcher pm = PRIVATE_FROM.matcher(rest);
            if (!pm.matches()) {
                pm = PRIVATE_SAY.matcher(rest);
            }
            if (pm.matches()) {
                String who = pm.group(1).trim();
                if (!who.isEmpty() && !who.equals("你")) {
                    sender = who;
                    body = pm.group(2).trim();
                }
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
        String head = s.length() > 48 ? s.substring(0, 48) : s;

        // 1) 开头的频道标签最可靠：[附近]【本地】[世界] [交易] [私聊]…，也兼容
        //    「[副本][附近]」这种先区域后频道的写法
        ChatCategory tagged = byTags(head);
        if (tagged != null) {
            return tagged;
        }

        if (s.startsWith("隐秘的声音") || s.startsWith("【系统】") || s.startsWith("[系统]") || s.startsWith("[公告]")
                || SYSTEM_BROADCAST.matcher(s).lookingAt()) {
            return ChatCategory.SYSTEM;
        }
        if (isPrivateChat(head)) {
            return ChatCategory.PRIVATE;
        }
        if (head.contains("[交易]") || head.contains("[经济]") || head.contains("[市场]")) {
            return ChatCategory.ECONOMY;
        }
        if (head.contains("[G]") || s.startsWith("[世界]") || s.startsWith("[全球]")) {
            return ChatCategory.WORLD;
        }
        // 个别服务端不带方括号，直接以频道名开头：“附近 小明：…”
        if (head.startsWith("附近") || head.startsWith("本地") || head.startsWith("周边") || head.startsWith("周围")) {
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

    /**
     * 私聊识别。各服务端/插件的写法差别很大，这里按「先强后弱」判断：
     * <ol>
     *   <li>标签开头：{@code [私聊]} {@code 【私信】} {@code [MSG]} {@code [PM]} {@code [DM]} {@code [tell]}</li>
     *   <li>公共频道前缀（{@code [G]}/{@code [世界]}/{@code [交易]}…）直接判否，
     *       避免把“世界频道里提到私聊”误判成私聊</li>
     *   <li>强标记：悄悄 / 密语 / whispers to you / you whisper</li>
     *   <li>定向箭头：{@code A → 你} / {@code A -> 你} / {@code 你 → A}</li>
     *   <li>弱标记：前 48 个字符里出现「私聊 / 私信 / 对你说」</li>
     * </ol>
     */
    private static boolean isPrivateChat(String head) {
        if (PRIVATE_TAG.matcher(head).lookingAt()) {
            return true;
        }
        if (PUBLIC_PREFIX.matcher(head).lookingAt()) {
            return false;
        }
        if (PRIVATE_STRONG.matcher(head).find()) {
            return true;
        }
        if (PRIVATE_ARROW.matcher(head).find()) {
            return true;
        }
        return head.contains("私聊") || head.contains("私信") || head.contains("对你说");
    }

    /**
     * 扫描消息开头的 1~3 个标签（{@code [附近]} / {@code 【本地】} / {@code 『世界』}），
     * 按标签内容判断频道。先区域后频道的写法（{@code [副本][附近]}）也能命中。
     */
    private static ChatCategory byTags(String head) {
        int index = 0;
        int count = 0;
        while (index < head.length() && count < 3) {
            char c = head.charAt(index);
            if (Character.isWhitespace(c) || c == '|' || c == '｜') {
                index++;
                continue;
            }
            char close;
            if (c == '[') {
                close = ']';
            } else if (c == '【') {
                close = '】';
            } else if (c == '『') {
                close = '』';
            } else if (c == '《') {
                close = '》';
            } else {
                break;
            }
            int end = head.indexOf(close, index + 1);
            if (end < 0) {
                break;
            }
            ChatCategory category = tagCategory(head.substring(index + 1, end));
            if (category != null) {
                return category;
            }
            index = end + 1;
            count++;
        }
        return null;
    }

    /** 单个标签到分类的映射；认不出的标签返回 null，继续看下一个标签。 */
    private static ChatCategory tagCategory(String token) {
        String t = token.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.equalsIgnoreCase("g") || t.contains("世界") || t.contains("全球") || t.contains("全服")) {
            return ChatCategory.WORLD;
        }
        if (t.equalsIgnoreCase("l") || t.equalsIgnoreCase("r")
                || t.contains("附近") || t.contains("本地") || t.contains("周边")
                || t.contains("周围") || t.contains("身边")) {
            return ChatCategory.NEARBY;
        }
        if (t.contains("交易") || t.contains("经济") || t.contains("市场") || t.contains("商店")) {
            return ChatCategory.ECONOMY;
        }
        if (t.equalsIgnoreCase("msg") || t.equalsIgnoreCase("pm") || t.equalsIgnoreCase("dm")
                || t.equalsIgnoreCase("tell") || t.contains("私聊") || t.contains("私信") || t.contains("密语")) {
            return ChatCategory.PRIVATE;
        }
        if (t.contains("系统") || t.contains("公告")) {
            return ChatCategory.SYSTEM;
        }
        if (t.contains("战斗") || t.contains("战报") || t.equalsIgnoreCase("pvp")) {
            return ChatCategory.BATTLE;
        }
        return null;
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