# 批斗乐事 (Denunciation Delight)

Minecraft 1.20.1 Forge 装饰/整活模组。
包名：`net.qiansiqi.denunciationdelight`

---

## 内容一览

### 1. 白色圆锥高帽 (`White Dunce Cone Hat`)
- **佩戴槽位**：头部（原生 Helmet 槽位，同时支持 Curios 的 `head` 饰品槽位）。
- **合成配方**：3 张纸呈倒 V 形排列。
- **外观与效果**：高耸的白色纸制尖顶帽，随头部动作同步转动。
- **自定义文字**：右键编辑竖排文字与字号；Shift + 右键自戴；右键玩家/村民/NPC 可强行戴上。

### 2. 白色脖挂挂牌 (`White Neck Placard`)
- **佩戴槽位**：胸甲（原生 Chestplate 槽位，同时支持 Curios 的 `body` 和 `necklace` 槽位）。
- **合成配方**：锁链 + 纸 + 任意木板/原木竖排。
- **自定义文字**：三行文字（罪名/姓名/批注）+ 可选大红叉 + 字号调节。
- **交互**：右键编辑，Shift + 右键自戴，右键其他生物可强行挂上。

### 3. 宣传标语横幅 (`Slogan Banner`)
- 1 张纸合成 1 个横幅。
- 贴在墙面上的长条标语；右键编辑文本、底色、字色、字号。
- 相邻同朝向横幅会自动拼接：横向横幅横向排版，纵向横幅自动竖排；最多同步 64 格。

### 4. 壁挂画像 (`Wall Portrait`)
- 8 根木棍 + 1 张纸合成。
- **这是一个通用图片画框，不内置任何人物/领导人画像**；资源里只有默认占位图。
- 右键输入本地图片路径或图片 URL，客户端等比缩放（最大 256 像素）后上传为 PNG。
- 服务端按 SHA-256 短 ID 存入世界目录 `saved_portraits/<id>.png`；其他玩家按需请求并缓存纹理。
- 上传限制：默认最大 2 MB、最大 512×512、每 2 秒一次、需要靠近画像并有交互权限。

### 5. 国家旗帜 (`National Flag`)
- 木棍 + 白色羊毛合成。
- 带金属旗杆与双面旗面；右键打开可搜索的国旗选择界面。
- 内置 253 面旗帜：249 个 ISO 3166-1 国家/地区 + 欧盟、联合国、苏联。
- 支持中文名、英文名、国家代码搜索，分页显示。
- 旧存档的 `china/usa/uk/russia/france/germany` 会自动映射到新的 ISO ID。
- 服务端只接受注册表中的 ID，非法值会回退到中国旗。
- 旗帜贴图来自 [flagcdn.com](https://flagcdn.com/)，统一压缩为 64×40 PNG；来源说明见 `textures/flag/SOURCE.txt`。

### 6. 墨水瓶 (`Ink Bottle`)
- 玻璃瓶 + 墨囊合成。
- 右键投掷，命中生物或方块后对周围 2.5 格范围泼墨。
- 效果：黑色墨水污渍、失明、减速；村民会恐慌/拒绝交易，怪物会丢失目标，玩家疯度 +20。
- 墨水污渍在水中/雨中会产生洗刷粒子（视觉表现）。

### 7. 疯度与自裁系统
- 疯度 0–100；佩戴高帽/挂牌每 30 秒 +10，被墨水瓶直击 +20。
- 疯度 >80：反胃、饱食度下降、心跳/低语警告；进食可小幅降低疯度。
- 疯度到 100：触发自裁过场，按物品栏优先级选择：
  - 有子弹的枪械（支持 TACZ）：饮弹；
  - 冷兵器（原版剑/斧/三叉戟、EpicFight 武器等）：挥刀自刎；
  - 无武器：撞墙。
- 表现使用火焰、烟雾、横扫、伤害指示粒子与红色屏幕反馈；**没有血液贴图或 gore 效果**。
- 该内容为主题向过场，服务器可以通过配置关闭。

### 8. 兼容与附加表现
- **Curios**：高帽/挂牌可放进对应饰品槽，并随玩家模型姿态渲染。
- **EpicFight**：反射识别武器与战斗姿态。
- **TACZ**：通过 API/反射读取枪械与弹药；不装 TACZ 时自动回退。
- 村民、流浪商人、僵尸、盔甲架也支持高帽/挂牌/墨水的部分渲染与交互。

---

## 配置

首次运行会生成 `config/denunciationdelight-common.toml`（`ModConfig.Type.COMMON`）：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `gameplay.enableMadness` | `true` | 开启疯度、佩戴惩罚与高疯度效果 |
| `gameplay.enableSuicide` | `true` | 允许疯度 100 触发自裁过场；关闭后不会再触发 |
| `gameplay.enableInk` | `true` | 开启墨水瓶的失明/减速/污渍/疯度效果 |
| `portraits.allowPortraitUpload` | `true` | 是否允许玩家上传画像 |
| `portraits.maxPortraitSizeKb` | `2048` | 单张 PNG 最大 KiB |

COMMON 配置会在客户端和服务端各自加载；玩法判定以服务端值为准，客户端只读取与本地表现相关的开关。

---

## 项目结构

```
denunciationdelight/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── src/main/
│   ├── java/net/qiansiqi/denunciationdelight/
│   │   ├── DenunciationDelight.java          # 模组入口 + 配置注册
│   │   ├── config/ModConfig.java             # ForgeConfigSpec
│   │   ├── block/                            # 横幅/画像/旗帜方块
│   │   ├── block/entity/                     # 对应 BlockEntity
│   │   ├── item/                             # 高帽、挂牌、墨水瓶
│   │   ├── entity/                           # 投掷墨水瓶
│   │   ├── effect/                           # 泼墨效果
│   │   ├── event/                            # 通用交互事件
│   │   ├── flag/                             # 国旗注册表、中文/英文名
│   │   ├── madness/                          # 疯度、自裁、伤害来源
│   │   ├── network/                          # 10 个网络包
│   │   ├── portrait/                         # 服务端画像存储
│   │   ├── compat/curios/                    # Curios 通用能力（common）
│   │   ├── compat/epicfight/                 # EpicFight 反射兼容
│   │   └── client/
│   │       ├── ClientSetup.java
│   │       ├── ClientItemHooks.java           # 物品客户端逻辑隔离
│   │       ├── BlockClientInteractions.java   # 方块 GUI 客户端入口
│   │       ├── ClientPortraitCache.java       # 画像纹理与请求缓存
│   │       ├── compat/curios/                 # Curios 客户端渲染
│   │       ├── gui/                           # 5 个编辑/选择界面
│   │       ├── model/                         # 高帽/挂牌 3D 模型
│   │       └── renderer/                      # 护甲/方块/文字/墨渍渲染
│   └── resources/
│       ├── META-INF/mods.toml
│       ├── pack.mcmeta
│       ├── assets/denunciationdelight/        # 模型、贴图、flags.json、中英文语言文件
│       └── data/denunciationdelight/          # 配方
```

---

## 开发与构建

- **JDK 17**（项目声明 Java 17 toolchain；Gradle 本身可用 JDK 21 运行）
- IntelliJ IDEA 打开本目录后：
  - `fg_runs -> runClient` 启动客户端；
  - `fg_runs -> runServer` 启动专用服务器；
  - `build -> build` 在 `build/libs` 生成发布 jar。

依赖：
- Curios 通过 Maven 依赖 `curios-forge:5.14.1+1.20.1`。
- TACZ 通过反射读取 `IGun`，构建不再硬依赖 69 MB 的枪械 jar；运行时把 TACZ 放进 `mods/` 即可启用兼容。
- `libs/tacz-1.20.1-1.2.0.jar` 仅保留为本地测试用的可选文件，不参与构建。

---

## 自动构建与发布

仓库已配置 GitHub Actions（`.github/workflows/gradle-publish.yml`）：

- push 到 `main` / PR / 手动运行：构建并上传 `build/libs/*.jar` 到 Actions Artifacts；
- push 形如 `v1.0.0` 的 tag：构建后自动创建 GitHub Release，并把 jar 挂到 Release 资产。

发布新版本：

```bash
git tag v1.0.0
git push origin v1.0.0
```

## 已知限制

- 画像/旗帜的 ID 与文本长度、字号、上传大小、上传频率在服务端校验；客户端退出服务器时会释放画像纹理。
- 画像上传仍属于玩家自定义内容，公开服务器建议通过 `allowPortraitUpload` 或权限插件控制。
- 已加入少量 JUnit 校验（画像 ID/路径穿越/PNG 头）；游戏内行为仍建议跑一次 `runClient`，并破坏/放置一次横幅验证 NBT 掉落。