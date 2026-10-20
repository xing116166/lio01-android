# Xboard API 对接说明

本文记录客户端实际使用的接口、字段与单位约定，便于在面板升级后排查兼容性问题。

## 基础约定

- Base URL：`https://<panel>`（客户端会自动补全协议、去掉尾部 `/`、去掉误填的 `/api/v1`）
- API 前缀：`/api/v1`
- 认证：登录返回 `data.auth_data`，格式为 `Bearer <sanctum-token>`；后续请求放到 `Authorization` 头
- 请求体：`application/json`
- 多数响应为：

```json
{ "status": "success", "message": "", "data": { } }
```

- 失败响应使用 HTTP 状态码（400/403/422/500），`message` 为面板语言包中的提示
- 部分旧接口不套 `data` 信封，见下文

## 认证与会话

### POST /passport/auth/login

```json
{ "email": "user@example.com", "password": "********" }
```

响应 `data`：

```json
{ "token": "订阅 token", "auth_data": "Bearer 1|xxxx", "is_admin": false }
```

> 客户端保存 `auth_data` 作为请求凭证；`token` 是订阅 token，不用于 API 认证。

### POST /passport/auth/register

```json
{ "email": "...", "password": "...", "invite_code": "可选", "email_code": "可选" }
```

成功直接返回与登录相同的 `data`，客户端会自动登录。

### POST /passport/auth/forget

```json
{ "email": "...", "email_code": "123456", "password": "新密码" }
```

### POST /passport/comm/sendEmailVerify

```json
{ "email": "..." }
```

## 用户与订阅

### GET /user/info

`data` 主要字段：

| 字段 | 说明 |
| --- | --- |
| `email` | 邮箱 |
| `balance` / `commission_balance` | 余额 / 佣金（分） |
| `expired_at` | 到期时间（秒级时间戳） |
| `plan_id` | 当前套餐 ID，0 表示未订阅 |
| `transfer_enable` | 总流量（字节） |
| `device_limit` | 设备数限制 |
| `banned` | 是否封禁 |
| `uuid` | 用户 UUID |

注意：`/user/info` **不返回** `u` / `d`，已用流量需从 `/user/getSubscribe` 获取。

### GET /user/getSubscribe

`data` 主要字段：

| 字段 | 说明 |
| --- | --- |
| `plan` | 当前套餐对象 |
| `u` / `d` | 上行 / 下行已用流量（字节） |
| `transfer_enable` | 总流量（字节） |
| `subscribe_url` | 订阅链接 |
| `expired_at` / `next_reset_at` | 到期 / 下次重置时间 |
| `device_limit` / `speed_limit` | 设备 / 限速 |
| `reset_day` | 流量重置日 |

## 套餐

### GET /user/plan/fetch

`data` 为数组。价格字段（单位：分，面板已把元 ×100）：

`month_price`、`quarter_price`、`half_year_price`、`year_price`、`two_year_price`、`three_year_price`、`onetime_price`、`reset_price`

其它字段：

| 字段 | 说明 |
| --- | --- |
| `transfer_enable` | 流量（GB） |
| `device_limit` / `speed_limit` | `null` 表示不限 |
| `content` | HTML 描述，客户端会去标签显示 |
| `tags` | 标签数组，可能为 `null` |
| `capacity_limit` | 整数或字符串（售罄时为 `"Sold out"`），可能为 `null` |
| `sell` / `renew` / `show` | 销售状态 |

## 节点

### GET /user/server/fetch

返回 `data` 数组，元素字段：

```json
{ "id": 1, "type": "vmess", "name": "香港 01", "rate": 1, "tags": ["HK"], "is_online": true }
```

节点可用性由面板根据用户订阅过滤，无有效订阅时返回空数组。

## 订单与支付

### POST /user/order/save

```json
{ "plan_id": 1, "period": "month_price", "coupon_code": "可选" }
```

- `period` 只接受旧版键（见 README），客户端已按此发送
- 返回 `data` 为 `trade_no` 字符串

### GET /user/order/getPaymentMethod

`data` 数组：`id`、`name`、`payment`、`icon`、`handling_fee_fixed`（分）、`handling_fee_percent`

### POST /user/order/checkout

```json
{ "trade_no": "2024...", "method": 1 }
```

**此接口不套 `data` 信封**，直接返回：

```json
{ "type": 0, "data": "https://pay.example.com/..." }
```

| type | 含义 | 客户端处理 |
| --- | --- | --- |
| `-1` | 无需支付（余额抵扣/免费） | 直接视为开通成功 |
| `0` | 支付链接 | 打开系统浏览器 |
| `1` | 二维码内容 | 本地生成二维码弹窗 |
| 其它 | 插件自定义 | 文本展示 + 复制 |

支付完成后客户端轮询 `GET /user/order/check?trade_no=...`，`data` 为订单状态：

- `0` 待支付
- `1` 开通中
- `2` 已取消
- `3` 已完成
- `4` 已折抵

### GET /user/order/fetch

`data` 数组，元素为订单对象，含 `plan`（套餐对象）、`period`（旧版周期键）、`total_amount`（分）、`status`、`created_at` 等。

## 公告

### GET /user/notice/fetch?current=1

**不套信封**：

```json
{ "data": [ { "id": 1, "title": "...", "content": "<p>HTML</p>", "created_at": 1700000000 } ], "total": 1 }
```

每页固定 5 条。

## 邀请

### GET /user/invite/fetch

```json
{
  "data": {
    "codes": [ { "id": 1, "code": "abcd1234", "status": 0 } ],
    "stat": [注册人数, 有效佣金, 确认中佣金, 佣金比例, 可用佣金]
  }
}
```

`stat` 中金额单位为分，比例为整数百分比。

### GET /user/invite/save

生成新邀请码，`data` 为 `true`。

## 其它

- `POST /user/changePassword`：`old_password`、`new_password`
- `GET /user/comm/config`：货币符号、Telegram 等（登录后）
- `GET /guest/comm/config`：面板名称、logo、TOS、是否开启邮箱验证 / 邀请码强制 / 人机验证

## 错误处理约定

- HTTP 401：登录失效
- HTTP 403 且 `message` 包含「登录」：登录失效（Xboard 中间件返回「未登录或登陆已过期」）
- HTTP 422：参数校验失败，`message` 为通用提示，`errors` 含字段级提示
- 其它 HTTP 错误：展示 `message`

## 兼容性备注

- Xboard 使用 Laravel Sanctum；原始 V2Board 直接使用 token，本客户端兼容两种返回（`auth_data` 优先，缺失时回退 `token` 并补 `Bearer`）
- 面板若启用验证码（reCAPTCHA / Turnstile），登录/注册接口需要额外验证参数，客户端会提示改用网页端
- 支付能力完全取决于面板已启用的支付插件
