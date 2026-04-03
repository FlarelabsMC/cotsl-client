package com.flarelabsmc.cotsl.client.particle;

import com.flarelabsmc.cotsl.client.particle.options.FireSparkParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;

public class FireSparkParticle extends SingleQuadParticle {
    public static final ParticleRenderType FIRE_SPARK_RENDER_TYPE = new ParticleRenderType("cotsl:fire_spark");
    private static final float LOD_DIVISOR = 500.0f;
    private static final int TRAIL_LENGTH = 12;
    private static final int fromR = 1, fromG = 1, fromB = 1, toR = 1, toG = 173 / 255, toB = 44 / 255;

    private float bounce;
    private boolean virgin = true;
    private final TextureAtlasSprite sprite;
    private float prevSpeed, currentSpeed;
    private float brightness = 3.0f;

    private final float[] trailX = new float[TRAIL_LENGTH], trailY  = new float[TRAIL_LENGTH], trailZ = new float[TRAIL_LENGTH];
    private final float[] trailDX = new float[TRAIL_LENGTH], trailDY = new float[TRAIL_LENGTH], trailDZ = new float[TRAIL_LENGTH];
    private int trailFill;

    private final Vector3f prevDir = new Vector3f();
    private final Vector3f headFromDir = new Vector3f();
    private final Vector3f sCamRight = new Vector3f(), sCamUp = new Vector3f();
    private final Vector3f sCurrDir = new Vector3f(), sDFrom = new Vector3f(), sDTo = new Vector3f();
    private final Quaternionf sQPrev = new Quaternionf(), sQCurr = new Quaternionf(), sQHead = new Quaternionf();

    public FireSparkParticle(
            ClientLevel level, double x, double y, double z,
            double xa, double ya, double za,
            FireSparkParticleOptions options, TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, xa, ya, za, sprite);
        this.sprite = sprite;
        this.lifetime = options.lifetime();
        this.gravity = 0.9F;
        this.friction = .99f;
        this.quadSize = 300f;
        this.scale(0.001f);
        this.bounce = .6f + this.random.nextFloat() * .2f;
        float initSpd = (float) Math.sqrt(xa * xa + ya * ya + za * za);
        if (initSpd > 0.0001f) prevDir.set((float) (xa / initSpd), (float) (ya / initSpd), (float) (za / initSpd));
        headFromDir.set(prevDir);
        prevSpeed = initSpd; currentSpeed = initSpd;
        for (int i = 0; i < TRAIL_LENGTH; i++) {
            trailX[i] = (float) x;
            trailY[i] = (float) y;
            trailZ[i] = (float) z;
            trailDX[i] = prevDir.x;
            trailDY[i] = prevDir.y;
            trailDZ[i] = prevDir.z;
        }
        trailFill = TRAIL_LENGTH;
    }

    @Override
    public void tick() {
        float spd = speed();
        prevSpeed = spd;
        if (spd > 0.0001f) {
            headFromDir.set(prevDir);
            prevDir.set((float) (xd / spd), (float) (yd / spd), (float) (zd / spd));
        }

        int limit = Math.min(trailFill, TRAIL_LENGTH - 1);
        for (int i = limit; i > 0; i--) {
            trailX[i] = trailX[i-1];
            trailY[i] = trailY[i-1];
            trailZ[i] = trailZ[i-1];
            trailDX[i] = trailDX[i-1];
            trailDY[i] = trailDY[i-1];
            trailDZ[i] = trailDZ[i-1];
        }
        trailX[0] = (float) x;
        trailY[0] = (float) y;
        trailZ[0] = (float) z;
        trailDX[0] = prevDir.x;
        trailDY[0] = prevDir.y;
        trailDZ[0] = prevDir.z;
        trailFill = Math.min(trailFill + 1, TRAIL_LENGTH);

        if (virgin && lifetime < 30) lifetime++;
        if (this.onGround) {
            virgin = false;
            this.yd *= -bounce;
            bounce *= .8f;
        }

        super.tick();
        brightness = Mth.clamp(brightness * (1.5f - ((float) age / lifetime) * 2f), 0.0f, 1.5f);
        currentSpeed = speed();
        this.alpha = Mth.clamp(brightness, 0.0f, 1.0f);
        if (this.quadSize < 0.0001f || this.alpha < 0.01f) this.remove();
    }

    private float speed() {
        return Mth.sqrt((float)(xd * xd + yd * yd + zd * zd));
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTickTime) {
        if (!(state instanceof FireSparkRenderState s)) { super.extract(state, camera, partialTickTime); return; }
        if (alpha <= 0.004f || quadSize < 0.0001f || trailFill == 0) return;

        Vec3 camPos = camera.position();
        sCamRight.set(1, 0, 0).rotate(camera.rotation());
        sCamUp.set(0, 1, 0).rotate(camera.rotation());
        sCurrDir.set((float) (x - xo), (float) (y - yo), (float) (z - zo));
        if (sCurrDir.length() < 0.0001f) return;
        sCurrDir.normalize();

        // i neeeeed thiiiiis
        float baseHalf = quadSize * (1.0f + Mth.lerp(partialTickTime, prevSpeed, currentSpeed)) * 0.5f;
        int light = getLightCoords(partialTickTime);
        float sv0 = sprite.getV0(), sv1 = sprite.getV1(), dv = sv0 - sv1;
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float brightOver = Math.max(0f, 1 - ((float) age / lifetime));
        int rI = (int)(Mth.lerp(brightOver, toR, fromR) * 255) << 16;
        int gI = (int)(Mth.lerp(brightOver, toG, fromG) * 255) << 8;
        int bI = (int)(Mth.lerp(brightOver, toB, fromB) * 255);

        int n = trailFill + 1;
        float invNm1 = 1.0f / Math.max(n - 1, 1);

        float hx = (float) (Mth.lerp(partialTickTime, xo, x) - camPos.x());
        float hy = (float) (Mth.lerp(partialTickTime, yo, y) - camPos.y());
        float hz = (float) (Mth.lerp(partialTickTime, zo, z) - camPos.z());

        int startI = 0;
        if (baseHalf > 0.0f) {
            float ratio = Mth.sqrt(hx*hx + hy*hy + hz*hz) / (LOD_DIVISOR * baseHalf);
            if (ratio > 0.4f) startI = Math.min((int) Math.ceil((ratio - 0.4f) / (invNm1 * 0.6f)), trailFill - 1);
        }

        for (int i = startI; i < n; i++) {
            int ti = trailFill - 1 - i;
            boolean isHead = ti < 0, isAnchor = i == startI, isBreak;
            float cx, cy, cz;

            if (isHead) {
                cx = hx; cy = hy; cz = hz;
                sDFrom.set(headFromDir);
                if (sDFrom.length() < 0.0001f || sDFrom.dot(sCurrDir) < 0.0f) {
                    rotFromDir(sCurrDir, sCamRight, sCamUp, camera, sQHead);
                    isBreak = true;
                } else {
                    rotFromDir(sDFrom, sCamRight, sCamUp, camera, sQPrev);
                    rotFromDir(sCurrDir, sCamRight, sCamUp, camera, sQCurr);
                    sQHead.set(sQPrev).nlerp(sQCurr, partialTickTime);
                    isBreak = false;
                }
            } else if (isAnchor) {
                cx = (float) (trailX[ti] - camPos.x());
                cy = (float) (trailY[ti] - camPos.y());
                cz = (float) (trailZ[ti] - camPos.z());
                sDTo.set(trailDX[ti], trailDY[ti], trailDZ[ti]);
                if (sDTo.length() > 0.0001f) rotFromDir(sDTo.normalize(), sCamRight, sCamUp, camera, sQPrev);
                else sQPrev.set(camera.rotation());
                sQHead.set(sQPrev);
                isBreak = true;
            } else {
                cx = (float) (Mth.lerp(partialTickTime, trailX[ti+1], trailX[ti]) - camPos.x());
                cy = (float) (Mth.lerp(partialTickTime, trailY[ti+1], trailY[ti]) - camPos.y());
                cz = (float) (Mth.lerp(partialTickTime, trailZ[ti+1], trailZ[ti]) - camPos.z());
                sDFrom.set(trailDX[ti+1], trailDY[ti+1], trailDZ[ti+1]);
                sDTo.set(trailDX[ti], trailDY[ti], trailDZ[ti]);
                if (sDFrom.length() > 0.0001f && sDTo.length() > 0.0001f) {
                    sDFrom.normalize(); sDTo.normalize();
                    rotFromDir(sDTo, sCamRight, sCamUp, camera, sQCurr);
                    if (sDFrom.dot(sDTo) < 0.0f) { sQHead.set(sQCurr); isBreak = true; }
                    else { sQHead.set(sQPrev).nlerp(sQCurr, partialTickTime); isBreak = false; }
                    sQPrev.set(sQCurr);
                } else {
                    sQPrev.set(camera.rotation()); sQHead.set(sQPrev); isBreak = false;
                }
            }

            float t = i * invNm1;
            float a = alpha * t;
            int color = ((int) (a * 255) << 24) | rI | gI | bI;
            s.addRibbonPoint(
                    getLayer(),
                    cx, cy, cz,
                    sQHead.x, sQHead.y, sQHead.z, sQHead.w,
                    baseHalf * (0.4f + t * 0.6f), sv1 + t * dv,
                    u0, u1, sv0, sv1,
                    color,
                    light, isBreak
            );
        }
    }

    private static void rotFromDir(Vector3f dir, Vector3f camRight, Vector3f camUp, Camera camera, Quaternionf out) {
        out.set(camera.rotation()).rotateZ((float) Math.atan2(-dir.dot(camRight), dir.dot(camUp)));
    }

    @Override
    public ParticleRenderType getGroup() {
        return FIRE_SPARK_RENDER_TYPE;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static class FireSparkProvider implements ParticleProvider<FireSparkParticleOptions> {
        private final SpriteSet sprites;
        public FireSparkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                FireSparkParticleOptions options, ClientLevel level,
                double x, double y, double z, double xa, double ya, double za, RandomSource random
        ) {
            return new FireSparkParticle(level, x, y, z, xa, ya, za, options, sprites.get(random));
        }
    }

    public static class FireSparkRenderState extends QuadParticleRenderState {
        private float[] vCoords = new float[2048];
        private int[] storedColors = new int[2048];
        private boolean[] starts = new boolean[2048];
        private int addIdx = 0, renderIdx = 0;
        private float prevLX, prevLY, prevLZ, prevRX, prevRY, prevRZ, prevV;
        private int prevColor;
        private final Quaternionf sQuat = new Quaternionf();
        private final Vector3f sRight = new Vector3f();

        public void addRibbonPoint(
                SingleQuadParticle.Layer layer,
                float x, float y, float z, float xRot, float yRot, float zRot, float wRot,
                float halfWidth, float v, float u0, float u1, float v0, float v1,
                int color, int lightCoords, boolean isStart
        ) {
            if (addIdx >= vCoords.length) {
                vCoords = Arrays.copyOf(vCoords,vCoords.length * 2);
                storedColors = Arrays.copyOf(storedColors, storedColors.length * 2);
                starts = Arrays.copyOf(starts,starts.length * 2);
            }
            vCoords[addIdx] = v; storedColors[addIdx] = color; starts[addIdx] = isStart;
            addIdx++;
            super.add(layer, x, y, z, xRot, yRot, zRot, wRot, halfWidth, u0, u1, v0, v1, color, lightCoords);
        }

        @Override
        public void clear() {
            super.clear();
            addIdx = renderIdx = 0;
        }

        @Override
        protected void renderRotatedQuad(
                VertexConsumer builder,
                float x, float y, float z, float xRot, float yRot, float zRot, float wRot,
                float width, float u0, float u1, float v0, float v1, int color, int lightCoords
        ) {
            float v = vCoords[renderIdx];
            int storedColor = storedColors[renderIdx];
            boolean isStart = starts[renderIdx++];

            sQuat.set(xRot, yRot, zRot, wRot);
            sRight.set(width, 0, 0).rotate(sQuat);
            float lx = x - sRight.x, ly = y - sRight.y, lz = z - sRight.z;
            float rx = x + sRight.x, ry = y + sRight.y, rz = z + sRight.z;

            if (isStart) {
                builder.addVertex(lx, ly, lz).setUv(u0, v).setColor(0).setLight(lightCoords);
                builder.addVertex(lx, ly, lz).setUv(u0, v).setColor(0).setLight(lightCoords);
                builder.addVertex(lx, ly, lz).setUv(u0, v).setColor(0).setLight(lightCoords);
                builder.addVertex(lx, ly, lz).setUv(u0, v).setColor(0).setLight(lightCoords);
            } else {
                builder.addVertex(prevRX, prevRY, prevRZ).setUv(u1, prevV).setColor(prevColor).setLight(lightCoords);
                builder.addVertex(rx, ry, rz).setUv(u1, v).setColor(storedColor).setLight(lightCoords);
                builder.addVertex(lx, ly, lz).setUv(u0, v).setColor(storedColor).setLight(lightCoords);
                builder.addVertex(prevLX, prevLY, prevLZ).setUv(u0, prevV).setColor(prevColor).setLight(lightCoords);
            }

            prevLX = lx; prevLY = ly; prevLZ = lz;
            prevRX = rx; prevRY = ry; prevRZ = rz;
            prevV = v; prevColor = storedColor;
        }
    }
}