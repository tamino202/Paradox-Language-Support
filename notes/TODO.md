# TODO

## 更新计划

* 完善在线参考文档
* BUG修复
* 功能优化
    * [ ] 有些地方获取的作用域是可能的（`from = country?`）
    * [ ] 确认重命名功能能够预期正确进行（如果对应的声明/引用支持重命名）
    * [ ] 基于引用的重命名需要考虑存在前后缀的情况（主要是为图标引用考虑）
    * [ ] ~~一些检查可以基于当前文件、文件路径（相对于游戏或模组根目录）、定义类型（例如，`event`）、定义成员路径（例如，`event.abc`）等来部分禁用~~（已经没有必要）
    * [ ] 可扩展的对本地化命令、本地化命令作用域的支持（包括引用解析、作用域获取、代码补全等）
    * [ ] 需要重新调整对返回的规则列表的排序
    * [ ] 编辑本地化文件时提供输入彩色文本、图标等的快捷键（仅在可用的位置生效）
* 功能优化 - CWT规则支持
    * [ ] ~~为复杂枚举如`complex_enum[policy_option]`提供相关本地化支持（类似定义）~~（搁置，不能很好地显示出来，复杂枚举名可能本身就是一个本地化引用）
    * [ ] 优化：scope的名字（准确来说是别名）可以包含点号
    * [ ] CWT规则文件中有些之前被认为必定是常量字符串的地方（如枚举值的名字），也可能是一个表达式而非仅仅是常量字符串
    * [ ] ~~兼容CWT规则文件中的错误级别`severity = warning`或`## severity = warning`~~（PLS和CWTools实现有所不同，需要分析）
    * [ ] ［待确定］作为trigger的值的CWT规则`scope_field` `scope[xxx]` `scope_group[xxx]`也可以匹配一个布尔值？
* 功能优化 - 智能推断
    * [ ] 基于使用处推断一些定义（如`scripted_effect`）的作用域上下文
    * [ ] 基于使用处推断本地化命令的作用域上下文
    * [ ] ~~可以通过特殊注释强制指定定义类型（基于文件路径或者基于直接的类型+子类型） - 用于实现差异比较等功能~~（不考虑）
* 新增功能
    * 代码检查（`Code > Inspect Code...`）
        * [ ] ~~图标属性的值引用了定义自身（`foo { icon = foo ... }`）~~（不觉得这有什么意义）
    * 操作（`Action`）
        * [ ] 从指定的本地化文件生成其他语言区域的本地化文件（右键菜单&项目视图&工具栏操作，考虑支持指定多个或者整个目录的情况）
        * [ ] 从封装变量/定义/本地化/文件进行重载（通过对话框选择生成的位置）
    * 其他
        * [ ] ［待确定］实现对`*.gui`文件中的GUI定义的UI预览（参考IDEA的Markdown插件的实现）
        * [ ] ［待确定］实现对`*.txt`文件中的定义的UI预览（参考游戏中的效果以及灰机Wiki的实现）
    * ［低优先级］上下移动声明（`Code > Move Statement Up/Down`）
        * [ ] 脚本文件：上下移动属性或单独的值
        * [ ] 本地化文件：上下移动本地化属性
    * ［低优先级］对于封装变量、定义、本地化和内联脚本实现安全删除功能
    * ［低优先级］对于封装变量、scripted_trigger、scripted_effect、inline_script等实现内联功能

## 追踪中

* [ ] DDS文件路径以及符合条件的快速文档链接也能作为html/markdown等文件中的图片超链接使用，从而渲染DDS图片和本地化
* [ ] 改为基于语言注入功能（`Language Injection`）支持脚本文件中的各种复杂表达式以及本地化文件中的本地化命令表达式
* [ ] 将获取作用域上下文的代码提取成扩展点
* [ ] 对任何带有作用域上下文的声明或引用（包括CWT规则的引用），统一提示作用域上下文
* [ ] 参照Irony Mod Manager的实现，实现扩展点以在查询时，如果有必要，按照覆盖顺序排序封装变量/定义/本地化
* [ ] 添加代码检查，基于下列规则检查脚本文件中是否存在可能的BUG
    * ~~scripted_trigger/scripted_effect不能递归调用~~（已实现对应的代码检查）
    * scripted_trigger/scripted_effect的调用层数最大只能有5层
    * ~~内联数学表达式（inline_math）在每个scripted_trigger/scripted_effect中最多只能使用1次~~（当前游戏版本已无此限制）
    * 对于valueSetValue，只能通过后缀的`@xxx`切换flag和event_target的作用域
    * ~~不能在asset文件中使用scripted_variable和inline_math~~（已实现对应的代码检查）
* [ ] 在更多情况下尝试推断脚本参数对应的CWT规则，从而提供各种高级语言功能（如，基于CWT规则的代码高亮、引用解析和代码补全）

新增可以通过以下几种情况推断脚本参数对应的CWT规则：

```
ethic = ethic_$ETHIC$ # 脚本参数作为某个脚本表达式的一部分，这个脚本表达式中仅存在这唯一一个脚本参数，且可以得到这个脚本表达式对应的CWT规则
```