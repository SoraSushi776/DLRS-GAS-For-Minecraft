# DLRS-GAS For Minecraft

一个Minecraft Paper插件，用于集成DLRS（Digital Learning & Research System）账号系统。

## 功能特性

- ✅ OAuth 2.0授权登录
- ✅ 自动登录支持（使用access_token）
- ✅ AES-256-CBC加密保护应用密钥
- ✅ 玩家数据持久化存储
- ✅ 完整的命令系统
- ✅ 玩家加入时自动登录
- ✅ 中文消息输出

## 安装步骤

1. 将编译好的JAR文件放入服务器的`plugins`文件夹
2. 启动服务器以生成配置文件
3. 编辑`plugins/DLRS-GAS-For-Minecraft/config.yml`文件
4. 配置您的DLRS应用ID和应用Token
5. 重启服务器

## 配置说明

在`config.yml`中配置以下参数：

```yaml
dlrs:
  # DLRS应用ID (从DLRS开发者后台获取)
  app-id: "55"
  
  # DLRS应用Token (从DLRS开发者后台获取，请妥善保管)
  app-token: "YOUR_APP_TOKEN"
  
  # 语言设置 (en=英文, zh=中文)
  language: "en"
```

**重要**: 您需要从DLRS开发者后台获取`app-id`和`app-token`才能使用此插件。

## 命令列表

| 命令 | 权限 | 描述 |
|------|------|------|
| `/dlrs login` | 所有玩家 | 开始DLRS账号登录流程 |
| `/dlrs logout` | 所有玩家 | 登出DLRS账号 |
| `/dlrs status` | 所有玩家 | 查看当前登录状态 |
| `/dlrs info` | 所有玩家 | 查看账号详细信息 |

### 命令别名
- `/dlrsgas` 
- `/dlrsl`

## 使用流程

### 首次登录

1. 在游戏中输入 `/dlrs login`
2. 插件会显示一个授权链接
3. 在浏览器中打开该链接
4. 在网页上完成DLRS账号登录和授权
5. 返回游戏，插件会自动验证登录状态
6. 登录成功后会显示欢迎信息

### 自动登录

- 玩家首次登录成功后，access_token会被保存
- 下次加入服务器时，插件会自动尝试验证token并登录
- 如果token过期，玩家需要重新执行 `/dlrs login`

### 查看信息

使用 `/dlrs info` 可以查看：
- 用户ID (uid)
- 昵称 (nickname)
- 邮箱 (email)
- 用户组 (user_group)
- 头像URL (avatar_url)

## 技术实现

### 架构组件

- **AESEncryptor**: AES-256-CBC加密工具，用于加密appToken
- **HttpUtil**: HTTP请求工具，与DLRS API通信
- **DLRSConfig**: 配置管理类
- **DLRSLoginService**: OAuth登录服务，处理完整登录流程
- **DLRSAutoLoginService**: 自动登录服务，验证access_token
- **DLRSCommandHandler**: 命令处理器

### API端点

插件使用以下DLRS API端点：

- `https://api.chinadlrs.com/developer/oauth.php` - OAuth认证
- `https://api.chinadlrs.com/developer/profile.php` - 获取用户信息
- `https://api.chinadlrs.com/developer/auto-login.php` - 自动登录验证
- `https://gas.chinadlrs.com/oauth` - OAuth授权页面

### 数据存储

玩家登录信息存储在`config.yml`的`players`节点下：

```yaml
players:
  <player-uuid>:
    uid: "用户ID"
    nickname: "昵称"
    email: "邮箱"
    access_token: "访问令牌"
    avatar_url: "头像URL"
    user_group: "用户组"
```

## 安全注意事项

1. **保护appToken**: 不要在公开场合泄露您的appToken
2. **HTTPS通信**: 所有API请求都使用HTTPS加密
3. **Token管理**: access_token本地存储，定期验证有效性
4. **加密传输**: appToken在传输前使用AES-256-CBC加密

## 常见问题

### Q: 登录后没有反应？
A: 确保您已在浏览器中完成了授权流程，并且网络连接正常。

### Q: 自动登录失败怎么办？
A: Token可能已过期，请使用 `/dlrs logout` 然后重新登录。

### Q: 如何更改配置？
A: 编辑`config.yml`后，使用 `/reload` 命令或重启服务器。

### Q: 支持哪些Minecraft版本？
A: 本插件基于Paper API 1.21.8+构建，适用于1.21.8及以上版本。

## 开发信息

- **语言**: Kotlin 2.3.21
- **平台**: PaperMC 1.21.8+
- **Java版本**: Java 25
- **构建工具**: Maven

## 许可证

本项目遵循MIT许可证。

## 联系方式

如有问题或建议，请联系插件作者。

---

**注意**: 本插件需要有效的DLRS开发者账户和应用凭证才能正常工作。
