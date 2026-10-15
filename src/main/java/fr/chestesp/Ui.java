package fr.chestesp;

import net.minecraft.client.gui.DrawContext;

/** Couleurs et petits outils de dessin pour l'interface. */
public final class Ui {
    public static final int DIM = 0x99000000;
    public static final int BORDER = 0xFF3B2A5E;
    public static final int BG = 0xF2100C18;
    public static final int SIDE = 0xFF0D0A14;
    public static final int ROW = 0xFF1B1626;
    public static final int ROW_HOVER = 0xFF261F36;
    public static final int ACCENT = 0xFFA855F7;
    public static final int ACCENT2 = 0xFFC084FC;
    public static final int CYAN = 0xFF22D3EE;
    public static final int TEXT = 0xFFE9E4F5;
    public static final int MUTED = 0xFF8E88A6;
    public static final int GREEN = 0xFF4ADE80;
    public static final int RED = 0xFFF87171;
    public static final int OFF = 0xFF3B3550;

    private Ui() {}

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** 0..1 qui oscille doucement (effet de halo). */
    public static float pulse() {
        return (float) (Math.sin(System.currentTimeMillis() / 450.0) * 0.5 + 0.5);
    }

    public static void window(DrawContext c, int x, int y, int w, int h) {
        c.fill(x - 1, y - 1, x + w + 1, y + h + 1, lerp(BORDER, ACCENT, pulse() * 0.5f));
        c.fill(x, y, x + w, y + h, BG);
    }

    public static void rowBg(DrawContext c, int x, int y, int w, int h, boolean hover, boolean selected) {
        c.fill(x, y, x + w, y + h, hover ? ROW_HOVER : ROW);
        if (selected) c.drawBorder(x, y, w, h, lerp(ACCENT, ACCENT2, pulse()));
    }
}
