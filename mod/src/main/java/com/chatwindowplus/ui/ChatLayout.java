package com.chatwindowplus.ui;

/**
 * 面板布局。所有坐标都是 GUI 像素（已按 GUI Scale 缩放）。
 */
public record ChatLayout(
        boolean open,
        int screenW,
        int screenH,
        int panelX,
        int panelY,
        int panelW,
        int panelH,
        int listX,
        int listY,
        int listW,
        int listH,
        int textX,
        int textW,
        int lineH,
        int chipW,
        int tabY,
        int tabH,
        int inputX,
        int inputY,
        int inputW,
        int inputH) {

    public int visibleRows() {
        return Math.max(1, this.listH / this.lineH);
    }

    public int listBottom() {
        return this.listY + this.listH;
    }
}