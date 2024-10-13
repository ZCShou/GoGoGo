<p align="center">
<img src="./docs/images/LOGO.png" height="80"/>
</p>

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

## 简介
&emsp;&emsp;影梭是一个基于 Android 调试 API + 百度地图及定位 SDK 实现的安卓定位修改工具，并且同时实现了一个可以自由控制移动的摇杆。使用影梭，不需要 ROOT 权限就可以随意修改自己的当前位置以及模拟移动。

1. 源码仓库：[Github](https://github.com/ZCShou/GoGoGo)（推荐）、[Gitee](https://gitee.com/itexp/gogogo)（镜像）
2. 下载地址：[Github](https://github.com/ZCShou/GoGoGo/releases)（推荐）、[Gitee](https://gitee.com/itexp/gogogo/releases)（镜像）

## 警告一
&emsp;&emsp;**最近，有网友直接白嫖影梭后改名为标枪定位，然后添加广告（除了加广告，功能没有任何改变），但是，没有按照 GPLv3 协议的要求进行开源（我已经联系过该网友进了提醒，但并没有收到回复），在此提醒：**
1. **开源 ≠ 白嫖，请遵循开源协议**
2. **GPL 的法律效力在国内相关诉讼案例很多，请自行搜索，权衡利弊。影梭保留追究相关侵权人员法律责任的所有权利！**
3. **开源不易，且行且珍惜**

## 警告二
&emsp;&emsp;**最近，有很多人将影梭用在校园运动类 APP（包括但不限于闪动校园、TakeTwo、运动世界校园等）中作弊，开发者也收到了很多人提问为何影梭定位并不起作用或者寻求对影梭的改进，在此提醒：**
1. **影梭不支持任何校园运动类 APP 的作弊行为**
2. **影梭开发者也不赞同采用任何形式在校园运动中作弊**

## 背景
&emsp;&emsp;之前在玩一款 VR 游戏：一起来捉妖。为了省事，就想有没有可以更改位置的 APP。经过一番摸索发现确实有不少可以修改位置的 APP。但是，绝大多数这种 APP 都是收费的，而且贼贵！

&emsp;&emsp;我比较感兴趣的是这样的技术是如何实现的，因此，决定研究研究自己写一个！现在游戏已经弃坑了，但是技术不能丢。因此，将研究结果开源出来方便大家一起学习！但是请注意（重要的事情说三遍！否则后果自负）：

1. 该 APP 仅仅是为了学习 Android + 百度地图的实现方法，请勿用于游戏作弊！
2. 该 APP 仅仅是为了学习 Android + 百度地图的实现方法，请勿用于游戏作弊！
3. 该 APP 仅仅是为了学习 Android + 百度地图的实现方法，请勿用于游戏作弊！

## 功能
1. 定位修改
2. 摇杆控制移动
3. 历史记录
4. 位置搜索
5. 直接输入坐标

## 截图
![joystick.jpg](./docs/images/joystick.jpg)
![search_history.jpg](./docs/images/search_history.jpg)
![map.jpg](./docs/images/map.jpg)

## 用法
1. 下载 APK 直接安装
2. 启动影梭，赋予相关权限
3. 单击地图位置，然后点击启动按钮

## 文档
&emsp;&emsp;由于本人并不是做移动开发的，很多功能代码写的都比较差。我也第一次写  Android APP，目前还处在学习中。。。此外，就一个简单的 APP，应该也不需要啥文档，开发过程中遇到的一些问题，我一般都会记录在个人博客中，具体参见：https://blog.csdn.net/zcshoucsdn/category_10559121.html

&emsp;&emsp;如果有疑问可以直接搜索 ISSUE 或者 在上面直接提交问题。

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

Q：是否支持鸿蒙系统？

A：经过测试，影梭可以在鸿蒙系统上正常运行。

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
