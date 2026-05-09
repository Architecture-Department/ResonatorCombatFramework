package architecture.resonator_combat_framework.client.renderer

import cn.solarmoon.spark_core.animation.IAnimatable
import cn.solarmoon.spark_core.animation.model.ModelInstance
import cn.solarmoon.spark_core.animation.model.ModelPose
import cn.solarmoon.spark_core.animation.model.origin.OBone
import cn.solarmoon.spark_core.animation.model.origin.OCube
import cn.solarmoon.spark_core.animation.model.origin.OMesh
import cn.solarmoon.spark_core.animation.model.origin.OModel
import cn.solarmoon.spark_core.animation.renderer.IGeoRenderer
import cn.solarmoon.spark_core.animation.renderer.layer.RenderLayer
import cn.solarmoon.spark_core.animation.renderer.tmpM3
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType

/**
 * 渲染器扩展接口
 * 提供完整的模型渲染流程和自定义钩子
 */
interface ISparkGeoRendererExpand<T, S> : IGeoRenderer<T, S> where S : IAnimatable<T> {
	// TODO 未满足要求
	override val layers: MutableList<RenderLayer<T, S>>

	/** 主渲染入口，协调整个渲染流程*/
	override fun render(
		animatable: S,
		partialTick: Float,
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int
	) {
		poseStack.pushPose()
		val model = getModel(animatable)
		val renderType = getRenderType(animatable)
		val buffer = getBuffer(bufferSource, renderType)
		val packedOverlay = getOverlay(animatable, partialTick)
		val renderColor = getColor(animatable, partialTick)
		preRender(
			poseStack,
			animatable,
			model,
			bufferSource,
			buffer,
			false,
			partialTick,
			packedLight,
			packedOverlay,
			renderColor
		)
		if (firePreRenderEvent(
				poseStack,
				model,
				bufferSource,
				partialTick,
				packedLight
			)
		) {
			preApplyRenderLayers(
				poseStack,
				animatable,
				model,
				renderType,
				bufferSource,
				buffer,
				partialTick,
				packedLight,
				packedOverlay
			)
			actuallyRender(
				poseStack,
				animatable,
				model,
				renderType,
				bufferSource,
				buffer,
				false,
				partialTick,
				packedLight,
				packedOverlay,
				renderColor
			)
			applyRenderLayers(
				poseStack,
				animatable,
				model,
				renderType,
				bufferSource,
				buffer,
				partialTick,
				packedLight,
				packedOverlay,
				renderColor
			)
			postRender(
				poseStack,
				animatable,
				model,
				bufferSource,
				buffer,
				false,
				partialTick,
				packedLight,
				packedOverlay,
				renderColor
			)
			firePostRenderEvent(
				poseStack,
				model,
				bufferSource,
				partialTick,
				packedLight
			)
		}
		poseStack.popPose()
		renderFinal(
			poseStack,
			animatable,
			model,
			bufferSource,
			buffer,
			partialTick,
			packedLight,
			packedOverlay,
			renderColor
		)
		doPostRenderCleanup()
	}

	/** 重新渲染指定模型，用于渲染层或子模型 */
	fun reRender(
		model: ModelInstance?,
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		animatable: S,
		renderType: RenderType?,
		buffer: VertexConsumer?,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int,
		colour: Int
	) {
		poseStack.pushPose()
		preRender(
			poseStack, animatable, model, bufferSource, buffer!!,
			true, partialTick, packedLight, packedOverlay, colour
		)
		actuallyRender(
			poseStack, animatable, model, renderType, bufferSource,
			buffer, true, partialTick, packedLight, packedOverlay, colour
		)
		postRender(
			poseStack, animatable, model, bufferSource, buffer,
			true, partialTick, packedLight, packedOverlay, colour
		)
		poseStack.popPose()
	}

	/** 获取顶点缓冲区 */
	fun getBuffer(bufferSource: MultiBufferSource, renderType: RenderType): VertexConsumer =
		bufferSource.getBuffer(renderType)

	/** 获取模型实例 */
	fun getModel(animatable: S): ModelInstance? = animatable.modelController.model

	/** 渲染后事件钩子 */
	fun firePostRenderEvent(
		poseStack: PoseStack,
		model: ModelInstance?,
		bufferSource: MultiBufferSource,
		partialTick: Float,
		packedLight: Int
	) {
	}

	/** 渲染后处理，可添加额外渲染逻辑 */
	fun postRender(
		poseStack: PoseStack,
		animatable: S,
		model: ModelInstance?,
		bufferSource: MultiBufferSource,
		buffer: VertexConsumer,
		isReRender: Boolean,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int,
		renderColor: Int
	) {
	}

	/** 渲染前准备，处理矩阵变换等 */
	fun preRender(
		poseStack: PoseStack,
		animatable: S,
		model: ModelInstance?,
		bufferSource: MultiBufferSource,
		buffer: VertexConsumer,
		isReRender: Boolean,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int,
		colour: Int
	) {
	}

	/** 应用所有渲染层 */
	fun applyRenderLayers(
		poseStack: PoseStack,
		animatable: S,
		model: ModelInstance?,
		renderType: RenderType?,
		bufferSource: MultiBufferSource,
		buffer: VertexConsumer?,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int,
		colour: Int
	) {
		layers.forEach {
			it.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay)
		}
	}


	/** 执行实际渲染，遍历模型骨骼 */
	fun actuallyRender(
		poseStack: PoseStack,
		animatable: S,
		model: ModelInstance?,
		renderType: RenderType?,
		bufferSource: MultiBufferSource,
		buffer: VertexConsumer?,
		isReRender: Boolean,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int,
		colour: Int
	) {
		var buffer = buffer
		if (buffer == null) {
			if (renderType == null) return
			buffer = bufferSource.getBuffer(renderType)
		}
		poseStack.pushPose()
		poseStack.mulPose(animatable.getWorldPositionMatrix(partialTick))

		updateAnimatedTextureFrame(animatable)

		model?.apply {
			renderModel(origin, pose, poseStack, buffer, packedLight, packedOverlay, colour, partialTick)
		}
		poseStack.popPose()
	}

	/** 更新动画纹理帧 */
	fun updateAnimatedTextureFrame(animatable: S) {
	}

	/** 渲染前应用渲染层 */
	fun preApplyRenderLayers(
		poseStack: PoseStack,
		animatable: S,
		model: ModelInstance?,
		renderType: RenderType?,
		bufferSource: MultiBufferSource,
		buffer: VertexConsumer?,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int
	) {
	}

	/** 渲染前事件钩子，返回false可取消渲染 */
	fun firePreRenderEvent(
		poseStack: PoseStack,
		model: ModelInstance?,
		bufferSource: MultiBufferSource,
		partialTick: Float,
		packedLight: Int
	): Boolean

	/** 最终渲染处理，在popPose后执行 */
	fun renderFinal(
		poseStack: PoseStack,
		animatable: S,
		model: ModelInstance?,
		bufferSource: MultiBufferSource,
		buffer: VertexConsumer,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int,
		renderColor: Int
	) {
	}

	/** 递归渲染模型所有骨骼 */
	fun renderModel(
		model: OModel,
		modelPose: ModelPose,
		poseStack: PoseStack,
		buffer: VertexConsumer,
		packedLight: Int,
		packedOverlay: Int,
		color: Int,
		partialTick: Float,
		force: Boolean = false
	) {
		model.bones.forEach { (name, bone) ->
			renderBones(name, bone, modelPose, poseStack, buffer, packedLight, packedOverlay, color, partialTick)
		}
	}

	/** 渲染单个骨骼及其子元素 */
	fun renderBones(
		name: String,
		bone: OBone,
		pose: ModelPose,
		poseStack: PoseStack,
		buffer: VertexConsumer,
		packedLight: Int,
		packedOverlay: Int,
		color: Int,
		partialTick: Float,
		force: Boolean = false
	) {
		bone.tmpM4.identity()
		tmpM3.identity()
		bone.applyTransformWithParents(pose, bone.tmpM4, partialTick)
		poseStack.pushPose()
		poseStack.mulPose(bone.tmpM4)

		// 渲染立方体
		for (cube in bone.cubes) {
			renderCubes(cube, poseStack, buffer, packedLight, packedOverlay, color, force)
		}
		// 渲染网格
		renderMesh(poseStack, bone.mesh, bone, buffer, packedLight, packedOverlay, color, partialTick, force)

		poseStack.popPose()
	}

	/** 渲染骨骼下的立方体 */
	fun renderCubes(
		cube: OCube,
		poseStack: PoseStack,
		buffer: VertexConsumer,
		packedLight: Int,
		packedOverlay: Int,
		color: Int,
		force: Boolean
	) {
		poseStack.pushPose()
		cube.renderVertexes(poseStack, buffer, packedLight, packedOverlay, color, force)
		poseStack.popPose()
	}

	/** 渲染骨骼下的网格 */
	fun renderMesh(
		poseStack: PoseStack,
		mesh: OMesh?,
		bone: OBone,
		buffer: VertexConsumer,
		packedLight: Int,
		packedOverlay: Int,
		color: Int,
		partialTick: Float,
		force: Boolean
	) {
		poseStack.pushPose()
		mesh?.renderVertexes(bone.tmpM4, tmpM3, buffer, packedLight, packedOverlay, color, partialTick, force)
		poseStack.popPose()
	}

	/** 渲染完成后的清理工作 */
	fun doPostRenderCleanup() {
	}

	/** 模型缩放处理，重渲染时跳过 */
	fun scaleModelForRender(
		widthScale: Float,
		heightScale: Float,
		poseStack: PoseStack,
		animatable: S?,
		model: ModelInstance?,
		isReRender: Boolean,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int
	) {
		if (isReRender || !(widthScale != 1f || heightScale != 1f)) return
		poseStack.scale(widthScale, heightScale, widthScale)
	}
}
