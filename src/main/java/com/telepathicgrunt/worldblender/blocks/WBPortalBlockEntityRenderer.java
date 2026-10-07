package com.telepathicgrunt.worldblender.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

/**
 * Renders the World Blender portal using the vanilla end portal render type (animated starfield).
 * The 1.16.5 version used a custom multi-pass render type; this is a simpler port. See HISTORY.md.
 */
public class WBPortalBlockEntityRenderer implements BlockEntityRenderer<WBPortalBlockEntity>
{
	public WBPortalBlockEntityRenderer(BlockEntityRendererProvider.Context dispatcher) {
	}

	private static final float MIN = 0.0625F;
	private static final float MAX = 0.9375F;

	@Override
	public void render(WBPortalBlockEntity blockEntity, float partialTick, PoseStack poseStack,
					   MultiBufferSource bufferSource, int packedLight, int packedOverlay)
	{
		Matrix4f matrix = poseStack.last().pose();
		VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.endPortal());

		// south (+Z)
		face(vertexConsumer, matrix, MIN, MAX, MIN, MIN, MAX, MAX, MAX, MAX, MAX, MAX, MIN, MAX);
		// north (-Z)
		face(vertexConsumer, matrix, MAX, MAX, MIN, MAX, MAX, MAX, MIN, MAX, MAX, MIN, MAX, MIN);
		// east (+X)
		face(vertexConsumer, matrix, MAX, MAX, MAX, MAX, MAX, MIN, MAX, MIN, MIN, MAX, MIN, MAX);
		// west (-X)
		face(vertexConsumer, matrix, MIN, MAX, MIN, MIN, MAX, MAX, MIN, MIN, MAX, MIN, MIN, MIN);
		// up (+Y)
		face(vertexConsumer, matrix, MIN, MAX, MAX, MAX, MAX, MAX, MAX, MAX, MIN, MIN, MAX, MIN);
		// down (-Y)
		face(vertexConsumer, matrix, MIN, MIN, MIN, MAX, MIN, MIN, MAX, MIN, MAX, MIN, MIN, MAX);
	}

	private static void face(VertexConsumer vc, Matrix4f m,
							 float x1, float y1, float z1,
							 float x2, float y2, float z2,
							 float x3, float y3, float z3,
							 float x4, float y4, float z4)
	{
		vc.vertex(m, x1, y1, z1).endVertex();
		vc.vertex(m, x2, y2, z2).endVertex();
		vc.vertex(m, x3, y3, z3).endVertex();
		vc.vertex(m, x4, y4, z4).endVertex();
	}

	@Override
	public boolean shouldRenderOffScreen(WBPortalBlockEntity blockEntity) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}
}
