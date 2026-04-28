# DLRS-GAS For Minecraft

<p align="center">
  <b>一个为 Minecraft 服务器提供 DLRS-GAS 账号系统集成的高性能插件</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-1.0.3--7-blue.svg" alt="Version">
  <img src="https://img.shields.io/badge/Minecraft-1.21+-brightgreen.svg" alt="Minecraft">
  <img src="https://img.shields.io/badge/Paper-1.21.8-orange.svg" alt="Paper">
  <img src="https://img.shields.io/badge/Kotlin-2.3.21-purple.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License">
</p>

---

## 📋 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [系统要求](#系统要求)
- [安装指南](#安装指南)
- [配置说明](#配置说明)
- [命令列表](#命令列表)
- [功能详解](#功能详解)
- [开发指南](#开发指南)
- [常见问题](#常见问题)
- [许可证](#许可证)

---

## 🎯 项目简介

**DLRS-GAS For Minecraft** 是一个专为 Minecraft Paper 服务器设计的高级账号系统集成插件。它通过 DLRS-GAS 平台实现了完整的 OAuth 2.0 登录验证、自动登录、双重密码保护等功能，为服务器提供安全、便捷的账号管理解决方案。

### 主要特点

- 🔐 **OAuth 2.0 登录**：集成 DLRS-GAS 官方认证系统
- 🚀 **自动登录**：基于 Access Token 的快速登录机制
- 🛡️ **双重密码保护**：额外的账号安全层
- 🎨 **自定义 Tab 列表**：支持占位符的动态 Tab 列表显示
- 🔧 **玩家锁定系统**：未登录玩家的行为限制
- 📊 **账号绑定管理**：GAS 账号与 Minecraft 账号绑定
-  **维护模式检查**：远程服务器维护状态检测

---

## ✨ 核心功能

### 1. 账号认证系统

#### OAuth 登录流程
```
玩家输入 /gas login
    ↓
插件获取 OAuth Token
    ↓
玩家浏览器打开授权链接
    ↓
玩家在 GAS 平台完成登录
    ↓
插件轮询验证登录状态
    ↓
获取 Access Token 和用户信息
    ↓
验证双重密码（如果已设置）
    ↓
完成登录，解锁玩家
```

#### 自动登录机制
- 玩家首次登录后，Access Token 保存在本地数据库
- 下次加入服务器时自动尝试验证 Token
- Token 有效则自动登录，无效则提示手动登录
- 自动登录成功后仍需验证双重密码

### 2. 双重密码保护

双重密码是登录后的第二层安全保护：

- **设置密码**：`/gas double-password set <密码>`
- **验证密码**：`/gas double-password verify <密码>` 或 `/login <密码>`
- **修改密码**：`/gas double-password change <新密码>`
- **移除密码**：`/gas double-password remove`（仅 OP）
- **查看状态**：`/gas double-password status`

**密码要求**：
- 长度：4-16 位
- 字符：仅限数字和字母（a-z, A-Z, 0-9）
- 加密：SHA-256 + 随机盐值

### 3. 玩家锁定系统

未登录或未验证双重密码的玩家会被锁定：

**锁定效果**：
- 🌑 **失明效果**：无法看清周围环境
-  **完全减速**：Slowness VI (等级 255)，无法移动
- 🦘 **禁止跳跃**：Jump Boost 负等级，无法跳跃
- 🎮 **冒险模式**：无法破坏或放置方块
- 🛡️ **无敌状态**：免疫所有伤害

**解锁条件**：
- 完成 OAuth 登录
- 验证双重密码（如果已设置）

### 4. Tab 列表自定义

支持动态占位符的 Tab 列表显示：

**可用占位符**：
- `%player%` - 玩家游戏 ID
- `%gas_nickname%` - GAS 昵称
- `%ping%` - 网络延迟
- `%tps%` - 服务器 TPS

**示例配置**：
```yaml
tab-list:
  enabled: true
  header: |
    &e&lDLRS-GAS 服务器
    &7欢迎回来，%player%
  footer: |
    &7GAS 昵称：%gas_nickname%
    &7延迟：%ping%ms | TPS: %tps%
```

### 5. 权限管理

根据 GAS 用户组自动设置权限：

- **用户组 104**：自动授予 OP 权限
- **其他用户组**：取消 OP 权限
- **动态更新**：登录时自动检测并应用

### 6. 账号绑定管理

防止 GAS 账号被多人使用：

- 每个 GAS UID 只能绑定一个 Minecraft UUID
- 绑定冲突时拒绝登录并踢出玩家
- OP 可执行解绑操作：`/gas unbind <UID>`
- 查看绑定状态：`/gas bind`

### 7. 维护模式检查

支持远程维护状态检测：

- 玩家加入时自动检查服务器维护状态
- 维护中则拒绝玩家加入并显示自定义消息
- 可在配置中启用/禁用此功能

---

## 💻 系统要求

### 运行环境

| 组件 | 最低要求 | 推荐配置 |
|------|---------|---------|
| **Minecraft** | 1.21+ | 1.21.8 |
| **服务端** | Paper | Paper 1.21.8 (build 26.1.2+) |
| **Java** | 21+ | Java 21 LTS |
| **内存** | 1 GB | 2 GB+ |

### 依赖项

插件已内置以下依赖（通过 Maven Shade 打包）：
- Kotlin 2.3.21
- JSON 20231013
- SQLite JDBC 3.49.1.0

### 前置要求

- DLRS-GAS 开发者账号
- 有效的 App ID 和 App Token
- 可访问 `api.chinadlrs.com`

---

## 📦 安装指南

### 1. 下载插件

```bash
# 从 Releases 页面下载最新版本的 JAR 文件
# 或自行编译
git clone https://github.com/your-repo/DLRS-GAS-For-Minecraft.git
cd DLRS-GAS-For-Minecraft
mvn clean package
```

### 2. 安装到服务器

```bash
# 将 JAR 文件复制到服务器 plugins 目录
cp DLRS-GAS-For-Minecraft-1.0.3-7.jar /path/to/server/plugins/

# 重启服务器
./start.sh
```

### 3. 配置插件

1. 首次启动后，插件会自动生成配置文件
2. 编辑 `plugins/DLRS-GAS-For-Minecraft/config.yml`
3. 填写 DLRS 应用信息

```yaml
dlrs:
  app-id: "your-app-id-here"
  app-token: "your-app-token-here"
  language: "zh"  # zh 或 en
```

4. 重载配置：`/gas reload`（需要 OP 权限）

### 4. 验证安装

```bash
# 在游戏中执行
/gas status

# 控制台查看日志
# 应看到：[DLRS-GAS] DLRS-GAS For Minecraft 插件已启用!
```

---

## ⚙️ 配置说明

### 完整配置文件

```yaml
# DLRS-GAS For Minecraft 配置文件
# 请根据实际情况修改以下配置

dlrs:
  # DLRS 应用 ID (从 DLRS 开发者后台获取)
  app-id: "your-app-id"
  
  # DLRS 应用 Token (从 DLRS 开发者后台获取，请妥善保管)
  app-token: "your-app-token"
  
  # 语言设置 (en=英文，zh=中文)
  language: "zh"

# Tab 列表配置
tab-list:
  # 是否启用 Tab 列表自定义
  enabled: true
  
  # Tab 列表顶部显示内容（支持颜色代码和占位符）
  header: |
    &e&lDLRS-GAS 服务器
    &7欢迎回来，%player%
  
  # Tab 列表底部显示内容（支持颜色代码和占位符）
  footer: |
    &7GAS 昵称：%gas_nickname%
    &7延迟：%ping%ms | TPS: %tps%

# 登录超时配置
login-timeout:
  # 是否启用登录超时检测
  enabled: true
  # 超时时间（秒），默认 60 秒
  timeout-seconds: 60

# 服务器维护状态检查配置
maintenance:
  # 是否启用维护状态检查（在玩家加入时检查）
  enabled: false
  # 自定义维护消息（当 API 返回维护状态时显示）
  custom-message: |
    &c[DLRS-GAS] 服务器正在维护中
    &e请稍后再试！

# 玩家数据存储
# 玩家登录信息将自动存储在 players 节点下
# 无需手动修改
players: {}
```

### 配置项说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `dlrs.app-id` | String | `""` | DLRS 应用 ID，必填 |
| `dlrs.app-token` | String | `""` | DLRS 应用 Token，必填 |
| `dlrs.language` | String | `"en"` | 界面语言，`zh` 或 `en` |
| `tab-list.enabled` | Boolean | `true` | 是否启用 Tab 列表自定义 |
| `tab-list.header` | String | - | Tab 列表顶部内容，支持多行 |
| `tab-list.footer` | String | - | Tab 列表底部内容，支持多行 |
| `login-timeout.enabled` | Boolean | `true` | 是否启用登录超时检测 |
| `login-timeout.timeout-seconds` | Integer | `60` | 登录超时时间（秒） |
| `maintenance.enabled` | Boolean | `false` | 是否启用维护状态检查 |
| `maintenance.custom-message` | String | - | 维护时的自定义消息 |

---

## 🎮 命令列表

### 基础命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/gas` | 显示简要提示 | 所有人 |
| `/gas help` | 显示完整命令帮助 | 所有人 |
| `/gas login` | 开始 OAuth 登录流程 | 所有人 |
| `/gas logout` | 登出当前账号 | 所有人 |
| `/gas status` | 查看登录状态 | 所有人 |
| `/gas info` | 查看详细账号信息 | 所有人 |
| `/gas redeem <兑换码>` | 兑换 DLRS 兑换码 | 所有人 |

### 简化登录命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/login <密码>` | 快速验证双重密码 | 所有人 |

### 双重密码命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/gas double-password set <密码>` | 设置双重密码（4-16位数字或字母） | 所有人 |
| `/gas double-password verify <密码>` | 验证双重密码 | 所有人 |
| `/gas double-password change <新密码>` | 修改双重密码 | 所有人 |
| `/gas double-password remove` | 移除双重密码 | OP |
| `/gas double-password status` | 查看双重密码设置状态 | 所有人 |

### 管理命令（需要 OP 权限）

| 命令 | 描述 | 权限 |
|------|------|------|
| `/gas reload` | 重载插件配置 | OP |
| `/gas kickall` | 踢出所有玩家 | OP |
| `/gas logoutall` | 登出所有已登录的 GAS 账号 | OP |
| `/gas unbind <玩家/UID>` | 解绑 GAS 账号 | OP |
| `/gas bind` | 查看绑定状态 | OP |

### 命令别名

- `/gasl`
- `/dlrs`
- `/dlrsgas`

所有别名与 `/gas` 命令功能完全相同。

---

## 🔍 功能详解

### OAuth 登录流程

#### 1. 启动登录
```
/gas login
```

插件执行以下操作：
1. 向 DLRS API 请求 OAuth Token
2. 生成授权链接并发送给玩家
3. 开始在后台轮询验证登录状态

#### 2. 完成授权
玩家在浏览器中：
1. 打开授权链接
2. 使用 GAS 账号登录
3. 授权应用访问权限

#### 3. 自动验证
插件自动：
1. 每 5 秒检查一次登录状态
2. 检测到登录成功后获取 Access Token
3. 获取用户详细信息（昵称、UID、用户组等）
4. 保存数据到本地 SQLite 数据库
5. 检查账号绑定情况
6. 提示验证双重密码（如果已设置）

#### 4. 超时处理
- 默认超时时间：60 秒
- 超时后自动踢出玩家
- 可在配置中调整超时时间

### 自动登录流程

#### 首次登录
1. 玩家通过 OAuth 登录
2. Access Token 保存到本地数据库
3. 设置双重密码（可选）

#### 后续加入
1. 玩家加入服务器
2. 插件检查是否有保存的 Token
3. 向 DLRS API 验证 Token 有效性
4. 有效则自动登录，无效则提示手动登录
5. 自动登录成功后仍需验证双重密码

### 双重密码机制

#### 为什么需要双重密码？

DLRS-GAS 是跨平台的通用账号系统，而双重密码提供了：
- **服务器级保护**：即使 GAS 账号泄露，攻击者仍需知道双重密码
- **快速验证**：无需每次打开浏览器登录
- **灵活性**：可随时修改或移除

#### 工作流程

```
玩家加入服务器
    ↓
自动登录成功（或手动 OAuth 登录）
    ↓
检查是否设置双重密码
    ↓
未设置 → 提示设置（可选）
已设置 → 要求验证
    ↓
验证成功 → 解锁玩家
验证失败 → 保持锁定
```

#### 安全特性

- **加密存储**：SHA-256 + 随机盐值
- **本地存储**：SQLite 数据库，不上传服务器
- **格式限制**：仅允许数字和字母，防止注入攻击
- **OP 管理**：服务器管理员可移除忘记的密码

### 玩家锁定系统

#### 锁定条件

玩家会被锁定在以下情况：
1. 首次加入服务器，未登录
2. OAuth 登录超时
3. Access Token 过期，自动登录失败
4. 已登录但未验证双重密码

#### 锁定效果详解

| 效果 | 药水类型 | 等级 | 作用 |
|------|---------|------|------|
| 失明 | Blindness | 0 | 无法看清环境 |
| 减速 | Slowness | 255 (VI) | 完全无法移动 |
| 禁跳 | Jump Boost | -4 | 无法跳跃 |
| 模式 | Adventure | - | 无法破坏/放置方块 |
| 无敌 | Invulnerable | true | 免疫所有伤害 |

#### 解锁流程

```
玩家处于锁定状态
    ↓
完成 OAuth 登录
    ↓
如果设置了双重密码
    ↓
    验证双重密码
    ↓
验证成功 → 移除所有限制效果
    ↓
恢复原有游戏模式
    ↓
正常游戏
```

### Tab 列表自定义

#### 占位符说明

| 占位符 | 说明 | 示例 |
|--------|------|------|
| `%player%` | 玩家游戏 ID | `EroSushi_Meow` |
| `%gas_nickname%` | GAS 昵称 | `筱夕Sushi` |
| `%ping%` | 网络延迟（毫秒） | `45` |
| `%tps%` | 服务器 TPS | `19.8` |

#### 颜色代码

使用 Minecraft 标准颜色代码：
- `&0` - `&9`：黑色到蓝色
- `&a` - `&f`：绿色到白色
- `&l`：粗体
- `&o`：斜体
- `&n`：下划线
- `&m`：删除线

#### 刷新机制

- 每 5 秒自动刷新所有在线玩家的 Tab 列表
- 玩家登录/登出时立即刷新
- 性能优化：仅在有变化时更新

### 账号绑定管理

#### 绑定规则

- 一个 GAS UID 只能绑定一个 Minecraft UUID
- 绑定后无法更改（除非 OP 手动解绑）
- 防止账号共享和盗用

#### 冲突处理

```
玩家 A 使用 GAS UID: 12345 绑定 Minecraft UUID: AAA
    ↓
玩家 B 尝试使用 GAS UID: 12345 登录
    ↓
系统检测到绑定冲突
    ↓
拒绝登录并踢出玩家 B
    ↓
显示错误信息：该 GAS 账号已绑定其他玩家
```

#### 管理命令

```bash
# 查看绑定状态
/gas bind

# 解绑指定 UID（需要 OP）
/gas unbind 12345

# 解绑指定玩家（需要 OP）
/gas unbind PlayerName
```

### 维护模式检查

#### 工作原理

1. 玩家尝试加入服务器
2. 插件同步检查 DLRS 服务器维护状态
3. 如果正在维护：
   - 拒绝玩家加入
   - 显示自定义维护消息
   - 不显示加入消息
4. 如果正常运行：
   - 继续正常登录流程

#### 配置示例

```yaml
maintenance:
  enabled: true
  custom-message: |
    &c[DLRS-GAS] 服务器正在维护中
    &7预计恢复时间：2024-01-01 12:00
    &e请稍后再试！
```

---

## 💡 使用场景

### 场景 1：新玩家首次加入

```
玩家加入服务器
    ↓
显示：[DLRS-GAS] 您尚未登录，请使用 /gas login 进行登录
    ↓
玩家执行：/gas login
    ↓
显示：请在浏览器中打开以下链接完成登录...
    ↓
玩家在 GAS 平台完成登录
    ↓
显示：登录成功！欢迎，筱夕Sushi!
    ↓
提示：请设置双重密码来保护您的账号
    ↓
玩家设置双重密码
    ↓
解锁并正常游戏
```

### 场景 2：老玩家自动登录

```
玩家加入服务器
    ↓
显示：正在尝试自动登录...
    ↓
显示：自动登录成功！欢迎回来，筱夕Sushi!
    ↓
提示：请验证您的双重密码
    ↓
玩家执行：/login 1234
    ↓
显示：双重密码验证成功！您现在可以正常游戏了
    ↓
解锁并正常游戏
```

### 场景 3：Token 过期处理

```
玩家加入服务器
    ↓
显示：正在尝试自动登录...
    ↓
显示：自动登录失败，Token 已过期
    ↓
显示：请使用 /gas login 重新登录
    ↓
玩家被锁定
    ↓
玩家执行：/gas login
    ↓
重新完成 OAuth 流程
```

### 场景 4：账号绑定冲突

```
玩家 A 已绑定 GAS UID: 12345
    ↓
玩家 B 尝试登录同一个 GAS 账号
    ↓
系统检测到冲突
    ↓
玩家 B 被踢出
    ↓
显示：该 GAS 账号已绑定其他 Minecraft 玩家
    ↓
联系管理员解绑
```

---

## 🛠️ 开发指南

### 项目结构

```
DLRS-GAS-For-Minecraft/
├── src/
│   └── main/
│       ├── kotlin/com/sushi/dLRSGASForMinecraft/
│       │   ├── command/
│       │   │   └── DLRSCommandHandler.kt       # 命令处理器
│       │   ├── config/
│       │   │   ── DLRSConfig.kt               # 配置管理
│       │   ├── listener/
│       │   │   ├── PlayerCommandInterceptor.kt # 命令拦截器
│       │   │   ── PlayerLockListener.kt       # 锁定监听器
│       │   ├── model/
│       │   │   └── UserInfo.kt                 # 用户数据模型
│       │   ├── service/
│       │   │   ├── DLRSAutoLoginService.kt     # 自动登录服务
│       │   │   ├── DLRSLoginService.kt         # OAuth 登录服务
│       │   │   ├── DoublePasswordService.kt    # 双重密码服务
│       │   │   ├── MaintenanceService.kt       # 维护检查服务
│       │   │   ├── PlayerDataService.kt        # 玩家数据服务
│       │   │   ├── PlayerLockService.kt        # 玩家锁定服务
│       │   │   └── TabListService.kt           # Tab 列表服务
│       │   ├── util/
│       │   │   ├── AESEncryptor.kt             # AES 加密工具
│       │   │   └── HttpUtil.kt                 # HTTP 请求工具
│       │   ├── DLRSGASForMinecraft.kt          # 主类
│       │   └── DLRSGASForMinecraftBootstrap.kt # 引导类
│       └── resources/
│           ├── config.yml                      # 配置文件模板
│           └── paper-plugin.yml                # Paper 插件描述
├── pom.xml                                     # Maven 配置
── README.md                                   # 项目文档
```

### 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.3.21 | 主要编程语言 |
| Paper API | 1.21.8 | Minecraft 服务端 API |
| Maven | 3.x | 项目构建管理 |
| SQLite | 3.49.1.0 | 本地数据存储 |
| JSON | 20231013 | HTTP 请求/响应解析 |

### 构建项目

```bash
# 克隆项目
git clone https://github.com/your-repo/DLRS-GAS-For-Minecraft.git

# 进入项目目录
cd DLRS-GAS-For-Minecraft

# 编译项目
mvn clean package

# 编译后的 JAR 文件位置
target/DLRS-GAS-For-Minecraft-1.0.3-7.jar
```

### 核心 API

#### 获取服务实例

```kotlin
// 在主类中获取各种服务实例
val plugin = DLRSGASForMinecraft.instance
val dataService = plugin.getDataService()
val tabListService = plugin.getTabListService()
val config = plugin.getConfigManager()
```

#### 玩家数据操作

```kotlin
// 保存玩家信息
dataService.saveUserInfo(playerUUID, userInfo)

// 获取玩家信息
val userInfo = dataService.getPlayerInfo(playerUUID)

// 检查登录状态
val isLoggedIn = dataService.isLoggedIn(playerUUID)

// 清除玩家数据
dataService.clearPlayerData(playerUUID)
```

#### 玩家锁定控制

```kotlin
// 锁定玩家（带消息）
lockService.lockPlayer(player, sendMessage = true)

// 锁定玩家（不带消息）
lockService.lockPlayer(player, sendMessage = false)

// 解锁玩家
lockService.unlockPlayer(player)

// 检查锁定状态
val isLocked = lockService.isLocked(player)
```

### 扩展开发

#### 添加新命令

1. 在 `DLRSCommandHandler.kt` 中添加命令处理逻辑
2. 在 `paper-plugin.yml` 中注册命令（如需要）
3. 更新帮助信息

#### 添加新占位符

1. 在 `TabListService.kt` 中添加占位符替换逻辑
2. 在 README 中更新占位符文档

#### 自定义锁定效果

修改 `PlayerLockService.kt` 中的 `lockPlayer` 方法：

```kotlin
fun lockPlayer(player: Player, sendMessage: Boolean = true) {
    // 添加自定义药水效果
    player.addPotionEffect(PotionEffect(...))
    
    // 添加自定义限制
    player.allowFlight = false
}
```

---

## ❓ 常见问题

### Q1: 插件启动后提示 App ID 或 Token 无效？

**A**: 请检查：
1. 是否正确填写了 `config.yml` 中的 `app-id` 和 `app-token`
2. 这些信息可以从 DLRS 开发者后台获取
3. 确保服务器可以访问 `api.chinadlrs.com`

### Q2: 自动登录失败怎么办？

**A**: 可能原因：
1. Access Token 已过期（默认有效期未知，取决于 DLRS 设置）
2. 网络连接问题
3. 解决方案：执行 `/gas login` 重新登录

### Q3: 忘记了双重密码怎么办？

**A**: 
1. 联系服务器管理员
2. 管理员使用 `/gas double-password remove` 移除密码
3. 重新设置新密码

### Q4: 如何禁用双重密码功能？

**A**: 
- 双重密码是可选的，玩家可以不设置
- 未设置双重密码的玩家在 OAuth 登录后直接解锁
- 无法全局禁用，因为这是核心安全功能

### Q5: Tab 列表占位符不显示？

**A**: 检查：
1. `config.yml` 中 `tab-list.enabled` 是否为 `true`
2. 占位符拼写是否正确
3. 玩家是否已登录（未登录玩家部分占位符可能无效）

### Q6: 玩家被踢出提示"账号已绑定"？

**A**: 
1. 该 GAS 账号已被其他 Minecraft 账号绑定
2. 如果是误操作，联系管理员使用 `/gas unbind <UID>` 解绑
3. 如果是账号被盗，联系 GAS 客服

### Q7: 如何查看服务器维护状态？

**A**: 
1. 确保 `config.yml` 中 `maintenance.enabled` 为 `true`
2. 维护状态由 DLRS 平台控制
3. 维护时会显示自定义消息并拒绝玩家加入

### Q8: 插件与其他权限插件冲突？

**A**: 
- 本插件仅处理 OP 权限（用户组 104）
- 不影响其他权限插件的权限组
- 如有冲突，检查权限插件的优先级设置

### Q9: 如何备份玩家数据？

**A**: 
- 玩家数据存储在 `plugins/DLRS-GAS-For-Minecraft/` 目录
- 定期备份以下文件：
  - `config.yml`（包含玩家登录信息）
  - `double_password.db`（双重密码数据库）
- 建议使用服务器自动备份脚本

### Q10: 支持 BungeeCord/Velocity 吗？

**A**: 
- 当前版本仅支持单服（Paper/Spigot）
- BungeeCord/Velocity 版本正在开发中
- 关注 Releases 页面获取更新

---

## 📊 性能优化

### 数据库优化

- 使用 SQLite 本地存储，减少网络延迟
- 玩家数据缓存在内存中，减少磁盘 I/O
- 异步执行数据库操作，避免阻塞主线程

### 网络优化

- HTTP 请求使用异步线程
- 连接池复用，减少握手开销
- 超时设置合理，避免长时间等待

### 事件优化

- 使用 `EventPriority.LOWEST` 优先处理登录逻辑
- 减少事件监听器数量
- 避免在事件处理中执行耗时操作

---

##  安全建议

### 服务器端

1. **保护配置文件**：确保 `config.yml` 的 App Token 不被泄露
2. **定期备份**：备份玩家数据和配置文件
3. **权限控制**：限制管理命令的 OP 权限
4. **日志监控**：定期检查服务器日志，发现异常登录

### 玩家端

1. **设置双重密码**：强烈建议所有玩家设置双重密码
2. **保护 GAS 账号**：不要在公共场合泄露 GAS 账号信息
3. **定期修改密码**：建议定期修改双重密码
4. **警惕钓鱼**：只在官方 GAS 平台登录，不点击可疑链接

---

## 📝 更新日志

### v1.0.3-7 (当前版本)

#### ✨ 新增功能
- 完整的 OAuth 2.0 登录流程
- 基于 Access Token 的自动登录机制
- 双重密码保护系统
- 自定义 Tab 列表显示
- 玩家锁定与解锁系统
- 账号绑定管理
- 维护状态检查
- 命令拦截器（阻止未登录玩家执行命令）

#### 🐛 修复问题
- 修复异步线程安全问题（Bukkit API 在主线程调用）
- 修复自动登录成功后显示误导性消息的问题
- 优化 Tab 列表刷新性能
- 改进错误提示信息

#### 🔧 优化改进
- 使用 Kotlin 2.3.21 提升代码质量
- 优化数据库查询性能
- 改进配置加载逻辑
- 增强日志输出

---

### v1.0.2

- 初步实现 OAuth 登录
- 添加基础锁定系统
- 支持 Tab 列表自定义

---

### v1.0.1

- 修复命令注册问题
- 改进错误处理
- 优化配置结构

---

### v1.0.0

- 首次发布
- 基础登录功能
- SQLite 数据存储

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

### 贡献流程

1. **Fork 项目**
   ```bash
   git fork https://github.com/your-repo/DLRS-GAS-For-Minecraft.git
   ```

2. **创建分支**
   ```bash
   git checkout -b feature/your-feature
   ```

3. **提交更改**
   ```bash
   git commit -m "feat: 添加新功能"
   ```

4. **推送到分支**
   ```bash
   git push origin feature/your-feature
   ```

5. **创建 Pull Request**

### 代码规范

- 使用 Kotlin 编码规范
- 添加必要的注释
- 遵循现有代码风格
- 测试新功能

### 报告问题

使用 GitHub Issues 报告问题，请包含：
- 问题描述
- 复现步骤
- 预期行为
- 实际行为
- 服务器版本和插件版本
- 相关日志

---

## 📜 许可证

本项目采用 **MIT License** 开源许可证。

```
MIT License

Copyright (c) 2024 Sushi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📞 联系方式

- **项目地址**：https://github.com/your-repo/DLRS-GAS-For-Minecraft
- **问题反馈**：https://github.com/your-repo/DLRS-GAS-For-Minecraft/issues
- **DLRS 官网**：https://gas.chinadlrs.com
- **DLRS API 文档**：https://api.chinadlrs.com/developer

---

##  致谢

- **DLRS-GAS 团队**：提供优秀的账号系统平台
- **PaperMC 团队**：开发高性能的 Minecraft 服务端
- **Kotlin 团队**：提供优雅的编程语言

---

<p align="center">
  <b>如果觉得这个项目对你有帮助，请给一个 ⭐ Star！</b>
</p>

<p align="center">
  Made ❤️ by Sushi
</p>
