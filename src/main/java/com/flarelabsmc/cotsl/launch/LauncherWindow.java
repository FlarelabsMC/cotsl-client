package com.flarelabsmc.cotsl.launch;

import com.flarelabsmc.cotsl.launch.ui.component.GlowButton;
import com.flarelabsmc.cotsl.launch.ui.font.BitmapFont;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class LauncherWindow extends JFrame {
    private static final URL BG = LauncherWindow.class.getResource("/assets/cotsl/textures/launch/bg/launch_bg.png");
    private static final BufferedImage ASCII_FONT = loadImage("/assets/cotsl/textures/font/ascii.png");
    private static final BufferedImage CLOSE_ICON_RAW = loadImage("/assets/cotsl/textures/launch/ui/x.png");

    private double targetMX = 0.5, targetMY = 0.5;
    private double smoothMX = 0.5, smoothMY = 0.5;
    private BufferedImage ss = null;
    private float fadeProgress = 0f;
    private boolean fadeActive = false;

    private static BufferedImage loadImage(String resource) {
        URL url = LauncherWindow.class.getResource(resource);
        if (url == null) return null;
        try { return ImageIO.read(url); }
        catch (Exception e) { return null; }
    }

    private LauncherWindow(CountDownLatch latch) {
        super("Crypt of the Second Lord");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(860, 520);
        setMinimumSize(new Dimension(640, 400));
        setLocationRelativeTo(null);
        setUndecorated(true);
        Image bgImage = BG != null ? new ImageIcon(BG).getImage() : null;
        JPanel root = new JPanel(new BorderLayout(0, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    double maxAngle = Math.toRadians(15);
                    double rotY = (smoothMX - 0.5) * 2.0 * maxAngle;
                    double rotX = (smoothMY - 0.5) * 2.0 * maxAngle;
                    double sx = Math.cos(rotY) * 1.15;
                    double sy = Math.cos(rotX) * 1.15;
                    double shx = Math.sin(rotY) * 0.15;
                    double shy = Math.sin(rotX) * 0.15;
                    g2.translate(getWidth() / 2, getHeight() / 2);
                    g2.transform(new AffineTransform(sx, shy, shx, sy, 0, 0));
                    g2.drawImage(
                            bgImage,
                            -getWidth() / 2, -getHeight() / 2,
                            getWidth(), getHeight(),
                            this
                    );
                    g2.dispose();
                }
                if (fadeActive && ss != null) {
                    int blockSize = Math.max(2, (int)(Math.pow(fadeProgress, 0.6) * 64));
                    int tinyW = Math.max(1, getWidth()  / blockSize);
                    int tinyH = Math.max(1, getHeight() / blockSize);
                    BufferedImage tiny = new BufferedImage(tinyW, tinyH, BufferedImage.TYPE_INT_RGB);
                    Graphics2D tg = tiny.createGraphics();
                    tg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    tg.drawImage(ss, 0, 0, tinyW, tinyH, null);
                    tg.dispose();
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2.drawImage(tiny, 0, 0, getWidth(), getHeight(), null);
                    float fadeAlpha = Math.max(0f, fadeProgress * 2f - 1f);
                    if (fadeAlpha > 0f) {
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, fadeAlpha)));
                        g2.setColor(Color.BLACK);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    g2.dispose();
                }
            }

        };
        root.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                targetMX = (double) e.getX() / root.getWidth();
                targetMY = (double) e.getY() / root.getHeight();
            }
        });
        Timer tiltTimer = new Timer(16, _ -> {
            smoothMX += (targetMX - smoothMX) * 0.08;
            smoothMY += (targetMY - smoothMY) * 0.08;
            root.repaint();
        });
        tiltTimer.start();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                tiltTimer.stop();
            }
        });
        root.setOpaque(true);
        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.setBackground(new Color(15, 13, 11));
        setContentPane(root);
        GlowButton launchBtn = new GlowButton(
                "Launch",
                new Color(74, 72, 67),
                new Color(255, 255, 255),
                10,
                6,
                new Color(227, 191, 145),
                new Color(60, 60, 60),
                false
        );
        launchBtn.setBmpFont(ASCII_FONT);
        launchBtn.setBmpFontScale(2.5f);
        launchBtn.setPreferredSize(new Dimension(220, 64));
        launchBtn.setBackground(new Color(30, 30, 30));
        launchBtn.setForeground(Color.WHITE);
        launchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        launchBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { launchBtn.setBackground(new Color(94, 160, 104)); }
            @Override
            public void mouseExited(MouseEvent e) { launchBtn.setBackground(new Color(74, 130, 84)); }
        });
        launchBtn.addActionListener(() -> {
            launchBtn.setEnabled(false);
            int w = root.getWidth(), h = root.getHeight();
            ss = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D sg = ss.createGraphics();
            root.paint(sg);
            sg.dispose();
            fadeActive = true;
            int[] frame = {0};
            int totalFrames = 50;
            Timer t = new Timer(16, e -> {
                frame[0]++;
                fadeProgress = (float) frame[0] / totalFrames;
                root.repaint();
                if (frame[0] >= totalFrames) {
                    ((Timer) e.getSource()).stop();
                    latch.countDown();
                    dispose();
                }
            });
            t.start();
        });
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnRow.setBackground(new Color(15, 13, 11));
        btnRow.add(launchBtn);
        root.add(btnRow, BorderLayout.SOUTH);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(15, 13, 11));
        bar.setPreferredSize(new Dimension(0, 32));
        JComponent title = new JComponent() {
            private static final String TITLE = "Crypt of the Second Lord";

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int x = (getWidth() - BitmapFont.getWidth(TITLE))  / 2;
                int y = (getHeight() - BitmapFont.getHeight()) / 2;
                BitmapFont.drawString(g2, ASCII_FONT, TITLE, x, y, new Color(180, 165, 140));
                g2.dispose();
            }
        };
        bar.add(title, BorderLayout.CENTER);
        ImageIcon xNormal = tintCloseBtn(CLOSE_ICON_RAW, new Color(227, 191, 145));
        ImageIcon xHover = tintCloseBtn(CLOSE_ICON_RAW, new Color(220, 80, 60));
        JLabel closeBtn = new JLabel(xNormal);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { System.exit(0); }
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setIcon(xHover); }
            @Override public void mouseExited(MouseEvent e) { closeBtn.setIcon(xNormal); }
        });
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(40, 32));
        rightPanel.add(closeBtn);
        bar.add(rightPanel, BorderLayout.EAST);
        JPanel leftGhost = new JPanel();
        leftGhost.setOpaque(false);
        leftGhost.setPreferredSize(new Dimension(40, 32));
        bar.add(leftGhost, BorderLayout.WEST);
        MouseAdapter drag = new MouseAdapter() {
            private Point origin;
            @Override public void mousePressed(MouseEvent e)  { origin = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (origin == null) return;
                Point loc = getLocationOnScreen();
                setLocation(loc.x + e.getX() - origin.x, loc.y + e.getY() - origin.y);
            }
        };
        bar.addMouseListener(drag);
        bar.addMouseMotionListener(drag);
        return bar;
    }

    private static ImageIcon tintCloseBtn(BufferedImage src, Color color) {
        if (src == null) return new ImageIcon();
        BufferedImage out = new BufferedImage(12, 14, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, 12, 14, null);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(color);
        g.fillRect(0, 0, 12, 14);
        g.dispose();
        return new ImageIcon(out);
    }

    public static void create(CountDownLatch latch) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            LauncherWindow w = new LauncherWindow(latch);
            w.setVisible(true);
        });
    }
}
