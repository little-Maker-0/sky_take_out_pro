## uni-miniapp（根据后端接口重建）

这是一个基于 `sky-take-out/sky-server` C 端接口重新生成的 uni-app 微信小程序工程（最小可用闭环）。

### 后端要求

- 默认后端地址：`http://localhost:8080`（可在 `utils/config.js` 修改）
- 登录：`POST /user/user/login`，body：`{ "code": "<wx.login返回的code>" }`
- 鉴权请求头：`authentication: <jwt>`（后端配置 `sky.jwt.user-token-name`）

### 运行方式（推荐）

1. 使用 HBuilderX 打开本目录 `uni-miniapp/`
2. 运行到 **微信小程序**
3. 首次进入会自动调用 `wx.login` 并请求后端登录接口获取 token

### 常见坑

- **真机/预览请求不到后端**
  - 把 `utils/config.js` 的 `baseURL` 从 `http://localhost:8080` 改成你电脑的局域网 IP，例如 `http://192.168.1.10:8080`
  - 确保手机和电脑在同一局域网
  - 确保后端 `8080` 端口未被防火墙拦截
- **后端返回“未登录/401/权限不足”**
  - 检查请求头是否带了 `authentication`
  - 在小程序“我的”页可以看到 token 预览
- **微信登录失败**
  - 后端 `wxLogin` 会拿 `code` 换取 openid，需要后端配置正确的 `appid/secret`
  - 若你只是本地联调，可临时在后端做“mock 登录”（不建议上生产）

### 目录结构

- `App.vue`：应用入口
- `pages/`：页面
- `utils/request.js`：统一请求封装（自动带 token、统一错误提示）
- `api/`：按业务拆分的接口方法

