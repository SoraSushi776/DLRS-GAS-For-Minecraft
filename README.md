# DLRS-GAS For Minecraft

一个 Minecraft Paper 插件，用于集成 DLRS GAS账号系统。

## 功能特性

- ✅ OAuth 2.0 授权登录
- ✅ 自动登录支持（使用 access_token）
- ✅ AES-256-CBC 加密保护应用密钥
- ✅ 玩家数据持久化存储（SQLite）
- ✅ 完整的命令系统
- ✅ 玩家加入时自动登录
- ✅ 中文消息输出
- ✅ **账号绑定系统**（GAS 账号与 Minecraft 玩家一一绑定）
- ✅ **Tab 列表自定义**（支持占位符显示 GAS 信息）
- ✅ **配置热重载**（无需重启服务器）
- ✅ **OP 管理命令**（踢出所有玩家、绑定查询/解绑）

## 安装步骤

1. 将编译好的 JAR 文件放入服务器的 `plugins` 文件夹
2. 启动服务器以生成配置文件
3. 编辑 `plugins/DLRS-GAS-For-Minecraft/config.yml` 文件
4. 配置您的 DLRS 应用 ID 和应用 Token
5. 重启服务器

## 配置说明

在 `config.yml` 中配置以下参数：

```yaml
dlrs:
  # DLRS 应用 ID (从 DLRS 开发者后台获取)
  app-id: "1"

  # DLRS 应用 Token (从 DLRS 开发者后台获取，请妥善保管)
  app-token: "YOUR_APP_TOKEN"

  # 语言设置 (en=英文，zh=中文)
  language: "en"

# Tab 列表自定义配置
tab-list:
  enabled: true
  header: "&e&lDLRS-GAS 服务器\n&7欢迎回来，%player%"
  footer: "&7GAS 昵称：%gas_nickname%\n&7GAS UID: %gas_uid%\n&7延迟：%ping%ms | TPS: %tps%"

# 登录超时配置
login-timeout:
  # 是否启用登录超时检测
  enabled: true
  # 超时时间（秒），默认 60 秒
  timeout-seconds: 60
```

**重要**: 您需要从 DLRS 开发者后台获取 `app-id` 和 `app-token` 才能使用此插件。

### Tab 列表占位符

| 占位符 | 说明 |
|--------|------|
| `%player%` | 玩家游戏名 |
| `%player_uuid%` | 玩家 UUID |
| `%ping%` | 玩家延迟 |
| `%tps%` | 服务器 TPS |
| `%gas_nickname%` | GAS 昵称 |
| `%gas_uid%` | GAS 用户 ID |
| `%gas_email%` | GAS 邮箱 |

## 命令列表

| 命令 | 权限 | 描述 |
|------|------|------|
| `/dlrs login` | 所有玩家 | 开始 DLRS 账号登录流程 |
| `/dlrs logout` | 所有玩家 | 登出 DLRS 账号 |
| `/dlrs status` | 所有玩家 | 查看当前登录状态 |
| `/dlrs info` | 所有玩家 | 查看账号详细信息 |
| `/dlrs reload` | OP | 热重载插件配置 |
| `/dlrs kickall` | OP | 踢出所有在线玩家 |
| `/dlrs bind [玩家/UID]` | OP | 查看绑定状态 |
| `/dlrs unbind [玩家/UID]` | OP | 解绑 GAS 账号 |

### 命令别名
- `/dlrsgas`
- `/dlrsl`

## 使用流程

### 首次登录

1. 在游戏中输入 `/dlrs login`
2. 插件会显示一个授权链接
3. 在浏览器中打开该链接
4. 在网页上完成 DLRS 账号登录和授权
5. 返回游戏，插件会自动验证登录状态
6. 登录成功后会显示欢迎信息

### 自动登录

- 玩家首次登录成功后，access_token 会被保存
- 下次加入服务器时，插件会自动尝试验证 token 并登录
- 如果 token 过期，玩家需要重新执行 `/dlrs login`

### 查看信息

使用 `/dlrs info` 可以查看：
- 用户 ID (uid)
- 昵称 (nickname)
- 邮箱 (email)
- 用户组 (user_group)
- 头像 URL (avatar_url)

## 账号绑定系统

### 绑定规则

每个 GAS 账号只能绑定一个 Minecraft 玩家，防止账号共享。

| 情况 | 处理方式 |
|------|----------|
| UID 未绑定，玩家未绑定 | 自动建立新绑定 |
| UID 已绑定到当前玩家 | 允许登录，昵称变化则自动更新 |
| UID 已绑定到其他玩家 | 踢出玩家，显示绑定冲突信息 |
| UID 未绑定，玩家已绑定其他 UID | 允许重新绑定 |

### 绑定冲突提示

当检测到账号绑定时，未绑定的玩家会被踢出并看到以下提示：

```
该 GAS 账号已绑定至其他玩家
绑定的玩家：Steve
你的玩家 ID: Alex
每个 GAS 账号只能绑定一个玩家，无法重新注册
```

### OP 管理命令

**查看绑定状态**
```
# 查看自己的绑定状态
/dlrs bind

# 查看指定玩家的绑定状态
/dlrs bind Steve

# 查看指定 UID 的绑定状态
/dlrs bind 12345
```

**解绑账号**
```
# 解绑自己的账号
/dlrs unbind

# 解绑指定玩家的账号
/dlrs unbind Steve

# 解绑指定 UID 的账号
/dlrs unbind 12345
```

## 技术实现

### 架构组件

- **AESEncryptor**: AES-256-CBC 加密工具，用于加密 appToken
- **HttpUtil**: HTTP 请求工具，与 DLRS API 通信
- **DLRSConfig**: 配置管理类
- **DLRSLoginService**: OAuth 登录服务，处理完整登录流程
- **DLRSAutoLoginService**: 自动登录服务，验证 access_token
- **PlayerDataService**: SQLite 数据服务，管理玩家数据和绑定关系
- **DLRSCommandHandler**: 命令处理器
- **TabListService**: Tab 列表管理服务

### API 端点

插件使用以下 DLRS API 端点：

- `https://api.chinadlrs.com/developer/oauth.php` - OAuth 认证
- `https://api.chinadlrs.com/developer/profile.php` - 获取用户信息
- `https://api.chinadlrs.com/developer/auto-login.php` - 自动登录验证
- `https://gas.chinadlrs.com/oauth` - OAuth 授权页面

### 数据存储

玩家登录信息存储在 SQLite 数据库中：

**players 表** - 玩家账号数据
```
uuid TEXT PRIMARY KEY
uid TEXT
nickname TEXT
email TEXT
access_token TEXT
avatar_url TEXT
user_group TEXT
last_login TIMESTAMP
```

**bindings 表** - 账号绑定关系
```
uid TEXT PRIMARY KEY
player_uuid TEXT
player_name TEXT
bound_at TIMESTAMP
```

## 安全注意事项

1. **保护 appToken**: 不要在公开场合泄露您的 appToken
2. **HTTPS 通信**: 所有 API 请求都使用 HTTPS 加密
3. **Token 管理**: access_token 本地存储，定期验证有效性
4. **加密传输**: appToken 在传输前使用 AES-256-CBC 加密
5. **账号绑定**: 防止 GAS 账号被多个玩家共享使用

## 常见问题

### Q: 登录后没有反应？
A: 确保您已在浏览器中完成了授权流程，并且网络连接正常。

### Q: 自动登录失败怎么办？
A: Token 可能已过期，请使用 `/dlrs logout` 然后重新登录。

### Q: 如何更改配置？
A: 编辑 `config.yml` 后，使用 `/dlrs reload` 命令或重启服务器。

### Q: 支持哪些 Minecraft 版本？
A: 本插件基于 Paper API 1.21.8+ 构建，适用于 1.21.8 及以上版本。

### Q: 玩家提示"账号已绑定至其他玩家"怎么办？
A: 这说明该 GAS 账号已在其他玩家处绑定。如需更换绑定，请使用 OP 命令 `/dlrs unbind` 先解绑。

### Q: Tab 列表占位符不显示？
A: 确保玩家已完成 GAS 登录。未登录玩家会显示"未登录"。

### Q: 如何设置登录超时时间？
A: 在 `config.yml` 中修改 `login-timeout.timeout-seconds` 参数，单位为秒。设置为 `false` 可禁用超时检测。

### Q: 登录超时后会怎样？
A: 玩家会被自动踢出服务器，提示"登录超时，请重新尝试登录"。玩家需要重新执行 `/dlrs login` 进行登录。

## 开发信息

- **语言**: Kotlin 2.3.21
- **平台**: PaperMC 1.21.8+
- **Java 版本**: Java 25
- **构建工具**: Maven

### 构建命令

```bash
mvn clean package
```

构建产物位于 `target/DLRS-GAS-For-Minecraft-<version>.jar`

## 许可证

本项目遵循 MIT 许可证。

## 联系方式

如有问题或建议，请联系插件作者。

---

**注意**: 本插件需要有效的 DLRS 开发者账户和应用凭证才能正常工作。
