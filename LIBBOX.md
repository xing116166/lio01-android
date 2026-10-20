# libbox（sing-box 内核）构建说明

`app/libs/libbox.aar` 由 [sing-box](https://github.com/SagerNet/sing-box) **v1.9.7** 源码通过 gomobile 编译，仅包含 Android `arm64-v8a` 与 `armeabi-v7a` 两个 ABI。

GPL-3.0 要求分发二进制时提供对应源代码，sing-box v1.9.7 源码可从
<https://github.com/SagerNet/sing-box/tree/v1.9.7> 获取。

## 工具链

| 组件 | 版本 |
| --- | --- |
| Go | 1.22.5 |
| Android NDK | 26.2.11394342（r26b） |
| gomobile | `github.com/sagernet/gomobile v0.1.4` |
| JDK | 17 |

## 步骤

```bash
# 1. 安装 gomobile / gobind
go install github.com/sagernet/gomobile/cmd/gomobile@v0.1.4
go install github.com/sagernet/gomobile/cmd/gobind@v0.1.4

# 2. 环境变量
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_NDK_HOME=/path/to/android-ndk-r26b
export JAVA_HOME=/path/to/jdk17

# 3. 编译（在 sing-box 源码根目录）
go run ./cmd/internal/build_libbox -target android
```

## 本地构建补丁

为了在 Windows 上使用已解压的 NDK 并减小 AAR 体积，编译前对源码做了两处临时修改（不改变内核行为）：

1. `cmd/internal/build_shared/sdk.go` 的 `findNDK()` 开头增加：

```go
if ndkHome := os.Getenv("ANDROID_NDK_HOME"); rw.FileExists(filepath.Join(ndkHome, "source.properties")) {
    androidNDKPath = ndkHome
    return true
}
```

2. `cmd/internal/build_libbox/main.go` 的 Android 参数中增加：

```go
"-target", "android/arm64,android/arm",
```

输出文件为 `libbox.aar`，复制到 `app/libs/` 即可参与构建。
