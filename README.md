# 老鼠：重启 | Rats: Reboot

Minecraft 1.20.1 的非官方 Rats 合并与扩展。驯养、进化并指挥老鼠，经营布瑞克工坊，从公会悬赏板派遣鼠队，探索下水道，或建造无线发电站。

## 游玩

在 Forge 1.20.1 实例中安装 Citadel 和本项目的发布 jar。发布包内含 Rats 与公会模块，同时保留 Rats、Rats Reborn 原作者署名。已测试 Forge 47.4.29 与 Citadel 2.6.1。安装 Patchouli 85 后，用书和奶酪合成《鼠鼠帝国手册》。多人游戏的服务端与客户端使用相同版本。

## 从源码构建（Windows）

需要 JDK 17。工程自带 Gradle 8.8 Wrapper。首次构建需要联网获取 Gradle、Forge 及 Maven 依赖。将下列开发依赖的 1.20.1 Forge jar 放入同一个目录，或使用已有的模组目录：

- Citadel 2.6.1、Create 6.0.8、Farmer’s Delight 1.3.4、Jade 11.12.2、JEI 15.58.0.209。
- RS Integration 1.6.0、Goety 2.5.60.6、Iron’s Spells ’n Spellbooks 3.16.3、Curios 5.14.1，以及诡厄铁法：黄昏 1.0.0-r53。

在仓库根目录运行 PowerShell，用自己的路径替换示例目录：

```powershell
.\prepare-dependencies.ps1 -ModsFolder 'D:\Minecraft\mods'
.\gradlew.bat build
```

构建产物为 `build/libs/Rats-Reboot-Forge-1.20.1-8.1.3-fieldguide.1.jar`。准备脚本按文件名查找依赖，将它们复制到 Git 忽略的本地目录；原始模组目录只读。

## 代码和资源

- `src/main/java/com/github/alexthe666/rats` 包含 Rats、进化、职业、军团和下水道等实现。
- `src/main/java/guildjane` 包含公会接待员、任务派遣、客户端界面、发电站与能量联动。
- `src/main/resources` 包含本版使用的模型、纹理、声音、维度定义、数据表与配方。
- 手册正文位于 `src/main/resources/assets/rats/patchouli_books/field_manual/en_us`。Patchouli 使用这个目录作默认内容树，本版正文为中文。修改其中的 JSON 后重新构建即可。

### 可选的资源维护工具

手册的编写素材和生成器位于 `tools/handbook`。使用 Python 3.10 或以上运行 `python tools/handbook/generate.py`，即可由仓库内的章节与注册表快照重新生成手册。正常构建会直接使用已经生成的 JSON。

接待员模型的转换工具位于 `tools/models`。从署名文件所列作品页取得两个原始 FBX 下载包后，运行 `python tools/models/extract.py <下载包目录>`，再用 Blender 4.3 的后台模式执行 `blender --background --python tools/models/export_model.py`，可重新导出网格和骨骼。构建和游玩均使用仓库中已经导出的资源。

## 许可与署名

Rats 原作者、Rats Reborn 来源和上游代码许可见 [MERGE-NOTICE.txt](MERGE-NOTICE.txt) 与 [LICENSE](LICENSE)。两套 Jane Doe 模型的作者、作品、CC BY 4.0 许可和适配说明见 [JANE-DOE-CREDITS.txt](JANE-DOE-CREDITS.txt)。语音来源也记录在该文件中，模型许可仅适用于对应模型。
