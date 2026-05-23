package com.flarelabsmc.cotsl.client.particle;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;

public class SparkParticleGroup extends QuadParticleGroup {
    private final SparkParticle.RenderState renderState = new SparkParticle.RenderState();

    public SparkParticleGroup(ParticleEngine engine) {
        super(engine, SparkParticle.RENDER_TYPE);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        renderState.clear();
        for (SingleQuadParticle particle : this.particles) {
            if (frustum.pointInFrustum(particle.getPos().x, particle.getPos().y, particle.getPos().z)) {
                try {
                    particle.extract(renderState, camera, partialTickTime);
                } catch (Throwable t) {
                    CrashReport crash = CrashReport.forThrowable(t, "Rendering particle");
                    CrashReportCategory category = crash.addCategory("Particle being rendered");
                    category.setDetail("Particle", particle::toString);
                    throw new ReportedException(crash);
                }
            }
        }
        return renderState;
    }
}
