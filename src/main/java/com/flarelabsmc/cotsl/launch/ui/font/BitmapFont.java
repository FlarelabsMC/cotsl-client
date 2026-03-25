package com.flarelabsmc.cotsl.launch.ui.font;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class BitmapFont {
    public static final int CELL = 8;
    public static final float SCALE = 2;
    public static final int ADVANCE = 6;

    private BitmapFont() {}

    public static void drawString(Graphics2D g, BufferedImage ascii, String text, int x, int y, Color color) {
        drawString(g, ascii, text, x, y, color, SCALE);
    }

    public static int getWidth(String text) {
        return getWidth(text, SCALE);
    }

    public static int getHeight() {
        return getHeight(SCALE);
    }

    public static void drawString(Graphics2D g, BufferedImage ascii, String text, int x, int y, Color color, float scale) {
        if (ascii == null || text == null || text.isEmpty()) return;
        int textW = (int) Math.ceil((text.length() - 1) * ADVANCE * scale + CELL * scale);
        int textH = (int) Math.ceil(CELL * scale);
        BufferedImage tmp = new BufferedImage(Math.max(1, textW), Math.max(1, textH), BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = tmp.createGraphics();
        tg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int i = 0; i < text.length(); i++) {
            int code = text.charAt(i);
            int col = code % 16, row = code / 16;
            int sx = col * CELL, sy = row * CELL;
            tg.drawImage(ascii,
                    (int)(i * ADVANCE * scale), 0,
                    (int)(i * ADVANCE * scale + CELL * scale), textH,
                    sx, sy, sx + CELL, sy + CELL, null);
        }
        tg.setComposite(AlphaComposite.SrcAtop);
        tg.setColor(color);
        tg.fillRect(0, 0, textW, textH);
        tg.dispose();
        g.drawImage(tmp, x, y, null);
    }

    public static int getWidth(String text, float scale) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil((text.length() - 1) * ADVANCE * scale + CELL * scale);
    }

    public static int getHeight(float scale) {
        return (int) Math.ceil(CELL * scale);
    }
}
