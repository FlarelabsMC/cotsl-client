package com.flarelabsmc.cotsl.client.particle;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;

public class FireSparkParticleGroup extends QuadParticleGroup {
    private final FireSparkParticle.FireSparkRenderState renderState = new FireSparkParticle.FireSparkRenderState();

    public FireSparkParticleGroup(ParticleEngine engine) {
        super(engine, FireSparkParticle.FIRE_SPARK_RENDER_TYPE);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        renderState.clear();
        for (SingleQuadParticle particle : this.particles) {
            if (frustum.pointInFrustum(particle.getPos().x, particle.getPos().y, particle.getPos().z)) {
                try {
                    particle.extract(renderState, camera, partialTickTime);
                } catch (Throwable t) {
                    CrashReport crash = CrashReport.forThrowable(t, "Rendering Fire Spark Particle");
                    CrashReportCategory category = crash.addCategory("Particle being rendered");
                    category.setDetail("Particle", particle::toString);
                    throw new ReportedException(crash);
                }
            }
        }
        return renderState;
    }
}
