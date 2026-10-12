
<div align="center">

[![GitHub stars](https://img.shields.io/github/stars/ZCShou/GoGoGo?logo=github)](https://github.com/ZCShou/GoGoGo/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/ZCShou/GoGoGo?logo=github)](https://github.com/ZCShou/GoGoGo/network)
[![license](https://img.shields.io/github/license/ZCShou/GoGoGo)](https://github.com/ZCShou/GoGoGo/blob/master/LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/ZCShou/GoGoGo?label=Release)](https://github.com/ZCShou/GoGoGo/releases)
[![standard-readme compliant](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)
</div>
<div align="center">

[![Build Check](https://github.com/ZCShou/GoGoGo/actions/workflows/build-check.yml/badge.svg)](https://github.com/ZCShou/GoGoGo/actions/workflows/build-check.yml)
[![CodeQL](https://github.com/ZCShou/GoGoGo/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/ZCShou/GoGoGo/actions/workflows/codeql-analysis.yml)
</div>

<div align="center">
影梭 - 用于 Android 8.0+ 的无需 ROOT 权限的虚拟定位 APP
</div>

## 特别鸣谢ZCShou

## 警告 有任何问题请直接提交issue 或是删除 或是修改 禁止商用行为

## 简介
&emsp;&emsp;影梭是一个基于 Android 调试 API实现的安卓定位修改工具，并且同时实现了一个可以自由控制移动的摇杆。使用影梭，不需要 ROOT 权限就可以随意修改自己的当前位置以及模拟移动。


## 个人Fork原因及背景
&emsp;&emsp;将影梭用在校园运动跑步类app（步道乐跑 为本人学校使用）

&emsp;&emsp;原本的那个没有脚本 用起来好累 我就自己借用工具进行了修改

## 功能
1. 定位修改
2. 摇杆控制移动
3. 历史记录
4. 位置搜索
5. 直接输入坐标
6. 脚本模式（按脚本预设自动移动）

## 脚本模式
&emsp;&emsp;脚本模式可以先把一串“带移动状态的位置”写成脚本（走 / 跑 / 骑），启动后模拟位置就会按脚本自动移动，不需要手动摇杆。

**脚本格式**：一行一个路点，`状态 经度 纬度 [速度] [停留秒数]`，`#` 或 `//` 开头为注释行。

```
# 先步行，再跑步，最后骑车绕一圈
walk 116.397 39.908          # 不写速度时使用“设置”里的步行速度
run  116.401 39.910 4.5      # 跑步，4.5 米/秒
bike 116.405 39.912 20km/h   # 骑行，也可以写 km/h
walk 116.410 39.910 1.2 10   # 到达后原地停留 10 秒
```

1. 状态支持 `步行/跑步/骑行`（`walk/run/bike`），也可以写 `速度4.5`、`停留10` 这类标签形式；
2. 坐标系可选 `GPS(WGS-84)` 与 `BD09`，两套坐标的值域重叠、无法自动识别，所以需要自己选对；
3. 每个脚本可以单独设置“单次跑完停止”或者“循环”；
4. 编辑页支持直接粘贴经纬度，也可以点地图选点（按当前选中的状态插入到光标处）。

## 用法
1. 下载 APK 直接安装
2. 启动影梭，赋予相关权限
3. 单击地图位置，然后点击启动按钮



## 参考
&emsp;&emsp;由于本人也是个新手，纯属业余瞎搞，因此，在写影梭的过程中，参考了很多网友分享的技术文章、示例代码等。包括但不限于以下列出的几个：
1. https://github.com/Hilaver/MockGPS
2. https://github.com/bxxfighting/together-go
3. https://github.com/P72B/Mocklation

&emsp;&emsp;还有些 CSDN 上的文章，目前不记得地址了，如果您发现其中有直接引用或借鉴您的地方，请与我联系，我会再第一时间进行处理，谢谢！

## FAQ
Q：为何不支持 Android 8.0 以下版本？

A：因为手里没有机器无法进行适配。。。

Q：为何定位不是很稳定，偶尔会飘回真实位置？

A：这是是由于实现原理导致的，Android 调试 API 固有的问题。确切的说，应该是由于手机本身还开启了其他定位方式（例如，基站定位、wifi定位等）导致的


Q：为何在微信等腾讯系应用上定位不起作用？

A：建议去问一下腾讯。

Q：编译时 java 报错？

A：Gradle 使用的 java 版本与 Android Studio 使用的不一致。Gradle 默认会在环境变量中搜索 JAVA_HOME 来确定 Java 位置。

## 如何贡献
1. FORK -> PR
2. 加入影梭开发，共同完善

## 许可证
GPL-3.0-only © ZCShou

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FZCShou%2FGoGoGo.svg?type=large&issueType=license)](https://app.fossa.com/projects/git%2Bgithub.com%2FZCShou%2FGoGoGo?ref=badge_large&issueType=license)
