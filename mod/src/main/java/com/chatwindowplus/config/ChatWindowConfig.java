package com.chatwindowplus.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户配置。第一次运行会生成 config/chatwindowplus.json，之后每次打开聊天窗口时
 * 若文件有改动会自动重新加载，不需要重启游戏。
 */
public final class ChatWindowConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("chatwindowplus");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;

    /** 面板宽度占屏幕比例，以及最小/最大宽度（GUI 像素）。 */
    public double panelWidthRatio = 0.335;
    public int minPanelWidth = 200;
    public int maxPanelWidth = 600;

    /** 未打开聊天时保留的行数（0 表示不显示悬浮聊天栏）。 */
    public int hudMaxLines = 8;
    /** 打开聊天时最多展示的行数。 */
    public int openMaxLines = 40;

    /** 打开聊天时，面板高度最多占屏幕高度的比例（约等于参考图红框）。 */
    public double openHeightRatio = 0.44;

    /** 背景不透明度（0~1）。 */
    public double hudPanelOpacity = 0.40;
    public double openPanelOpacity = 0.40;

    /** 面板位置（GUI 像素）：距屏幕左边缘、距底部。 */
    public int panelOffsetX = 4;
    public int panelOffsetY = 14;

    /** 圆角半径。 */
    public int cornerRadius = 5;

    /** 颜色（#RRGGBB）。 */
    public String panelColor = "#0A111C";
    public String borderColor = "#233A50";
    public String textColor = "#CBD8E6";
    public String nameColor = "#F4F8FF";
    public String tagColor = "#7E92A8";
    /** 面板渐变色（上/下），以及装饰纹理与切角样式。 */
    public String panelColorTop = "#0A111C";
    public String panelColorBottom = "#0E1B2A";
    public String decor = "bubbles";
    public boolean chamfer = false;

    /** 强调色：页签选中、输入框描边等。 */
    public String accentColor = "#5BC8F5";
    /** 7 个分类色签颜色：世界、附近、私聊、系统、经济、战斗、其他。 */
    public java.util.List<String> categoryColors = new java.util.ArrayList<>(java.util.List.of(
            "#5BC8F5", "#7ED88F", "#FF9AD5", "#9AA8FF", "#FFC266", "#FF7A7A", "#9DB0C4"));

    /** 是否显示底部筛选页签 / 称号标签。 */
    public boolean showTabs = true;
    public boolean showTags = true;

    /** 聊天记录条数上限。 */
    public int maxMessages = 400;

    /** 默认选中的分类：ALL / WORLD / NEARBY / PRIVATE / SYSTEM / ECONOMY / BATTLE / OTHER。 */
    public String defaultFilter = "ALL";

    public boolean hudEnabled = true;

    /**
     * 调试开关：开启后每条聊天消息都会把「分类 + 原文」写进 logs/latest.log，
     * 方便排查“私聊/世界/经济”分类没命中的问题。默认关闭。
     */
    public boolean debugChat = false;

    /** 自定义分类规则：从上到下匹配，命中即采用。pattern 为正则，category 取值见 defaultFilter。 */
    public java.util.List<Rule> rules = new java.util.ArrayList<>();

    public static final class Rule {
        public String category = "OTHER";
        public String pattern = "";

        public Rule() {
        }

        public Rule(String category, String pattern) {
            this.category = category;
            this.pattern = pattern;
        }
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("chatwindowplus.json");
    }

    public static ChatWindowConfig load() {
        Path path = file();
        ChatWindowConfig config = new ChatWindowConfig();
        try {
            if (Files.exists(path)) {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                ChatWindowConfig loaded = GSON.fromJson(json, ChatWindowConfig.class);
                if (loaded != null) {
                    JsonObject object = JsonParser.parseString(json).getAsJsonObject();
                    if (!object.has("panelColorTop")) {
                        // 旧配置：没有渐变/装饰字段，沿用原来的纯色面板
                        loaded.panelColorTop = loaded.panelColor;
                        loaded.panelColorBottom = loaded.panelColor;
                        loaded.decor = "none";
                    }
                    config = loaded;
                }
            }
            config.save();
        } catch (JsonSyntaxException e) {
            LOGGER.warn("配置文件格式有误，已使用默认配置: {}", e.getMessage());
        } catch (IOException e) {
            LOGGER.warn("读取配置失败: {}", e.getMessage());
        }
        return config;
    }

    public void save() {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("保存配置失败: {}", e.getMessage());
        }
    }

    public static long lastModified() {
        try {
            Path path = file();
            return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }

    /** 用另一份配置覆盖当前对象（用于“恢复默认”）。 */
    public void copyFrom(ChatWindowConfig o) {
        this.enabled = o.enabled;
        this.panelWidthRatio = o.panelWidthRatio;
        this.minPanelWidth = o.minPanelWidth;
        this.maxPanelWidth = o.maxPanelWidth;
        this.hudMaxLines = o.hudMaxLines;
        this.openMaxLines = o.openMaxLines;
        this.openHeightRatio = o.openHeightRatio;
        this.hudPanelOpacity = o.hudPanelOpacity;
        this.openPanelOpacity = o.openPanelOpacity;
        this.panelOffsetX = o.panelOffsetX;
        this.panelOffsetY = o.panelOffsetY;
        this.cornerRadius = o.cornerRadius;
        this.panelColor = o.panelColor;
        this.panelColorTop = o.panelColorTop;
        this.panelColorBottom = o.panelColorBottom;
        this.decor = o.decor;
        this.chamfer = o.chamfer;
        this.borderColor = o.borderColor;
        this.textColor = o.textColor;
        this.nameColor = o.nameColor;
        this.tagColor = o.tagColor;
        this.accentColor = o.accentColor;
        this.categoryColors = new java.util.ArrayList<>(o.categoryColors);
        this.showTabs = o.showTabs;
        this.showTags = o.showTags;
        this.maxMessages = o.maxMessages;
        this.defaultFilter = o.defaultFilter;
        this.hudEnabled = o.hudEnabled;
        this.debugChat = o.debugChat;
        this.rules = o.rules;
    }

    public double widthRatio() {
        return Math.max(0.2, Math.min(0.8, this.panelWidthRatio));
    }

    public int maxMessagesOrDefault() {
        return Math.max(50, this.maxMessages);
    }
}
