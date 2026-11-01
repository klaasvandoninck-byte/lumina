package com.lumina.render;

import com.lumina.module.ModuleManager;
import com.lumina.module.impl.PlayerESP;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/**
 * Box/Outline-ESP met een eigen render-pipeline zonder dieptetest, zodat het door blokken heen
 * zichtbaar is. Gebaseerd op het voorbeeld "Rendering in the World" uit de Fabric-docs voor 1.21.11.
 */
public final class EspRenderer {
	private static final RenderPipeline PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath("lumina", "pipeline/esp_filled_through_walls"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.build());

	private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

	private static BufferBuilder buffer;
	private static MappableRingBuffer vertexBuffer;

	private EspRenderer() {
	}

	public static void init() {
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(EspRenderer::onRender);
	}

	private static void onRender(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		PlayerESP esp = ModuleManager.esp();
		if (esp == null || !esp.isEnabled() || esp.mode.is("Glow") || mc.level == null || mc.player == null) {
			return;
		}

		PoseStack matrices = context.matrices();
		Vec3 camera = context.worldState().cameraRenderState.pos;
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

		int rgb = esp.color.rgb();
		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float b = (rgb & 0xFF) / 255f;
		float a = esp.alpha.getF();
		boolean outline = esp.mode.is("Outline");
		float t = esp.thickness.getF();
		double max = esp.range.get();

		matrices.pushPose();
		matrices.translate(-camera.x, -camera.y, -camera.z);
		Matrix4fc pose = matrices.last().pose();

		for (AbstractClientPlayer p : mc.level.players()) {
			if (p == mc.player || p.isSpectator() || mc.player.distanceTo(p) > max) {
				continue;
			}
			// Interpoleer tussen de vorige en huidige positie voor een vloeiende box.
			double dx = Mth.lerp(pt, p.xo, p.getX()) - p.getX();
			double dy = Mth.lerp(pt, p.yo, p.getY()) - p.getY();
			double dz = Mth.lerp(pt, p.zo, p.getZ()) - p.getZ();
			AABB box = p.getBoundingBox().move(dx, dy, dz);

			if (buffer == null) {
				buffer = new BufferBuilder(ALLOCATOR, PIPELINE.getVertexFormatMode(), PIPELINE.getVertexFormat());
			}
			if (outline) {
				addOutline(pose, buffer, box, t, r, g, b, Math.max(a, 0.6f));
			} else {
				addBox(pose, buffer, (float) box.minX, (float) box.minY, (float) box.minZ,
					(float) box.maxX, (float) box.maxY, (float) box.maxZ, r, g, b, a);
			}
		}

		matrices.popPose();

		if (buffer != null) {
			drawBuffer(mc);
		}
	}

	/** 12 dunne boxjes langs de randen van de box. */
	private static void addOutline(Matrix4fc m, BufferBuilder bb, AABB box, float t, float r, float g, float b, float a) {
		float x0 = (float) box.minX;
		float y0 = (float) box.minY;
		float z0 = (float) box.minZ;
		float x1 = (float) box.maxX;
		float y1 = (float) box.maxY;
		float z1 = (float) box.maxZ;

		for (float y : new float[] {y0, y1}) {
			for (float z : new float[] {z0, z1}) {
				addBox(m, bb, x0, y - t, z - t, x1, y + t, z + t, r, g, b, a);
			}
		}
		for (float x : new float[] {x0, x1}) {
			for (float z : new float[] {z0, z1}) {
				addBox(m, bb, x - t, y0, z - t, x + t, y1, z + t, r, g, b, a);
			}
		}
		for (float x : new float[] {x0, x1}) {
			for (float y : new float[] {y0, y1}) {
				addBox(m, bb, x - t, y - t, z0, x + t, y + t, z1, r, g, b, a);
			}
		}
	}

	private static void addBox(Matrix4fc m, BufferBuilder bb, float minX, float minY, float minZ,
							   float maxX, float maxY, float maxZ, float red, float green, float blue, float alpha) {
		// Front
		bb.addVertex(m, minX, minY, maxZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, maxX, minY, maxZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, minX, maxY, maxZ).setColor(red, green, blue, alpha);
		// Back
		bb.addVertex(m, maxX, minY, minZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, minX, minY, minZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, minX, maxY, minZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, maxX, maxY, minZ).setColor(red, green, blue, alpha);
		// Left
		bb.addVertex(m, minX, minY, minZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, minX, minY, maxZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, minX, maxY, maxZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, minX, maxY, minZ).setColor(red, green, blue, alpha);
		// Right
		bb.addVertex(m, maxX, minY, maxZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, maxX, minY, minZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, maxX, maxY, minZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
		// Top
		bb.addVertex(m, minX, maxY, maxZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, maxX, maxY, minZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, minX, maxY, minZ).setColor(red, green, blue, alpha);
		// Bottom
		bb.addVertex(m, minX, minY, minZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, maxX, minY, minZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, maxX, minY, maxZ).setColor(red, green, blue, alpha);
		bb.addVertex(m, minX, minY, maxZ).setColor(red, green, blue, alpha);
	}

	// ---- Drawing phase (zelfde opzet als het Fabric-docs voorbeeld) ----

	private static void drawBuffer(Minecraft client) {
		MeshData builtBuffer = buffer.buildOrThrow();
		MeshData.DrawState drawParameters = builtBuffer.drawState();
		VertexFormat format = drawParameters.format();

		GpuBuffer vertices = upload(drawParameters, format, builtBuffer);
		draw(client, builtBuffer, drawParameters, vertices, format);

		vertexBuffer.rotate();
		buffer = null;
	}

	private static GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
		int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

		if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
			if (vertexBuffer != null) {
				vertexBuffer.close();
			}
			vertexBuffer = new MappableRingBuffer(() -> "lumina esp vertices",
				GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
		}

		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
		try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
			vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
			MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
		}
		return vertexBuffer.currentBuffer();
	}

	private static void draw(Minecraft client, MeshData builtBuffer, MeshData.DrawState drawParameters,
							 GpuBuffer vertices, VertexFormat format) {
		GpuBuffer indices;
		VertexFormat.IndexType indexType;

		if (PIPELINE.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
			builtBuffer.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
			indices = PIPELINE.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
			indexType = builtBuffer.drawState().indexType();
		} else {
			RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(PIPELINE.getVertexFormatMode());
			indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
			indexType = shapeIndexBuffer.type();
		}

		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
			.writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
		try (RenderPass renderPass = RenderSystem.getDevice()
			.createCommandEncoder()
			.createRenderPass(() -> "lumina esp rendering",
				client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
				client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
			renderPass.setPipeline(PIPELINE);

			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", dynamicTransforms);

			renderPass.setVertexBuffer(0, vertices);
			renderPass.setIndexBuffer(indices, indexType);
			renderPass.drawIndexed(0, 0, drawParameters.indexCount(), 1);
		}

		builtBuffer.close();
	}

	public static void close() {
		ALLOCATOR.close();
		if (vertexBuffer != null) {
			vertexBuffer.close();
			vertexBuffer = null;
		}
	}
}
