package architecture.resonator_combat_framework.animation

import architecture.goldenboughs_lib.util.toPos
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.model.DynamicGeometryModel
import architecture.resonator_combat_framework.model.PoseData
import com.mojang.math.Axis
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3dc
import org.mesdag.particlestorm.data.molang.MolangExp
import org.mesdag.particlestorm.particle.MolangParticleEngine
import org.mesdag.particlestorm.particle.ParticleEmitter

/**
 * ParticleStorm 动画适配器。
 *
 * 功能对标 ParticleStorm 的 GeckoLibHelper：
 * - 粒子发射器生命周期管理（动画切换/结束自动清理）
 * - 每帧更新 parentSpace 实现骨骼追踪
 * - 发射器缓存复用（同一定位器重复使用）
 */
object ParticleStormAnimAdapter {

	/**
	 * 生成发射器追踪键：controllerId + locatorName。
	 *
	 * @param controllerName 控制器 ID
	 * @param locatorName 定位器名称（null 表示实体本身）
	 * @return 追踪用字符串键
	 */
	private fun trackKey(controllerName: ResourceLocation, locatorName: String?): String =
		"${controllerName}:${locatorName ?: "@entity"}"

	/**
	 * 尝试使用 ParticleStorm 生成粒子发射器。
	 *
	 * @param entity      动画所属实体
	 * @param particleId  粒子资源 ID
	 * @param locatorName 定位器名称（可为 null）
	 * @param preEffectScriptStr 预效果 MoLang 脚本字符串
	 * @param brModel     几何模型数据
	 * @param animationData 当前帧代理骨骼数据
	 * @param controller  触发该粒子的动画控制器
	 * @param manager     动画控制器管理器
	 * @return true 表示已由 ParticleStorm 处理
	 */
	fun trySpawnParticle(
		entity: Entity,
		particleId: ResourceLocation,
		locatorName: String?,
		preEffectScriptStr: String?,
		brModel: DynamicGeometryModel,
		animationData: PoseData,
		controller: IEntityAnimationController<*>,
		manager: AnimationControllerManager<*>,
	): Boolean {
		if (!entity.level().isClientSide) return false
		if (!MolangParticleEngine.INSTANCE.id2Emitter().containsKey(particleId)) return false

		val controllerId = controller.id

		// 1. 清除同定位器的旧发射器（粒子动画改变时重新创建）
		manager.clearEmitter(controllerId, locatorName)

		// 2. 计算定位器世界坐标
		val vector3f = entity.position().toVector3f()
		val matrix = Matrix4f()
			.translate(vector3f.x, vector3f.y, vector3f.z)
			.rotate(Axis.YP.rotation(-entity.getPreciseBodyRotation(1.0f)))
			.mul(brModel.computeLocatorGlobalMatrix(locatorName, animationData, isWorld = true))
		val pos = matrix.toPos().toVec3()

		// 3. 创建发射器
		val expression = if (preEffectScriptStr != null) MolangExp(preEffectScriptStr) else MolangExp.EMPTY
		val emitter = ParticleEmitter(entity.level(), pos, particleId, expression)
		emitter.attachEntity(entity)
		emitter.parentSpace = matrix

		// 4. 注册到管理器追踪
		MolangParticleEngine.INSTANCE.addEmitter(emitter, false)
		manager.trackEmitter(controllerId, locatorName, emitter.id)
		return true
	}

	/**
	 * 将 [Vector3dc] 转为 Minecraft 的 [Vec3]。
	 */
	fun Vector3dc.toVec3(): Vec3 = Vec3(x(), y(), z())

	/**
	 * 更新指定发射器的 parentSpace 以跟踪骨骼动画。
	 * 每渲染帧调用，对标 GeckoLibHelper.transformLocator()。
	 */
	fun updateEmitterTransform(
		emitterId: Int,
		brModel: DynamicGeometryModel,
		animationData: PoseData,
		locatorName: String?
	) {
		val emitter = MolangParticleEngine.INSTANCE.getEmitter(emitterId) ?: return
		if (emitter.isRemoved) return
		emitter.parentSpace = brModel.computeLocatorGlobalMatrix(locatorName, animationData, isWorld = true)
	}
}
