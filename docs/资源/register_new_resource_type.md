# 注册新的资源类型

注册新资源类型需要完成三个步骤：**定义数据类 → 创建动态注册表 → 实现并注册 Handler**。

<!-- TOC -->
* [注册新的资源类型](#注册新的资源类型)
  * [步骤一：定义资源数据类](#步骤一定义资源数据类)
  * [步骤二：创建动态注册表](#步骤二创建动态注册表)
  * [步骤三：实现 ResourceHandler](#步骤三实现-resourcehandler)
  * [注册机制说明](#注册机制说明)
  * [资源文件放置](#资源文件放置)
    * [Citations](#citations)
<!-- TOC -->

---

## 步骤一：定义资源数据类

创建你的资源数据类，例如 `OMyResource`：

- kotlin

```kotlin
class OMyResource(val data: String)
```

- java

```java
public record OMyResource(String data) {
}
```

---

## 步骤二：创建动态注册表

在你的注册表对象中（参考 `SparkRegistries.kt` 的模式），使用 `SparkCore.REGISTER.registry<T>()` 创建一个
`DynamicAwareRegistry`，并设置 `onDynamicRegister` / `onDynamicUnregister` 回调用于网络同步：

- kotlin

```kotlin
val MY_RESOURCES =
  (SparkCore.REGISTER.registry<OMyResource>()
    .id("my_resources")
    .valueType(OMyResource::class)
    .build { it.sync(true).create() } as? DynamicAwareRegistry<OMyResource>)
    ?.apply {
      this.onDynamicRegister = { key, value ->
        // 可选：发送同步包到客户端
      }
      this.onDynamicUnregister = { key, value ->
        // 可选：发送移除同步包
      }
    } ?: throw IllegalStateException("...")
```

- java

```java
public static final DynamicAwareRegistry<OMyResource> MY_RESOURCES;

static {
  // 1. 获取基础注册表构建器
  var builder = SparkCore.REGISTER.registry(OMyResource.class)
          .id("my_resources")
          .valueType(OMyResource.class);

  // 2. 构建并尝试转换为 DynamicAwareRegistry
  // 注意：Kotlin 的 .build { it.sync(true).create() } 在 Java 中通常对应链式调用或特定的构建参数
  // 这里假设 build 返回的是一个 Registry 对象，我们需要将其强转或确保它实现了 DynamicAwareRegistry
  Object rawRegistry = builder.build(r -> r.sync(true).create());

  if (!(rawRegistry instanceof DynamicAwareRegistry)) {
    throw new IllegalStateException("Failed to cast registry to DynamicAwareRegistry for my_resources");
  }

  @SuppressWarnings("unchecked")
  DynamicAwareRegistry<OMyResource> registry = (DynamicAwareRegistry<OMyResource>) rawRegistry;

  // 3. 设置动态注册的回调逻辑
  registry.setOnDynamicRegister((key, value) -> {
    // 可选：发送同步包到客户端
  });

  registry.setOnDynamicUnregister((key, value) -> {
    // 可选：发送移除同步包
  });

  MY_RESOURCES = registry;
}
```

---

## 步骤三：实现 ResourceHandler

继承 `ResourceHandlerBase`，实现三个核心抽象方法，并在静态初始化块中调用 `HandlerDiscoveryService.registerHandler` 完成自动注册：

- kotlin

```kotlin
@AutoRegisterHandler
class MyResourceHandler(
  private val registry: DynamicAwareRegistry<OMyResource>
) : ResourceHandlerBase() {

  companion object {
    init {
      // 关键：在静态初始化时注册工厂方法
      HandlerDiscoveryService.registerHandler {
        MyResourceHandler(MyRegistries.MY_RESOURCES)
      }
    }
  }

  override fun getResourceType(): String = "my_resources"  // 对应目录名
  override fun getRegistryIdentifier() = registry.key().location()
  override fun getSupportedExtensions() = setOf("json")

  override fun processResourceAdded(node: ResourceNode) {
    // 解析文件，注册到 registry
    val content = node.basePath.resolve(node.relativePath).readText()
    val resource = OMyResource(content)
    val key = ResourceKey.create(registry.key(), node.id)
    registry.register(key, resource, RegistrationInfo.BUILT_IN)
  }

  override fun processResourceModified(node: ResourceNode) = processResourceAdded(node)

  override fun processResourceRemoved(node: ResourceNode) {
    val key = ResourceKey.create(registry.key(), node.id)
    registry.unregisterDynamic(key)
  }

  override fun initialize(modMainClass: Class<*>): Boolean {
    val paths = ResourceDiscoveryService.discoverResourcePaths(getResourceType())
    for (basePath in paths) {
      ResourceDiscoveryService.scanResourceFiles(basePath, getSupportedExtensions())
        .forEach { onResourceAdded(it) }
    }
    return true
  }

  override fun initializeDefaultResources(modMainClass: Class<*>): Boolean {
    return MultiModuleResourceExtractionUtil.extractAllModuleResources(modMainClass, getResourceType())
  }
}
```

- java

```java

public class MyResourceHandler extends ResourceHandlerBase {

  private final DynamicAwareRegistry<OMyResource> registry;

  public MyResourceHandler(DynamicAwareRegistry<OMyResource> registry) {
    this.registry = registry;
  }

  static {
    // 关键：在静态初始化时注册工厂方法
    HandlerDiscoveryService.registerHandler(() -> new MyResourceHandler(MyRegistries.MY_RESOURCES));
  }

  @Override
  public String getResourceType() {
    return "my_resources"; // 对应目录名
  }

  @Override
  public ResourceLocation getRegistryIdentifier() {
    return registry.key().location();
  }

  @Override
  public Set<String> getSupportedExtensions() {
    return Set.of("json");
  }

  @Override
  protected void processResourceAdded(ResourceNode node) {
    try {
      // 解析文件，注册到 registry
      String content = Files.readString(node.getBasePath().resolve(node.getRelativePath()));
      OMyResource resource = new OMyResource(content);
      ResourceKey<OMyResource> key = ResourceKey.create(registry.key(), ResourceLocation.parse(node.getId()));
      registry.register(key, resource, RegistrationInfo.BUILT_IN);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void processResourceModified(ResourceNode node) {
    processResourceAdded(node);
  }

  @Override
  protected void processResourceRemoved(ResourceNode node) {
    ResourceKey<OMyResource> key = ResourceKey.create(registry.key(), ResourceLocation.parse(node.getId()));
    registry.unregisterDynamic(key);
  }

  @Override
  public boolean initialize(Class<?> modMainClass) {
    List<Path> paths = ResourceDiscoveryService.discoverResourcePaths(getResourceType());
    for (Path basePath : paths) {
      ResourceDiscoveryService.scanResourceFiles(basePath, getSupportedExtensions())
              .forEach(this::onResourceAdded);
    }
    return true;
  }

  @Override
  public boolean initializeDefaultResources(Class<?> modMainClass) {
    return MultiModuleResourceExtractionUtil.extractAllModuleResources(modMainClass, getResourceType());
  }
}
```

---

## 注册机制说明

`HandlerDiscoveryService.registerHandler` 接收一个工厂 lambda，在 `discoverAndInitializeHandlers()` 被调用时实例化所有已注册的
Handler。如果没有任何 Handler 通过此方式注册，系统会 fallback 到硬编码的内置 Handler 列表。

---

## 资源文件放置

注册完成后，将资源文件放在对应目录下即可被自动扫描：

```
run/sparkcore/{modId}/{moduleName}/my_resources/
```

资源 ID 格式为 `{modId}:{moduleName}/my_resources/{path}`（不含扩展名）。

### Citations

**File:** cn.solarmoon.spark_core.registry.common.SparkRegistries (L23-60)

```kotlin
    val TYPED_ANIMATION =
  (SparkCore.REGISTER.registry<TypedAnimation>()
    .id("typed_animation")
    .valueType(TypedAnimation::class) // Pass KClass for DynamicAwareRegistry
    .build { it.sync(true).create() } as? DynamicAwareRegistry<TypedAnimation>)
    ?.apply {
      // 'this' is now safely cast to DynamicAwareRegistry<TypedAnimation>
      this.onDynamicRegister = { key, value ->
        // 只在服务端发送同步包，避免客户端发送clientbound包错误
        try {

          val server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()
          if (server != null) {
            val packet =
              DynamicRegistrySyncS2CPacket.createForTypedAnimationAdd(key.location().namespace, key.location(), value)
            net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(packet)
            SparkCore.LOGGER.info("Triggered dynamic TypedAnimation ADD sync for ${key.location()} via callback")
          } else {
            SparkCore.LOGGER.debug("客户端跳过动画ADD同步，等待服务端同步: ${key.location()}")
          }
        } catch (e: Exception) {
          SparkCore.LOGGER.debug("动画ADD同步跳过（可能在客户端）: ${key.location()}")
        }
      }
      this.onDynamicUnregister = { key, value ->
        // 只在服务端发送同步包，避免客户端发送clientbound包错误
        try {
          val server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()
          if (server != null) {
            DynamicRegistrySyncS2CPacket.syncTypedAnimationRemovalToClients(key.location().namespace, key.location())
            SparkCore.LOGGER.info("Triggered dynamic TypedAnimation REMOVAL sync for ${key.location()} via callback. Animation: $value")
          } else {
            SparkCore.LOGGER.debug("客户端跳过动画REMOVAL同步，等待服务端同步: ${key.location()}")
          }
        } catch (e: Exception) {
          SparkCore.LOGGER.debug("动画REMOVAL同步跳过（可能在客户端）: ${key.location()}")
        }
      }
    }
    ?: throw IllegalStateException("TYPED_ANIMATION registry could not be cast to DynamicAwareRegistry. Check ObjectRegister implementation.")
```

**File:** src/main/kotlin/cn/solarmoon/spark_core/resource/handler/AnimationHandler.kt (L29-40)

```kotlin
@AutoRegisterHandler
class AnimationHandler(
  private val typedAnimationRegistry: DynamicAwareRegistry<TypedAnimation>
) : ResourceHandlerBase() {

  companion object {
    init {
      HandlerDiscoveryService.registerHandler {
        AnimationHandler(cn.solarmoon.spark_core.registry.common.SparkRegistries.TYPED_ANIMATION)
      }
    }
  }
}
```

**File:** cn.solarmoon.spark_core.resource.common.ResourceHandlerBase.kt (L13-65)

```kotlin
abstract class ResourceHandlerBase : IModuleAwareResourceHandler, IHotReloadAwareHandler, IResourceHandlerValidator {

  protected var initialScanComplete = false
  protected val moduleResources = ConcurrentHashMap<String, MutableSet<ResourceLocation>>()

  // 实现IResourceHandler的基础方法
  override fun onResourceAdded(filePath: Path) {
    val node = ResourceGraphManager.addOrUpdateResource(filePath, getResourceType())
    if (node != null) {
      try {
        processResourceAdded(node)
        ResourceHandlerLogger.logResourceAdded(getResourceType(), node.id, node.getFullModuleId())
      } catch (e: Exception) {
        ResourceHandlerLogger.logHandlerError(getResourceType(), "ADD", node.id, e)
        throw e
      }
    }
  }

  override fun onResourceModified(filePath: Path) {
    val node = ResourceGraphManager.addOrUpdateResource(filePath, getResourceType())
    if (node != null) {
      try {
        processResourceModified(node)
        ResourceHandlerLogger.logResourceModified(getResourceType(), node.id, node.getFullModuleId())
      } catch (e: Exception) {
        ResourceHandlerLogger.logHandlerError(getResourceType(), "MODIFY", node.id, e)
        throw e
      }
    }
  }

  override fun onResourceRemoved(filePath: Path) {
    // 对于删除，我们无法轻易地从路径反向解析出完整的ResourceLocation，
    // 这是一个新架构需要解决的问题。
    // 暂时策略：尝试让GraphManager处理，如果它能找到并删除，就继续。
    val node = ResourceGraphManager.findNodeByPath(filePath) // 需要在GraphManager中实现此方法
    if (node != null) {
      try {
        ResourceGraphManager.removeResource(node.id)
        processResourceRemoved(node)
        ResourceHandlerLogger.logResourceRemoved(getResourceType(), node.id, node.getFullModuleId())
      } catch (e: Exception) {
        ResourceHandlerLogger.logHandlerError(getResourceType(), "REMOVE", node.id, e)
        throw e
      }
    }
  }

  // 抽象方法，具体处理器需要实现
  protected abstract fun processResourceAdded(node: cn.solarmoon.spark_core.resource.graph.ResourceNode)
  protected abstract fun processResourceModified(node: cn.solarmoon.spark_core.resource.graph.ResourceNode)
  protected abstract fun processResourceRemoved(node: cn.solarmoon.spark_core.resource.graph.ResourceNode)
}
```

**File:** cn.solarmoon.spark_core.resource.autoregistry.HandlerDiscoveryService.kt (L28-49)

```kotlin
    @JvmStatic
fun registerHandler(factory: () -> IResourceHandler) {
  registeredHandlerFactories.add(factory)
  SparkCore.LOGGER.debug("注册handler工厂: {}", factory.javaClass.simpleName)
}

private fun findHandlerImplementations(): List<IResourceHandler> {
  // 优先使用自动注册的handlers
  val autoRegisteredHandlers = registeredHandlerFactories.map { factory ->
    try {
      factory()
    } catch (e: Exception) {
      SparkCore.LOGGER.error("创建自动注册的handler失败", e)
      null
    }
  }.filterNotNull()

  // 如果有自动注册的handlers，使用它们
  if (autoRegisteredHandlers.isNotEmpty()) {
    SparkCore.LOGGER.info("使用自动注册的handlers: {} 个", autoRegisteredHandlers.size)
    return autoRegisteredHandlers
  }
}
```

**File:** [resource_creation_guide.md](resource_creation_guide.md) (L55-72)

```markdown
### 2.2 资源ID的生成规则

资源ID由系统根据四层目录结构自动生成，格式为 `{modId}:{moduleName}/{resourceType}/{path}`。

- **`{modId}`**: 模组标识符，对应第一层目录名（如 `my_mod`, `spark_core`）
- **`{moduleName}`**: 模块名称，对应第二层目录名（如 `my_mod`, `combat_system`）
- **`{resourceType}`**: 资源类型，对应第三层目录名（如 `animations`, `models`）
- **`{path}`**: 从第四层开始到**不包含扩展名**的文件名为止的相对路径

**四层结构示例**:
| 文件物理路径 | 生成的资源ID |
|---|---|
| `run/sparkcore/my_mod/my_mod/animations/player/combat/sword_attack.json` |
`my_mod:my_mod/animations/player/combat/sword_attack` |
| `run/sparkcore/my_mod/combat_system/animations/sword/combo.json` | `my_mod:combat_system/animations/sword/combo` |
| `run/sparkcore/spark_core/sparkcore/models/player.json` | `spark_core:sparkcore/models/player` |
| `run/sparkcore/spark_core/sparkcore/textures/gui/icons.png` | `spark_core:sparkcore/textures/gui/icons` |
| `run/sparkcore/another_mod/magic_system/scripts/fireball.js` | `another_mod:magic_system/scripts/fireball` |

```
