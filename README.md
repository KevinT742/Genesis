# Genesis

**Minecraft 26.2 · NeoForge 26.2.0.93 · Java 25**

> 本模组是 [KevinT742MinecraftMod](../README.md) 家族仓库的成员 **base**，源码位于仓库的 `base/` 目录。

Genesis 把 Minecraft 变成一颗**行星**。它不是一个玩法模组，而是模组家族的**地基**：统一材质模板、物品注册信息与魔法模型、真实世界生成、结构体系与多星球架构。

---

## 一、核心能力

### 1. 统一材质模板（物理 / 化学 / 魔法）

每一个方块与物品——无论原版还是模组家族新增——都由一份 `MaterialProfile` 描述：

| 维度 | 内容 |
|---|---|
| **物理** PhysicalProperties | 密度、体积、硬度、抗冲击、弹性、导热率、比热、熔点、沸点、摩擦、发光 |
| **化学** ChemicalProperties | 化学构成（元素与个数）、摩尔质量、氧化态、物态、反应性、溶解性、可燃性、毒性、导电性、腐蚀性、燃烧热 |
| **魔法** MagicProperties | 元素亲和、魔力容量、熵、共振频率、稳定性、契合度、**适应度**、**引导效率**、**留存** |

方块通过 `BlockTemplate` 注册，物品通过 `ItemTemplate` 注册；Minecraft 侧的行为（挖掘速度、爆炸抗性、声音、摩擦、发光、堆叠、稀有度）全部由 profile **推导**，而不是各写各的。

```java
BlockTemplate template = BlockTemplate.builder("arcane_brick")
        .profile(MaterialFamily.arcane("genesis:arcane_brick"))
        .toolTier(BlockTemplate.ToolTier.DIAMOND)
        .sound(BlockTemplate.SoundKind.STONE)
        .build();

BLOCKS.registerSimpleBlock("arcane_brick", GenesisApi.blockProperties(template));
```

`MaterialRegistry` 是全局注册表：**原版世界也是全覆盖的**——游戏里的每一个方块和物品都有档案，所以不存在"原版的东西我们的模型不认识"这种缺口。

#### 原版世界怎么被认出来的

原版有 **1196 个方块**和 **1537 个物品**。逐个手写档案既不现实也容易错，所以材质是**按标识符的规则**推出来的（`mc/material/VanillaMaterials`）：原版的标识符本身是成系统的——`iron_ingot`、`iron_ore`、`iron_sword` 都带着同一个词，那个词就说明了它是什么做的。

两条规则保证结果自洽：

* **同一个标识符永远给出同一种材质**，所以一个方块和"放置它的那个物品"是同一种东西；
* **用量由东西的种类决定**：方块是一立方米，金属锭是其中的一小块，工具比锭多一点，食物是一份。

> 方块物品**不是**按名字拿材质的，而是**继承它放置的那个方块**的材质——因为二者的标识符不一定相同（一团线叫 `minecraft:string`，一格线块叫 `minecraft:string_block`）。

认不出来的物品不会漏掉：它拿到 `MaterialFamily.generic` 的档案，那份档案说的是"这个我们还不知道"，而不是假装知道。

> **物品优先用自己的标识符**，只有自己认不出来时才去看它放置的方块 —— 因为二者的标识符不一定相同（一团线是 `minecraft:string`，它放置的方块却是另一个 id）。

### 2. 真实地形、水系与生态系统

* **地形** `TerrainModel` —— 四层叠加：大陆度（决定海陆）、侵蚀（决定崎岖 vs 平坦）、山脊（只在内陆隆起）、细节噪声。海平面是基准线，`relative = seaLevel + …`。
* **水系** `HydrologyModel` —— 真实的两段式水文：
  1. **Priority-Flood 洼地填充**：所有洼地被抬升到溢出水位，空洞自然变成湖；
  2. **D8 径流累积**：降水沿最陡坡逐格下泄，流量超过阈值的格子成为河道，河宽与河深随流量增长。
* **生态系统** `EcologyModel` —— 温度由**纬度**（极地冷、赤道热）与**海拔**（每升高一格降温）决定；湿度来自大尺度噪声，并因靠近海岸与河道而升高；两者映射到生物群系族。

所有噪声都在**星球表面**的三维坐标上采样，因此地形、水文与气候在边界处天然无缝。

### 3. 连续的世界边界（星球感）

Genesis 的世界是**缠绕在球体上的圆柱**：

* **经度轴（X）周期化**：走到 `x = +halfSpan` 会从 `x = -halfSpan` 继续。`PlanetSurface.wrapLongitude` 负责折叠，玩家越界时由 `GenesisEvents` 传送回另一侧——脚下地面完全一致。
* **纬度轴（Z）映射到两极**：`|z| = halfSpan` 是北极与南极，越过极点后沿**同一条经线**折返，就像把平面地图沿边缘对折。

自检中这一项是**逐位精确**的：边界两侧地表点的差值为 `0.000000`，地形高度差为 `0`。

### 4. 多星球预留

一颗星球 = 一份 `PlanetDefinition`：

```java
new PlanetDefinition(
        "genesis:ember", "Ember",
        9000.0,        // 半径（格）
        0.72,          // 重力
        40000,         // 纬度半幅
        SamplingMode.SPHERE,
        false,         // 纬度轴是否也环绕
        AtmosphereSpec.thinDesert(),
        new TerrainProfile(...),
        HydrologyProfile.arid(),
        new EcologyProfile(...));
```

新增一颗星球只需写一份定义 + 一个 `dimension_type` JSON，生成器代码一行都不用改。已内置 **Terran**（类地）与 **Ember**（干燥、球面采样）两颗。

---

## 二、物品注册信息与魔法模型

### 物品注册的基本信息

每个物品由一份 `ItemTemplate` 注册，它把四组信息收在一处：

| 组 | 内容 | 来源 |
|---|---|---|
| **原版注册信息** | 标识、堆叠上限、稀有度、创造栏分类 | `ItemTemplate` 自身 |
| **物理信息** | 质量、体积、密度、莫氏硬度、导热、比热、熔点、沸点 | `MaterialProfile.physical()` |
| **化学信息** | 化学式与构成、摩尔质量、氧化态、物态、反应性、溶解度、可燃性、毒性、导电性、腐蚀性、燃烧热 | `MaterialProfile.chemical()` |
| **魔法信息** | 适应度、魔力容量、引导效率、留存、元素亲和、熵、共振、稳定性 | `MaterialProfile.magic()` |

物理量之间是**推导**关系，而不是各写各的：

* 质量 = 密度 × 体积。石头方块 1 m³、2600 kg/m³ → 2600 kg；一瓶 500 cm³ 的魔药 → 0.6 kg。
* 摩尔质量由**化学构成**算出：`SiO2` → 60.08 g/mol，并可读出质量分数（氧占石英 53.3%）。化学式支持括号（`Ca(OH)2`）。
* 物态由熔点/沸点与环境温度推出：`Phase.at(20 °C)` → 水是液体、石英是固体。
* 真实元素用真实常数：`ElementData` 里铁 7874 kg/m³、熔点 1538 °C、导电 1.0e7 S/m；金 19300 kg/m³、熔点 1064 °C。

**温度一律用热力学标度（开尔文）**，并由 `Temperature` 这个值类型承载：

```java
Temperature melting = iron.physical().meltingPoint();   // 1811.15 K
melting.celsius();          // 1538.0 —— 只在展示与读配置时转换
melting.differenceTo(other); // 温差在两套标度下数值相同
melting.format();           // "1811.15K（1538℃）" —— 调试界面的双标度格式
```

调试界面（F3）同时给出两套标度：模拟内部一律用开尔文，玩家看到的永远是摄氏。

原因有三：**比值只在绝对标度下有意义**（"这里比那里热两倍"只有在开尔文下为真）；**温差与温点必须是两种量**（退火率、极地温差、熔点不能混着加）；**偏移量是经典 bug 源**（273 与 0 是同一个温度点，混用会让水在室温下变成固体）。`Temperature` 在构造时拒绝低于绝对零度的值，降温操作钳位在 0 K。气候模型同样使用开尔文：`Climate.temperature`、`EcologyProfile.equatorialTemperature` 都是 `Temperature`，而**温差**（极地温差、高程 lapse rate）以开尔文数值表示——差值的数值与摄氏度一致，不存在标度歧义。

于是"这块铁有多重""这瓶魔药还能用几次"只有一个答案，而这个答案由物理与化学决定。

### 魔法从哪来

一切魔法都来自**对外界游离魔力的使用**。世界里每个地区都有自己的**魔力浓度**，浓度在**世界生成时**随种子初始化，之后可以被**外界操作**改变（仪式、献祭、汲取）。浓度分档：`NONE / TRACE / FAINT / MODERATE / STRONG / EXTREME`。

```java
MagicField field = new MagicField(new MagicFieldModel(planet, seed));  // 世界生成时初始化
double concentration = field.concentrationAt(x, z).value();             // 该地区的浓度
field.influence(x, z, 128.0, -0.6);                                    // 外界操作：抽干一片区域
```

**魔力场是持久化的。** 浓度**分布**由种子推出，处处相同，不必存；但**被施加过的影响**
（仪式、献祭、汲取）是玩家对世界做的事，必须写进存档。每个维度一份，写 NBT，
所以**服务器重启后仪式还在**，而且两个维度的魔力不会混。

```java
// 兄弟模组一律走 GenesisApi，不要伸进 Genesis 的内部结构
Optional<MagicField> field = GenesisApi.magicField(serverLevel);
field.ifPresent(f -> f.influence(x, z, 128.0, -0.6));   // 改了就是改了，存档里会有
```

`GenesisApi` 是家族成员进入 Genesis 的唯一入口，它给出：一个维度的**魔力场**、
**星球**、某个方块的**材质档案**，以及该位置的**浓度**与**强度**。

### 谁在操控魔力

* **魔力器官**：动物、植物都有器官；人（玩家、村民、女巫）的**灵魂**就是器官。器官有容量、引导效率、适应度与属性亲和，并分化为吸收型（根）、产出型（叶）、感知型（眼）、传导型（脉）、储存型（骨）。
* **魔力材料**：无生命的产物，对魔力的**适应度**够好并且带得动效果，就叫魔力材料。
* 炼药与炼金接受的材料 = **魔力器官** + **魔力材料**（无生命产物）。

### 魔法怎么被固化

| 形态 | 是什么 | 对应 |
|---|---|---|
| **固化的魔力** | 魔药、结晶体：把游离魔力收进材料里存着 | `MagicInput.ESSENCE` / `MaterialFamily.solidifiedMagic` |
| **固化的引导方式** | 炼金、附魔：把"怎么引导"写成配方或刻在物品上 | `MagicInput.CHANNEL` / `MagicChannelKind.ALCHEMY`、`ENCHANTMENT` |
| **引导方式本身** | 魔咒：说出口的引导方式，需要活着的施术者，说完即逝 | `MagicChannelKind.SPELL` |

`MagicProperties` 用三个量把这套模型落到每一种材质上：

| 量 | 含义 | 决定 |
|---|---|---|
| **适应度** adaptability | 材料多容易吃进魔力 | 是否算**魔力材料**（阈值 0.55） |
| **引导效率** channelEfficiency | 魔力能不能被**控制地**导过去 | 能不能当引导介质 |
| **留存** retention | 放进来的魔力能留多久 | 魔药能不能装瓶（`solidifiedMagic = 容量 × 适应度 × 留存`） |

`GenesisItems` 的五个物品正好覆盖这几种魔法输入：魔力材料（水晶碎片、奥术尘）、魔力器官（心脏核心）、固化的魔力（精粹瓶）、固化的引导方式（符文板）。

---

## 三、结构体系

三类结构，由 `SettlementPlanner` 在**服务器启动时基于世界种子一次性规划**，与区块加载顺序无关：

| 族 | 特征 |
|---|---|
| **聚落** SETTLEMENT | 有人居住，规模分 HAMLET / VILLAGE / TOWN / CITY，由道路网相连 |
| **遗迹** RUIN | 废弃、坍塌、与聚落保持距离 |
| **隐秘** HIDDEN | 罕见、埋于地下、需要刻意寻找 |

* 聚落位置用**黄金角螺旋**在世界球面上均匀撒点，再按地形适宜度过滤（海拔、平坦度、彼此间距 ≥ 900 格）。
* 道路网络用 **Prim 最小生成树**保证连通，再补若干近邻边形成环路；每条边用 **A\*** 在球面网格上寻路，水域代价高但可通行（= 修桥），陡坡代价高。
* 隐秘地点额外要求"异常度"噪声偏高，因此它们藏在看起来就不太一样的地方。

---

## 四、工程结构

```
base/src/main/java/genesis/
├── Genesis.java                  模组入口，注册区块生成器 / 生物源 / 方块 / 物品 / 创造标签
├── GenesisConfig.java            运行时配置（连续世界、结构开关、聚落密度）
├── GenesisPlanetCodecs.java      星球定义的 Codec（数据驱动）
├── api/                          ★ 模组家族的稳定扩展面
│   ├── material/                 MaterialProfile / MaterialFamily / BlockTemplate / MaterialRegistry
│   │                             Composition（化学构成）/ Phase（物态）/ ElementData（元素物理常数）
│   ├── physical/                 Temperature —— 热力学温度值类型（开尔文）
│   ├── item/                     ItemTemplate / ItemRarity / ItemCategory —— 物品注册信息
│   ├── magic/                    MagicInput / MagicOrgan / MagicMaterial / MagicChannel
│   │                             MagicConcentration / MagicTier —— 魔法模型
│   └── planet/                   PlanetDefinition / TerrainProfile / HydrologyProfile / EcologyProfile / AtmosphereSpec
├── core/                         ★ 纯算法层，不依赖 Minecraft
│   ├── math/                     Hash / Noise / Spline / MathUtil
│   ├── topology/                 PlanetSurface —— 球面映射与经度折叠
│   ├── terrain/                  TerrainModel / HydrologyModel / HydrologyGrid
│   ├── ecology/                  EcologyModel / Climate / BiomeKind
│   ├── magic/                    MagicFieldModel / MagicField —— 魔力浓度场
│   └── settlement/               SettlementPlanner / RoadPathfinder / WorldPlan
├── mc/                           ★ Minecraft / NeoForge 适配层
│   ├── GenesisBlocks.java        由模板注册方块
│   ├── GenesisItems.java         由模板注册物品
│   ├── GenesisEvents.java        越界传送、缓存清理
│   ├── world/                    GenesisChunkGenerator / GenesisBiomeSource / GenesisStructures
│   ├── material/                 原版方块的材质绑定
│   └── planet/                   星球注册表
└── dev/GenesisSelfTest.java      自检程序（不启动游戏即可验证算法）

../verification/                  ★ 离线验证工具（无需 NeoForge 仓库，位于家族根目录）
├── fetch26.ps1                   下载 Minecraft 26.2（仅 Mojang 官方源）
├── prep.ps1                      解包并生成编译类路径
├── build-and-test.ps1            编译全部源码 + 运行自检
├── gen-stubs.ps1                 生成 NeoForge API 桩件
├── gen-resources.ps1             生成模型 / 语言 / 维度 JSON
└── stubs/                        NeoForge API 最小桩件（不进产物）
```

**分层原则**：`api` 定义契约 → `core` 实现算法（零 Minecraft 依赖，可独立测试）→ `mc` 只做粘合。这既让家族成员能稳定扩展，也让核心算法能在没有游戏的环境里验证。

---

## 五、构建与运行

命令在**家族根目录**执行，用 `:base:` 指定成员：

```bash
./gradlew :base:build          # 构建
./gradlew :base:runClient      # 启动客户端
./gradlew :base:runServer      # 启动服务端
./gradlew :base:selfTest       # 运行算法自检（不需要游戏）
```

### 网络前提

构建需要访问 **Maven Central**、**Gradle Plugin Portal** 和 **`maven.neoforged.net`**。
若 `maven.neoforged.net` 不可达（国内常见），需要加两处镜像：根 `settings.gradle` 的
`pluginManagement`（Gradle 插件）与本工程 `build.gradle` 的 `repositories` 块（NeoForge 制品）。
完整写法见[家族根 README](../README.md)。

### 离线验证（无需 NeoForge 仓库）

当 `maven.neoforged.net` 完全不可达时，仍然可以**用真实的 Minecraft 26.2 jar 校验整个模组**。
`verification/` 目录提供了一套离线工具，它只从 Mojang 官方源下载游戏本体：

```powershell
cd verification            # 从家族根目录进入
pwsh -File fetch26.ps1        # 下载 Minecraft 26.2 客户端 / 服务端 / 依赖库（仅首次）
pwsh -File prep.ps1           # 解包并生成编译类路径
pwsh -File build-and-test.ps1 # 编译模组并运行自检
```

它会编译**全部**源码（包括 Minecraft 适配层），并跑完 145 条断言
（算法自检 104 条 + 材质自检 23 条 + 持久化自检 6 条 + 探针自检 12 条）。
`verification/stubs/` 是 NeoForge API 的最小桩件，仅用于离线编译校验，不会进入产物。

---

## 六、自检覆盖

`./gradlew :base:selfTest` 与 `./gradlew :base:persistenceTest`
（或 `verification/build-and-test.ps1`）校验 145 条断言：

| 分组 | 覆盖内容 |
|---|---|
| 材质模板 | 物理 / 化学 / 魔法的派生是否自洽；模板与注册表是否一致 |
| **物理与化学** | 化学式解析（含括号）与 Hill 记法；摩尔质量由构成算出；质量 = 密度 × 体积；物态随温度推出；真实元素常数（铁 7874 kg/m³、熔点 1538 °C） |
| **热力学温度** | 开尔文与摄氏度指向同一点；温差在两套标度下数值相同；低于绝对零度被拒绝；降温钳位在 0 K |
| **魔法模型** | 魔力材料判定（适应度阈值）；灵魂强于动物器官；根吸收、叶产出；炼金/附魔为固化引导方式、魔咒非固化且需活人；物品的堆叠/稀有度/分类/魔法输入 |
| **魔力场** | 地区间浓度有差异且分 ≥ 3 档；边界两侧浓度完全一致；同种子同魔力；外界操作改变局部浓度而不影响远处；浓度永不为负；干燥世界魔力更稀薄 |
| **魔力的存放** | 改动会写进它被给的那个列表；读回来的世界接着上次继续；每次改动都会被通知（世界因此知道该写盘） |
| **魔力的持久化** | 改动会到达被存下来的世界；**存盘的格式能往返**（编码→解码，数字一个不差）；读回来的世界保留被给过的魔力；没动过的世界不会写盘 |
| **原版材质** | **每一个原版方块与物品都有材质**（1196 方块 + 1537 物品，覆盖率 100%）；同名方块与物品是同一种材质；铁锭密度如铁、重量约一公斤；金块重 19.3 吨；钻石是碳；石头是二氧化硅；药水是固化的魔力、重半公斤；玻璃不是魔力材料；食物是有机物、一份重四分之一公斤；**37 个"世界真正由什么做成"的东西不能落在兜底档案上**；**查得到**（回归测试） |
| **星球连续性** | 边界两侧地表点差值为 `0.000000`；地形高度差为 `0`；极点折返同经线；经度折叠正确 |
| 地形与水文 | 同时存在陆与洋（海洋占比 ≈ 0.21）；起伏足够；能产出河流（≈ 0.08%）；洼地能填成湖（≈ 0.13%）；水面永不低于地面；流量沿程汇聚 |
| 生态系统 | 两极冷于赤道；能产出 ≥ 4 个群系族（实测 7 个） |
| 结构与道路 | 聚落 / 道路 / 遗迹 / 隐秘全部生成；**每个聚落都能经道路抵达**（71/71）；每条道路确实连到它声明的两个聚落 |
| 确定性 | 同种子 ⇒ 同地形、同聚落 |

最近一次运行：**145 checks / 0 failures / SELF TEST PASSED**
（74 个源文件、71 个聚落、103 条道路、23 处遗迹、10 处隐秘）

> 算法自检（`genesis/dev/`）是**故意用窄类路径编译**的：它只许用 `api/` 与 `core/` 层和 JDK。
> 这机械地证明了算法层不依赖游戏。持久化自检（`genesis/mc/dev/`）需要游戏的 NBT 机制，
> 所以它单独一个类、单独一个任务。

---

## 七、已知限制

* 生成器的 `fillFromNoise` 采用**高度图式**填充（而非原版的三维密度函数），因此洞穴由独立的 3D 噪声雕刻，而非原版洞穴算法。
* 结构为**程序化生成的简化建筑**（房屋、围墙、废墟、地下密室），尚未接入原版结构模板系统。
* 创造标签使用 26.2 的新 API（`Row` + 列号），物品通过 NeoForge 事件注入。
* 星球的重力、天空、云与天气目前是**参数化预留**，尚未驱动客户端渲染。
* 魔力浓度已经**持久化**（每维度一份、写 NBT、重启不丢），但还没有绑到游戏玩法：未驱动物品配方、生物强度或附魔。
* F3 调试行显示的是由种子推出的**浓度分布**，不含被施加过的影响（那一行拿不到维度上下文）。
* 魔力器官与引导方式目前是**描述性数据**（类型 + 数值），尚未接入实体与合成表。

---

## 八、许可

MIT
