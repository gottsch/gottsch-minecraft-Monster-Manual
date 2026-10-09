package mod.gottsch.neo.gmm.core.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws a tapered, optionally arcing tube between two points — the shared geometry behind every
 * "limb that reaches much further than its body" in GMM: {@code BlackPudding}'s tendril lash, and
 * (planned) the Roper/Reaper grapple-and-reel tentacle.
 * <p>
 * This exists because {@code ModelPart} cannot do it. Stretching a cuboid via {@code zScale} gets you
 * length, but only in a straight line and only by smearing the texture along it; a tentacle that
 * <em>arcs</em> would need a long chain of hand-posed bones to fake badly. Generating the vertices
 * instead makes length, taper and curvature all continuous parameters, at the cost of the geometry no
 * longer being editable in Blockbench — which is the right trade for a limb whose whole shape is
 * computed at runtime anyway.
 * <p>
 * Deliberately <em>not</em> the beacon-beam render type. That one is full-bright and translucent,
 * which suits magic and suits nothing here: these are wet physical limbs that must be lit by the same
 * lightmap as the body they grow out of. Callers pass the {@code VertexConsumer} they were already
 * handed, so the tube shares its parent's texture, render type, lighting and overlay — a hurt flash
 * reddens the tentacle along with the mob, for free.
 * <p>
 * Endpoints are taken per call rather than baked, so a caller may either fire and forget (a strike
 * that snaps out and retracts) or track a live, moving target every frame (a grapple that reels its
 * victim in).
 * <p>
 * <b>Coordinates are in model units</b> — the same 1/16-of-a-block units {@code ModelPart} takes, with
 * Y pointing down — so a caller can position this against its own cuboids without converting. The
 * division into block space happens in {@link #vertex}, exactly as {@code ModelPart.Cube.compile}
 * does it. Omitting that conversion is silent and total: the geometry still builds correctly and
 * throws nothing, but lands sixteen times too far out and sixteen times too large, which in practice
 * puts it underground and off-screen. That mistake cost a full debugging session on 2026-08-24.
 *
 * @author Mark Gottschling on 8/24/2026
 */
public final class TendrilRenderer {

    /** Model units per block — see the class note on why every position must be divided by this. */
    private static final float UNITS_PER_BLOCK = 16.0F;

    private TendrilRenderer() {
    }

    /**
     * @param from         origin, in model units
     * @param to           endpoint, in model units
     * @param sag          perpendicular bow, in model units. 0 draws straight; larger values bow the
     *                     curve out to the side, which reads as slack in a tether and as a whip-crack
     *                     in a strike when animated toward 0 at full extension
     * @param startRadius  half-thickness at {@code from}, in model units
     * @param endRadius    half-thickness at {@code to} — taper is just these two differing
     * @param segments     subdivisions along the length; more = smoother arc. 1 collapses to straight
     * @param sides        subdivisions around the tube. 4 reads as a blocky prism (fits Minecraft),
     *                     8 as a smooth tube
     * @param u0 v0 u1 v1  normalised UV rect to map the tube into, u wrapping around, v along the length
     */
    public static void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                              float red, float green, float blue, float alpha,
                              Vector3f from, Vector3f to, float sag,
                              float startRadius, float endRadius,
                              int segments, int sides,
                              float u0, float v0, float u1, float v1) {
        segments = Math.max(1, segments);
        sides = Math.max(3, sides);

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // quadratic bezier control point: the midpoint pushed out along the tube's own perpendicular,
        // so the bow follows the limb's orientation rather than always bending in world-space Y
        Vector3f axis = new Vector3f(to).sub(from);
        Vector3f control = new Vector3f(from).add(to).mul(0.5F);
        if (sag != 0.0F) {
            control.add(perpendicular(axis).mul(sag));
        }

        float[] ring = new float[(sides + 1) * 6];
        float[] next = new float[(sides + 1) * 6];

        buildRing(ring, from, control, to, 0.0F, startRadius, sides);
        for (int i = 0; i < segments; i++) {
            float t1 = (float) (i + 1) / segments;
            buildRing(next, from, control, to, t1, Mth.lerp(t1, startRadius, endRadius), sides);

            float va = v0 + (v1 - v0) * ((float) i / segments);
            float vb = v0 + (v1 - v0) * t1;

            for (int j = 0; j < sides; j++) {
                float ua = u0 + (u1 - u0) * ((float) j / sides);
                float ub = u0 + (u1 - u0) * ((float) (j + 1) / sides);
                int a = j * 6;
                int b = (j + 1) * 6;

                // entityCutoutNoCull, so winding does not affect visibility — but the normals still
                // drive shading, and must match the ring's outward radial direction
                vertex(consumer, pose, normal, ring, a, ua, va, packedLight, packedOverlay, red, green, blue, alpha);
                vertex(consumer, pose, normal, next, a, ua, vb, packedLight, packedOverlay, red, green, blue, alpha);
                vertex(consumer, pose, normal, next, b, ub, vb, packedLight, packedOverlay, red, green, blue, alpha);
                vertex(consumer, pose, normal, ring, b, ub, va, packedLight, packedOverlay, red, green, blue, alpha);
            }
            System.arraycopy(next, 0, ring, 0, ring.length);
        }
    }

    /** Fills {@code out} with {@code sides + 1} (position, normal) pairs around the curve at {@code t}. */
    private static void buildRing(float[] out, Vector3f from, Vector3f control, Vector3f to,
                                  float t, float radius, int sides) {
        Vector3f centre = bezier(from, control, to, t);
        Vector3f tangent = bezierTangent(from, control, to, t);
        if (tangent.lengthSquared() < 1.0E-6F) {
            tangent.set(0.0F, 0.0F, -1.0F);
        }
        tangent.normalize();

        Vector3f side = perpendicular(tangent);
        Vector3f up = new Vector3f(side).cross(tangent).normalize();

        for (int j = 0; j <= sides; j++) {
            float angle = (float) (Math.PI * 2.0 * j / sides);
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            float nx = side.x * cos + up.x * sin;
            float ny = side.y * cos + up.y * sin;
            float nz = side.z * cos + up.z * sin;
            int o = j * 6;
            out[o] = centre.x + nx * radius;
            out[o + 1] = centre.y + ny * radius;
            out[o + 2] = centre.z + nz * radius;
            out[o + 3] = nx;
            out[o + 4] = ny;
            out[o + 5] = nz;
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float[] ring, int o,
                               float u, float v, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        // Positions are model units and the matrix is block space, so divide — exactly as
        // ModelPart.Cube.compile does. Normals are directions rather than positions, so they are
        // deliberately left unscaled.
        consumer.vertex(pose,
                        ring[o] / UNITS_PER_BLOCK,
                        ring[o + 1] / UNITS_PER_BLOCK,
                        ring[o + 2] / UNITS_PER_BLOCK)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normal, ring[o + 3], ring[o + 4], ring[o + 5])
                .endVertex();
    }

    private static Vector3f bezier(Vector3f a, Vector3f c, Vector3f b, float t) {
        float inv = 1.0F - t;
        return new Vector3f(
                inv * inv * a.x + 2.0F * inv * t * c.x + t * t * b.x,
                inv * inv * a.y + 2.0F * inv * t * c.y + t * t * b.y,
                inv * inv * a.z + 2.0F * inv * t * c.z + t * t * b.z);
    }

    private static Vector3f bezierTangent(Vector3f a, Vector3f c, Vector3f b, float t) {
        float inv = 1.0F - t;
        return new Vector3f(
                2.0F * inv * (c.x - a.x) + 2.0F * t * (b.x - c.x),
                2.0F * inv * (c.y - a.y) + 2.0F * t * (b.y - c.y),
                2.0F * inv * (c.z - a.z) + 2.0F * t * (b.z - c.z));
    }

    /** Any unit vector perpendicular to {@code v}, chosen to stay well-conditioned near the poles. */
    private static Vector3f perpendicular(Vector3f v) {
        Vector3f reference = Math.abs(v.y()) < 0.99F * v.length()
                ? new Vector3f(0.0F, 1.0F, 0.0F)
                : new Vector3f(1.0F, 0.0F, 0.0F);
        Vector3f result = new Vector3f(v).cross(reference);
        if (result.lengthSquared() < 1.0E-6F) {
            result.set(1.0F, 0.0F, 0.0F);
        }
        return result.normalize();
    }
}
