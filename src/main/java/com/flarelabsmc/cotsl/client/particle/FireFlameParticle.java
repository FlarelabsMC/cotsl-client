package com.flarelabsmc.cotsl.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

public class FireFlameParticle extends RisingParticle {
    public FireFlameParticle(
            ClientLevel level,
            double x, double y, double z,
            double xd, double yd, double zd,
            TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, xd, yd, zd, sprite);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return Layer.TRANSLUCENT_TERRAIN;
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

    public static class FireFlameProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public FireFlameProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FlameParticle particle = new FlameParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
            particle.scale(1.5F);
            return particle;
        }
    }
}
