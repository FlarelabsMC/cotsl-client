package com.flarelabsmc.cotsl.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

public class FireFlameParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public FireFlameParticle(
            ClientLevel level,
            double x, double y, double z,
            double xd, double yd, double zd,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.sprites = sprites;
        this.friction = 0.96F;
        this.xd = this.xd * (double) 0.01F + xd;
        this.yd = this.yd * (double) 0.01F + yd;
        this.zd = this.zd * (double) 0.01F + zd;
        this.x += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
        this.y += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
        this.z += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
        this.lifetime = (int) ((double) 8.0F / ((double) this.random.nextFloat() * 0.8 + 0.2)) + 4;
    }

    @Override
    public void tick() {
        this.setSpriteFromAge(this.sprites);
        super.tick();
    }

    @Override
    public void move(double xa, double ya, double za) {
        this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
        this.setLocationFromBoundingbox();
    }

    @Override
    public float getQuadSize(float a) {
        float s = ((float) this.age + a) / (float) this.lifetime;
        return this.quadSize * (1.0F - s * s * 0.5F);
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static class FireFlameProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FireFlameProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FireFlameParticle particle = new FireFlameParticle(level, x, y, z, xAux, yAux, zAux, this.sprites);
            particle.scale(1.5F);
            return particle;
        }
    }
}
