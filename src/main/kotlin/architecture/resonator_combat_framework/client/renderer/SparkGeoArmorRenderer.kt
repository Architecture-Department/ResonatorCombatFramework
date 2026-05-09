package architecture.resonator_combat_framework.client.renderer

import cn.solarmoon.spark_core.animation.IAnimatable
import cn.solarmoon.spark_core.animation.model.BonePose
import cn.solarmoon.spark_core.animation.model.ModelInstance
import cn.solarmoon.spark_core.animation.model.ModelPose
import cn.solarmoon.spark_core.animation.model.origin.OBone
import cn.solarmoon.spark_core.animation.model.origin.OModel
import cn.solarmoon.spark_core.animation.renderer.layer.RenderLayer
import cn.solarmoon.spark_core.animation.renderer.tmpM3
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.PlayerSkin
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f

open class SparkGeoArmorRenderer<T, S> : ISparkGeoRendererExpand<T, S> where S : IAnimatable<T> {
	val boneInfoExpand: MutableMap<String, OBoneInfo> = mutableMapOf()
	override val layers: MutableList<RenderLayer<T, S>> = mutableListOf()

	var animatable: S? = null

	var lastModelInstance: ModelInstance? = null

	var bufferSource: MultiBufferSource? = null
	var partialTick: Float = 0f

	var limbSwing: Float = 0f
	var limbSwingAmount: Float = 0f

	var scaleWidth: Float = 1f
	var scaleHeight: Float = 1f

	var baseHumanoidModel: HumanoidModel<*>? = null
	var baseLastModelInstance: ModelInstance? = null

	var currentEntity: Entity? = null
	var currentStack: ItemStack? = null
	var currentSlot: EquipmentSlot? = null

	var netHeadYaw: Float = 0f
	var headPitch: Float = 0f

	var attackTime: Float = 0f
	var riding: Boolean = false
	var young: Boolean = false

	var isFineArm: Boolean = false

	var bipedHead: OBone? = null
	var armorHead: OBone? = null

	var bipedBody: OBone? = null
	var armorPants: OBone? = null
	var armorBody: OBone? = null

	var bipedRightArm: OBone? = null
	var armorRightArm: OBone? = null
	var armorFineRightArm: OBone? = null

	var bipedLeftArm: OBone? = null
	var armorLeftArm: OBone? = null
	var armorFineLeftArm: OBone? = null

	var bipedRightLeg: OBone? = null
	var armorRightLeg: OBone? = null
	var armorRightBoot: OBone? = null

	var bipedLeftLeg: OBone? = null
	var armorLeftLeg: OBone? = null
	var armorLeftBoot: OBone? = null

	override fun actuallyRender(
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
		poseStack.pushPose()
		poseStack.translate(0f, 24 / 16f, 0f)
		poseStack.scale(-1f, -1f, 1f)
		super.actuallyRender(
			poseStack,
			animatable,
			model,
			renderType,
			bufferSource,
			buffer,
			isReRender,
			partialTick,
			packedLight,
			packedOverlay,
			colour
		)
		poseStack.popPose()
	}

	override fun preRender(
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
		grabRelevantBones(model)
		applyBaseTransformations(this.baseHumanoidModel)
		scaleModelForBaby(poseStack, animatable, partialTick, false)
		scaleModelForRender(
			this.scaleWidth,
			this.scaleHeight,
			poseStack,
			animatable,
			lastModelInstance,
			false,
			partialTick,
			packedLight,
			packedOverlay
		)

		currentSlot?.run { applyBoneVisibilityBySlot(this) }
	}

	override fun firePreRenderEvent(
		poseStack: PoseStack,
		model: ModelInstance?,
		bufferSource: MultiBufferSource,
		partialTick: Float,
		packedLight: Int
	): Boolean {
		return true
	}

	fun scaleModelForBaby(poseStack: PoseStack, animatable: S, partialTick: Float, isReRender: Boolean) {
		if (!this.young || isReRender) return
		baseHumanoidModel?.apply {
			if (currentSlot == EquipmentSlot.HEAD) {
				if (scaleHead) {
					val headScale: Float = 1.5f / babyHeadScale

					poseStack.scale(headScale, headScale, headScale)
				}

				poseStack.translate(0f, babyYHeadOffset / 16f, babyZHeadOffset / 16f)
			} else {
				val bodyScale: Float = 1 / babyBodyScale

				poseStack.scale(bodyScale, bodyScale, bodyScale)
				poseStack.translate(0f, bodyYOffset / 16f, 0f)
			}
		}
	}

	override fun renderBones(
		name: String,
		bone: OBone,
		pose: ModelPose,
		poseStack: PoseStack,
		buffer: VertexConsumer,
		packedLight: Int,
		packedOverlay: Int,
		color: Int,
		partialTick: Float,
		force: Boolean
	) {
		if (!(boneInfoExpand[name]?.visible ?: true)) return

		poseStack.pushPose()

		val accumulatedTransformMatrix = bone.getAccumulatedModelPartTransformMatrix()
		if (accumulatedTransformMatrix != null) {
			poseStack.pushPose()
			poseStack.mulPose(accumulatedTransformMatrix)
		}

		bone.tmpM4.identity()
		tmpM3.identity()
		bone.applyTransformWithParents(pose, bone.tmpM4, partialTick)
		poseStack.mulPose(bone.tmpM4)

		for (cube in bone.cubes) {
			renderCubes(cube, poseStack, buffer, packedLight, packedOverlay, color, force)
		}

		renderMesh(poseStack, bone.mesh, bone, buffer, packedLight, packedOverlay, color, partialTick, force)

		if (accumulatedTransformMatrix != null) {
			poseStack.popPose()
		}
		poseStack.popPose()
	}

	override fun doPostRenderCleanup() {
		// 只清除必要的数据，保留缓存结构以提高下一帧性能
		boneInfoExpand.values.forEach { info ->
			info.visible = false
			info.matrix4f = null
			// 不清除 cachedChildren 和 cachedParent，因为骨骼层级结构不会改变

		}

		currentEntity = null
		currentStack = null
		currentSlot = null
		baseHumanoidModel = null
		baseLastModelInstance = null
		animatable = null
		bufferSource = null
		partialTick = 0f
		limbSwing = 0f
		limbSwingAmount = 0f
		netHeadYaw = 0f
		headPitch = 0f

		isFineArm = false
		attackTime = 0f
		riding = false
		armorFineRightArm = null
		armorFineLeftArm = null
		armorPants = null
		armorHead = null
		armorBody = null
		armorRightArm = null
		armorLeftArm = null
		armorRightLeg = null
		armorLeftLeg = null
		armorRightBoot = null
		armorLeftBoot = null
	}

	fun getModel(): OModel? {
		return getModelInstance()?.origin
	}

	fun getModelInstance(): ModelInstance? {
		return lastModelInstance
	}

	fun getBone(name: String): OBone? {
		return getBoneInfo(name)?.bone
	}

	fun getBoneInfo(boneName: String): OBoneInfo? {
		return boneInfoExpand[boneName]
	}

	fun setAllBonesVisible(visible: Boolean) {
		boneInfoExpand.values.forEach { it.visible = visible }
	}

	protected fun grabRelevantBones(bakedModelInstance: ModelInstance?) {
		if (this.lastModelInstance === bakedModelInstance) return

		this.lastModelInstance = bakedModelInstance

		// 只在模型实例改变时重建 boneInfoExpand
		val newBoneInfoMap = mutableMapOf<String, OBoneInfo>()
		lastModelInstance?.origin?.bones?.forEach { (_, v) ->
			// 尝试复用已有的 OBoneInfo 以保留缓存
			val existingInfo = boneInfoExpand[v.name]
			newBoneInfoMap[v.name] = if (existingInfo != null && existingInfo.bone === v) {
				// 复用已有对象，重置状态但保留缓存
				existingInfo.visible = false
				existingInfo.matrix4f = null
				existingInfo
			} else {
				// 创建新对象
				OBoneInfo(v)
			}
		}
		boneInfoExpand.clear()
		boneInfoExpand.putAll(newBoneInfoMap)

		getModel() ?: return

		bipedHead = getBone("bipedHead")
		armorHead = getBone("armorHead")

		bipedBody = getBone("bipedBody")
		armorPants = getBone("armorPants")
		armorBody = getBone("armorBody")

		bipedRightArm = getBone("bipedRightArm")
		armorRightArm = getBone("armorRightArm")
		armorFineRightArm = getBone("armorFineRightArm")

		bipedLeftArm = getBone("bipedLeftArm")
		armorLeftArm = getBone("armorLeftArm")
		armorFineLeftArm = getBone("armorFineLeftArm")

		bipedRightLeg = getBone("bipedRightLeg")
		armorRightLeg = getBone("armorRightLeg")
		armorRightBoot = getBone("armorRightBoot")

		bipedLeftLeg = getBone("bipedLeftLeg")
		armorLeftLeg = getBone("armorLeftLeg")
		armorLeftBoot = getBone("armorLeftBoot")
	}

	protected fun applyBoneVisibilityBySlot(currentSlot: EquipmentSlot) {
		setAllBonesVisible(false)

		val renderer = this@SparkGeoArmorRenderer
		baseHumanoidModel?.run {
			when (currentSlot) {
				EquipmentSlot.HEAD -> {
					renderer.bipedHead.setRecursionVisible(head.visible)
				}

				EquipmentSlot.CHEST -> {
					renderer.bipedBody.setRecursionVisible(body.visible)
					renderer.bipedRightArm.setRecursionVisible(rightArm.visible)
					renderer.bipedLeftArm.setRecursionVisible(rightArm.visible)

					renderer.armorRightArm.setRecursionVisible(!isFineArm && rightArm.visible)
					renderer.armorLeftArm.setRecursionVisible(!isFineArm && rightArm.visible)
					renderer.armorFineRightArm.setRecursionVisible(isFineArm && rightArm.visible)
					renderer.armorFineLeftArm.setRecursionVisible(isFineArm && rightArm.visible)
				}

				EquipmentSlot.LEGS -> {
					renderer.armorRightLeg.setRecursionVisible(rightLeg.visible)
					renderer.armorLeftLeg.setRecursionVisible(rightLeg.visible)
				}

				EquipmentSlot.FEET -> {
					renderer.armorRightBoot.setRecursionVisible(rightLeg.visible)
					renderer.armorLeftBoot.setRecursionVisible(leftLeg.visible)
				}

				else -> {}
			}
		}
	}

	protected fun applyBaseTransformations(baseModel: HumanoidModel<*>?) {
		baseModel ?: return
		bipedHead?.apply {
			setMatrix4f(baseModel.head.run { matrix4f(0, -this.y * 2, 0) })
		}
		bipedBody?.apply {
			setMatrix4f((baseModel.body.run { matrix4f(0, -this.y * 2, 0) }))
		}
		bipedRightArm?.apply {
			setMatrix4f((baseModel.rightArm.run { matrix4f(5, -2, 0) }))
		}
		bipedLeftArm?.apply {
			setMatrix4f((baseModel.leftArm.run { matrix4f(-5, -2, 0) }))
		}
		bipedRightLeg?.apply {
			setMatrix4f((baseModel.rightLeg.run { matrix4f(2, -12, 0) }))
		}
		bipedLeftLeg?.apply {
			setMatrix4f((baseModel.leftLeg.run { matrix4f(2, -12, 0) }))
		}
	}

	fun ModelPart.matrix4f(x: Number, y: Number, z: Number): Matrix4f = Matrix4f()
		.rotateZYX(zRot, -yRot, -xRot)
		.translate(
			(x.toFloat() + this.x) / 16f,
			(y.toFloat() + this.y) / 16f,
			(z.toFloat() + this.z) / 16f
		)

	fun prepForRender(
		animatable: S,
		entity: Entity?,
		stack: ItemStack,
		slot: EquipmentSlot?,
		baseModel: HumanoidModel<*>?,
		bufferSource: MultiBufferSource?,
		partialTick: Float,
		limbSwing: Float,
		limbSwingAmount: Float,
		netHeadYaw: Float,
		headPitch: Float
	) {
		this.baseHumanoidModel = baseModel
		(entity as? IAnimatable<*>)?.apply {
			baseLastModelInstance = modelController.model
		}
		this.currentEntity = entity
		this.currentStack = stack
		this.animatable = animatable
		this.currentSlot = slot
		this.bufferSource = bufferSource
		this.partialTick = partialTick
		this.limbSwing = limbSwing
		this.limbSwingAmount = limbSwingAmount
		this.netHeadYaw = netHeadYaw
		this.headPitch = headPitch

		if (currentEntity is AbstractClientPlayer) {
			this.isFineArm = (currentEntity as AbstractClientPlayer).skin.model() == PlayerSkin.Model.SLIM
		}
	}

	fun OBone?.setVisible(visible: Boolean) {
		this ?: return
		boneInfoExpand[name]?.visible = visible
	}

	fun OBone?.setRecursionVisible(visible: Boolean) {
		this ?: return
		// 使用栈代替递归，避免深层递归导致的栈溢出和性能问题
		val stack = ArrayDeque<OBone>()
		stack.add(this)

		while (stack.isNotEmpty()) {
			val current = stack.removeFirst()
			current.setVisible(visible)

			// 将子骨骼加入栈中
			val children = current.getChildrenBone()
			if (children.isNotEmpty()) {
				stack.addAll(children)
			}
		}
	}

	fun OBone?.isVisible(): Boolean {
		this ?: return false
		return boneInfoExpand[name]?.visible ?: true
	}

	fun OBone?.getInfo(): OBoneInfo? {
		this ?: return null
		return boneInfoExpand[name]
	}

	fun OBone?.getPose(): BonePose? {
		this ?: return null
		return lastModelInstance?.pose?.bonePoses[name]
	}

	fun OBone?.getChildrenBone(): List<OBone> {
		this ?: return emptyList()
		// 缓存结果到 OBoneInfo 中，避免每次都遍历所有骨骼
		val info = getInfo()
		if (info?.cachedChildren != null) return info.cachedChildren!!

		val children = rootModel.bones.values.filter { it.parentName == this.name }
		info?.cachedChildren = children
		return children
	}

	fun OBone?.getParentBone(): OBone? {
		this ?: return null
		// 缓存父骨骼引用，避免每次通过 parentName 查找
		val info = getInfo()
		if (info?.cachedParent !== null) return info.cachedParent

		val parent = parentName?.let { rootModel.bones[it] }
		info?.cachedParent = parent
		return parent
	}

	/**
	 * 获取从根骨骼到当前骨骼的所有 ModelPart 变换的累积矩阵
	 * 参考 OBone.kt 中 applyTransformWithParents 的实现方式
	 * 返回完整的 4x4 变换矩阵（包含平移 + 旋转）
	 */
	fun OBone?.getAccumulatedModelPartTransformMatrix(): Matrix4f? {
		this ?: return null

		// 收集从当前骨骼到根骨骼路径上的所有有 ModelPart 变换的骨骼
		val transformChain = mutableListOf<Matrix4f>()
		var current: OBone? = this

		while (current != null) {
			val transformData = current.getInfo()?.matrix4f
			if (transformData != null) {
				transformChain.add(transformData)
			}
			current = current.getParentBone()
		}

		if (transformChain.isEmpty()) return null

		// 参考 OBone.kt 的 applyTransformWithParents：从根到当前骨骼的顺序应用变换
		val result = Matrix4f().identity()

		// 反向遍历，从根骨骼开始应用变换（与 l.asReversed() 一致）
		for (i in transformChain.indices.reversed()) {
			val transformData = transformChain[i]
			// 直接相乘已预计算的矩阵
			result.mul(transformData)
		}

		return result
	}

	fun OBone?.setMatrix4f(matrix4f: Matrix4f) {
		this ?: return
		boneInfoExpand[name]?.matrix4f = matrix4f
	}

	data class OBoneInfo(
		var bone: OBone,
		var visible: Boolean = true,
		var matrix4f: Matrix4f? = null,  // 存储预计算的变换矩阵
		var cachedChildren: List<OBone>? = null,  // 缓存子骨骼列表
		var cachedParent: OBone? = null  // 缓存父骨骼引用
	)
}
