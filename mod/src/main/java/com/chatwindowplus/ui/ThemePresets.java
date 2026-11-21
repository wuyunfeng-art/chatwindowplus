package com.chatwindowplus.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置主题。每套主题包含：
 * 面板渐变（上/下）、边框、文字、名字、标签、强调色、7 个分类色签，
 * 以及装饰纹理（余烬、星点、花瓣、红石线路……）和是否使用 MC 式切角。
 */
public final class ThemePresets {
    private ThemePresets() {
    }

    public record Preset(String name, String panel, String border, String text, String nameColor, String tag,
                         String accent, String top, String bottom, String decor, boolean chamfer,
                         List<String> categories) {
    }

    public static final List<Preset> ALL = List.of(
            new Preset("深海蓝", "#0A111C", "#233A50", "#CBD8E6", "#F4F8FF", "#7E92A8", "#5BC8F5",
                    "#0A111C", "#10202F", "bubbles", false,
                    List.of("#5BC8F5", "#7ED88F", "#FF9AD5", "#9AA8FF", "#FFC266", "#FF7A7A", "#9DB0C4")),
            new Preset("下界熔岩", "#160806", "#5A2A18", "#F2D6C4", "#FFEAD9", "#B98A72", "#FF8A5C",
                    "#120605", "#3A1409", "embers", true,
                    List.of("#FF8A5C", "#FFC46B", "#FF7AA8", "#D9A066", "#FFB347", "#FF4D4D", "#B08A7A")),
            new Preset("末地虚空", "#0B0714", "#3B2A5A", "#D9D0F0", "#F0E9FF", "#8E7FB8", "#B58CFF",
                    "#0B0714", "#1E1338", "stars", true,
                    List.of("#B58CFF", "#8CE0C8", "#FF9AD5", "#A6A0FF", "#FFD27F", "#FF6B8A", "#9B92B8")),
            new Preset("钻石矿洞", "#06141A", "#1E4A57", "#C9EEF5", "#ECFEFF", "#6FA8B8", "#4FE0FF",
                    "#06141A", "#0E3442", "facets", true,
                    List.of("#4FE0FF", "#6BF0B0", "#FF9AD5", "#8FB8FF", "#FFD166", "#FF7A7A", "#8FA9B5")),
            new Preset("红石工程", "#120606", "#4A1A1A", "#F0D2D2", "#FFEDED", "#A87C7C", "#FF5C5C",
                    "#120606", "#320C0C", "circuit", true,
                    List.of("#FF6B6B", "#FFB26B", "#FF8FC7", "#C9A0A0", "#FFC46B", "#FF3B3B", "#9B8484")),
            new Preset("樱花原野", "#160D12", "#4A2A38", "#F5DDE6", "#FFF0F6", "#B58A9C", "#FFB7D5",
                    "#160D12", "#331722", "petals", true,
                    List.of("#FFB7D5", "#A8E6A1", "#FF9AD5", "#C7B3FF", "#FFD9A0", "#FF8A8A", "#B59AA8")),
            new Preset("金苹果", "#14100A", "#4A3A1A", "#F2E6C8", "#FFF6DC", "#B8A06A", "#FFD166",
                    "#14100A", "#32240F", "glints", true,
                    List.of("#FFD166", "#B8E986", "#FF9AD5", "#A8C8FF", "#FFC04D", "#FF7A5C", "#B0A08A")),
            new Preset("苔石地牢", "#08100A", "#2A4A2E", "#D6E8D2", "#EEFBE8", "#86A88A", "#8FE08F",
                    "#08100A", "#16281A", "moss", true,
                    List.of("#8FE08F", "#B8E986", "#FF9AD5", "#A8C8FF", "#FFD166", "#FF7A6B", "#90A894")),
            new Preset("石英大厅", "#14161A", "#3A3F45", "#E4E8EC", "#FAFCFF", "#9AA3AC", "#E8EEF5",
                    "#14161A", "#262B33", "bevel", true,
                    List.of("#E8EEF5", "#B8E6C0", "#FFC7E0", "#C9D4FF", "#FFE0A8", "#FF9B9B", "#B8C2CC")));

    public static int indexOf(String panelColor, String borderColor, String textColor, String nameColor,
                              String tagColor, String accentColor, String top, String bottom, String decor,
                              boolean chamfer, List<String> categories) {
        for (int i = 0; i < ALL.size(); i++) {
            Preset preset = ALL.get(i);
            if (eq(preset.panel(), panelColor) && eq(preset.border(), borderColor) && eq(preset.text(), textColor)
                    && eq(preset.nameColor(), nameColor) && eq(preset.tag(), tagColor)
                    && eq(preset.accent(), accentColor) && eq(preset.top(), top) && eq(preset.bottom(), bottom)
                    && eq(preset.decor(), decor) && preset.chamfer() == chamfer
                    && sameCategories(preset.categories(), categories)) {
                return i;
            }
        }
        return -1;
    }

    public static List<String> copyCategories(Preset preset) {
        return new ArrayList<>(preset.categories());
    }

    private static boolean eq(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private static boolean sameCategories(List<String> a, List<String> b) {
        if (b == null || a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!eq(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }
}