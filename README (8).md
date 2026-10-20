# lio01 Android 客户端

基于 [cedar2025/Xboard](https://github.com/cedar2025/Xboard) 面板 API 的第三方 Android 客户端，使用 **Kotlin + Jetpack Compose** 编写。Xboard 兼容 V2Board 的 `/api/v1` 接口，因此本客户端同样适用于大多数 V2Board 面板。

> 本项目是非官方客户端，与 Xboard 项目无隶属关系。账号、套餐、订单等数据全部保存在你连接的面板服务器上。

## 下载

- 安装包：[`dist/lio01-1.1.0.apk`](dist/lio01-1.1.0.apk)（已签名，可直接安装，约 29 MB）
- 源码包：[`dist/lio01-1.1.0-src.zip`](dist/lio01-1.1.0-src.zip)
- 校验和：`dist/*.sha256`

![应用图标](docs/app-icon.png)

## 界面预览

| 首页 | 套餐 | 节点 |
| --- | --- | --- |
| ![首页](docs/screenshots/home.png) | ![套餐](docs/screenshots/plans.png) | ![节点](docs/screenshots/nodes.png) |

| 套餐详情 | 我的 | 登录 |
| --- | --- | --- |
| ![套餐详情](docs/screenshots/plan-detail.png) | ![我的](docs/screenshots/profile.png) | ![登录](docs/screenshots/login.png) |

> 截图由 `ScreenshotTest` 在 Robolectric 中连接 Mock 面板真实渲染生成。

## 功能

| 模块 | 能力 |
| --- | --- |
| 服务器 | 首次启动填写面板地址，支持随时更换；自动校验 `/guest/comm/config` |
| 账号 | 邮箱登录、注册（支持邀请码 / 邮箱验证码）、忘记密码、修改密码 |
| 首页 | 套餐状态、剩余流量、到期时间、设备限制、余额、公告、快捷入口 |
| 套餐 | 套餐列表、详情、周期选择、优惠码、下单创建订单 |
| 支付 | 余额抵扣、支付方式与手续费展示、跳转支付 / 扫码支付、订单状态自动轮询 |
| 订阅 | 订阅链接复制 / 二维码 / 分享，节点列表（协议、服务器、端口） |
| 代理内核 | 内置 sing-box，支持 VMess / VLESS / Trojan / Shadowsocks / Hysteria2 / TUIC，一键 TUN 全局代理 |
| 订单 | 订单列表、状态、详情、取消、继续支付 |
| 邀请 | 邀请码、邀请链接、邀请人数与佣金统计 |
| 其他 | 深色 / 浅色 / 跟随系统主题、公告详情、多面板切换、退出登录 |

## 技术栈

- Kotlin 2.0.21 / Jetpack Compose（BOM 2024.10.01）/ Material 3
- Navigation Compose、ViewModel + StateFlow、DataStore Preferences
- OkHttp 4.12 + kotlinx.serialization（无 Retrofit / Hilt）
- sing-box 1.9.7 libbox（Go + gomobile AAR），负责 TUN、路由与协议实现
- ZXing core（本地生成订阅二维码）
- minSdk 24（Android 7.0），targetSdk 35（Android 15）
- 仅包含 rm64-v8a / rmeabi-v7a 原生库（不支持 x86 模拟器）

## 快速开始

1. 安装 `Xboard-1.0.0.apk`。
2. 客户端已预置面板 `https://airport.limx01.com`，安装后直接进入登录页；如需换服，可在登录页底部「切换服务器」或设置中修改。
3. 使用面板账号登录，即可查看订阅、购买套餐、复制订阅链接。
4. 打开「节点」页，选择节点后点击「连接」，授权系统 VPN 即可开启全局代理；点击「断开」停止。

> 如果面板启用了 reCAPTCHA / Turnstile 人机验证，注册或登录可能需要在网页端完成；客户端会给出提示。

默认服务器定义在 `app/src/main/java/com/xboard/app/AppConfig.kt` 的 `DEFAULT_BASE_URL`，改一行即可换成其他面板。

## 构建

内置 sing-box 内核（pp/libs/libbox.aar）的编译方式与对应源码说明见 [docs/LIBBOX.md](docs/LIBBOX.md)。

环境要求：

- JDK 17
- Android SDK Platform 35 + Build-Tools 35.0.0
- Gradle 8.11.1（或使用本机安装的兼容版本）

```bash
cd XboardAndroid
# 指向本机 Android SDK
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleDebug      # 调试包
./gradlew assembleRelease    # 正式包（需签名）
```

产物：

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

### 签名说明

仓库内的 `keystore/xboard-demo.jks` 仅用于生成可直接安装的演示包：

| 项 | 值 |
| --- | --- |
| Store password | `xboard2024` |
| Key alias | `xboard` |
| Key password | `xboard2024` |

正式发布前请替换为你自己的 keystore，并移除仓库中的演示密钥。

## 项目结构

```
XboardAndroid/
├── app/src/main/java/com/xboard/app/
│   ├── MainActivity.kt             # 单 Activity + Compose 入口
│   ├── AppViewModel.kt             # 会话、用户、套餐、订单等状态
│   ├── vpn/                        # sing-box 内核、VPN 服务、订阅解析
│   ├── data/
│   │   ├── Models.kt               # API 数据模型
│   │   ├── Session.kt              # DataStore 会话持久化
│   │   └── XboardApi.kt            # OkHttp + kotlinx.serialization 客户端
│   ├── ui/
│   │   ├── AppRoot.kt              # 导航图、启动页、登录态跳转
│   │   ├── components/Common.kt    # 通用组件
│   │   ├── screens/                # 各页面
│   │   └── theme/Theme.kt          # 品牌色 / 深色模式
│   └── util/                       # 格式化、二维码、跳转工具
├── app/src/test/                   # JVM 单元测试（API / 格式化）
├── docs/                           # 图标与文档
└── keystore/
```

## API 使用

所有请求以 `{面板地址}/api/v1` 为前缀，登录后使用 `Authorization: Bearer <token>`。核心接口：

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 面板配置 | GET | `/guest/comm/config` |
| 登录 / 注册 / 找回 | POST | `/passport/auth/login`、`/passport/auth/register`、`/passport/auth/forget` |
| 邮箱验证码 | POST | `/passport/comm/sendEmailVerify` |
| 用户信息 | GET | `/user/info` |
| 订阅信息 | GET | `/user/getSubscribe` |
| 套餐列表 | GET | `/user/plan/fetch` |
| 节点列表 | GET | `/user/server/fetch` |
| 创建订单 | POST | `/user/order/save` |
| 支付方式 | GET | `/user/order/getPaymentMethod` |
| 发起支付 | POST | `/user/order/checkout` |
| 订单状态 | GET | `/user/order/check` |
| 订单列表 / 详情 / 取消 | GET/POST | `/user/order/fetch`、`/user/order/detail`、`/user/order/cancel` |
| 公告 | GET | `/user/notice/fetch` |
| 邀请 | GET | `/user/invite/fetch`、`/user/invite/save` |
| 修改密码 | POST | `/user/changePassword` |

更详细的字段说明与单位约定见 [docs/API.md](docs/API.md)。

## 单位约定（重要）

- 用户流量 `transfer_enable`、`u`、`d` 单位为 **字节（bytes）**；套餐 `transfer_enable` 单位为 **GB**（购买时面板会 ×1024³ 转为字节）
- 套餐价格、订单金额、余额、佣金单位为 **分**（API 返回的套餐价格已 ×100）
- 下单时 `period` 必须使用旧版周期键：`month_price`、`quarter_price`、`half_year_price`、`year_price`、`two_year_price`、`three_year_price`、`onetime_price`、`reset_price`

## 推送到 GitHub

1. 在 GitHub 新建一个**空仓库**（不要勾选 README / .gitignore / License）。
2. 解压源码包，进入项目根目录（能看到 settings.gradle.kts 的那一层）。
3. 执行推送（Windows PowerShell）：

`powershell
.\tools\push-to-github.ps1 https://github.com/<user>/<repo>.git
`

macOS / Linux：

`ash
chmod +x tools/push-to-github.sh
./tools/push-to-github.sh https://github.com/<user>/<repo>.git
`

也可以手动执行：

`ash
git init
git add .
git commit -m "lio01 Android client v1.1.0"
git branch -M main
git remote add origin https://github.com/<user>/<repo>.git
git push -u origin main
`

仓库包含 .github/workflows/android.yml，推送后 GitHub Actions 会自动构建 Debug APK。
正式版可把本地 dist/lio01-1.1.0.apk 手动上传到仓库的 Release。

## 测试

```bash
cd XboardAndroid
./gradlew testDebugUnitTest
```

共 26 个 JVM 测试：

- `XboardApiTest`：使用 MockWebServer 校验登录、用户信息、套餐、下单、支付结果映射、错误处理与 401/403
- `FormatTest`：流量 / 金额格式化、地址规范化、HTML 清理、周期解析
- `AppSmokeTest`：Robolectric 启动真实 `MainActivity`，确认首屏渲染无崩溃
- `ScreenshotTest`：用 Mock 面板渲染首页 / 套餐 / 节点 / 我的 / 登录页，输出到 `app/build/screenshots/`

Robolectric 需要 `android-all` jar：首次运行会在联网时自动下载；也可以提前执行
`python tools/fetch-robolectric.py` 放进 `.robolectric-deps/`，之后测试完全离线运行。

## 已知限制 / Roadmap

- [ ] 工单系统（列表 / 新建 / 回复）
- [ ] 知识库
- [ ] 节点延迟测速
- [ ] Clash YAML 订阅格式
- [ ] 分应用代理 / 路由规则
- [ ] 订阅自动更新
- [ ] 多语言（当前为简体中文）
- [ ] 人机验证内置 WebView
- [ ] 支付回调深链跳转

## 许可证

本项目以 **GNU General Public License v3.0** 发布，详见 [LICENSE](LICENSE)。

内置的 [sing-box](https://github.com/SagerNet/sing-box)（libbox）同样使用 GPL-3.0，版权归 SagerNet 及其贡献者所有。
如果你分发本应用的修改版本，请同时提供对应源代码。

## 免责声明

本项目仅供学习与个人使用。请遵守你所在地区的法律法规以及面板服务条款。
