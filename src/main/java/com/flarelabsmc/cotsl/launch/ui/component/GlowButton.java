package com.flarelabsmc.cotsl.launch.ui.component;

import com.flarelabsmc.cotsl.launch.ui.font.BitmapFont;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class GlowButton extends JComponent {
    private final String text;
    private final Color textColor;
    private final Color glowColor;
    private final int glowRadius;
    private final int arc;
    private final Color fillColor;
    private final Color borderColor;
    private float glowAlpha = 0f;
    private boolean hovered = false;
    private boolean pressed = false;
    private Runnable action;
    private BufferedImage bmpFont = null;
    private final boolean btnRound;
    private float bmpFontScale = BitmapFont.SCALE;

    private final Timer animTimer = new Timer(16, e -> {
        float target = hovered ? 1f : 0f;
        glowAlpha += (target - glowAlpha) * 0.14f;
        if (Math.abs(glowAlpha - target) < 0.005f) {
            glowAlpha = target;
            ((Timer) e.getSource()).stop();
        }
        repaint();
    });

    public void setBmpFontScale(float scale) { this.bmpFontScale = scale; }

    public GlowButton(String text, Color textColor, Color glowColor, int glowRadius, int arc, Color fillColor, Color borderColor, boolean round) {
        this.text = text;
        this.textColor = textColor;
        this.glowColor = glowColor;
        this.glowRadius = glowRadius;
        this.arc = arc;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.btnRound = round;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        animTimer.setInitialDelay(0);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                animTimer.restart();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                pressed = false;
                animTimer.restart(); }
            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressed && hovered && action != null) action.run();
                pressed = false;
                repaint();
            }
        });
    }

    public void addActionListener(Runnable r) {
        this.action = r;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        int bx = glowRadius, by = glowRadius;
        int bw = getWidth()  - glowRadius * 2;
        int bh = getHeight() - glowRadius * 2;
        if (glowAlpha > 0.01f) {
            float r = glowColor.getRed()   / 255f;
            float grn = glowColor.getGreen() / 255f;
            float b = glowColor.getBlue()  / 255f;
            for (int i = glowRadius; i >= 1; i--) {
                float strength = (float)(glowRadius - i + 1) / glowRadius;
                float a = glowAlpha * strength * 0.18f;
                g2.setColor(new Color(r, grn, b, Math.min(a, 1f)));
                g2.fillRoundRect(bx - i, by - i, bw + i * 2, bh + i * 2, arc + i * 2, arc + i * 2);
            }
        }
        float dim = pressed ? 0.7f : 1f;
        g2.setColor(new Color(
                (int) (fillColor.getRed() * dim),
                (int) (fillColor.getGreen() * dim),
                (int) (fillColor.getBlue() * dim)
        ));
        if (btnRound) g2.fillRoundRect(bx, by, bw, bh, arc, arc);
        else g2.fillRect(bx, by, bw, bh);
        g2.setColor(lerp(borderColor, glowColor, glowAlpha));
        g2.setStroke(new BasicStroke(1.5f));
        if (btnRound) g2.drawRoundRect(bx, by, bw - 1, bh - 1, arc, arc);
        else g2.drawRect(bx, by, bw - 1, bh - 1);
        if (bmpFont != null) {
            int tx = bx + (bw - BitmapFont.getWidth(text, bmpFontScale))  / 2;
            int ty = by + (bh - BitmapFont.getHeight(bmpFontScale)) / 2;
            BitmapFont.drawString(g2, bmpFont, text, tx, ty,
                    lerp(textColor, glowColor.brighter(), glowAlpha * 0.4f), bmpFontScale);
        } else {
            Font font = getFont() != null ? getFont() : new Font("SansSerif", Font.BOLD, 18);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int tx = bx + (bw - fm.stringWidth(text)) / 2;
            int ty = by + (bh - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(lerp(textColor, glowColor.brighter(), glowAlpha * 0.4f));
            g2.drawString(text, tx, ty);
        }
        g2.dispose();
    }

    private static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed()   + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue()  + (b.getBlue() - a.getBlue()) * t)
        );
    }

    public void setBmpFont(BufferedImage font) {
        this.bmpFont = font;
    }

    @Override
    public Dimension getPreferredSize() {
        int textW = bmpFont != null
                ? BitmapFont.getWidth(text, bmpFontScale)
                : getFontMetrics(getFont() != null ? getFont() : new Font("SansSerif", Font.BOLD, 18)).stringWidth(text);
        return new Dimension(textW + 80 + glowRadius * 2, 48 + glowRadius * 2);
    }

}
