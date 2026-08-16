# Third API Admin UI

当前阶段先交付一个可打开的静态管理端原型，包含 3 种结构不同的页面变体，用于确认信息架构和交互方向。

## 打开方式

直接浏览器打开：

```bash
open prototype/index.html
```

## 变体切换

默认打开 `A · Command Center`，也可以带参数直达：

```text
prototype/index.html?variant=A
prototype/index.html?variant=B
prototype/index.html?variant=C
```

页面底部有固定切换条，支持左右箭头和键盘 `←` / `→` 切换。

- `A · Command Center`：深色实时运维总览，KPI、趋势图、渠道状态、事件流
- `B · Service Desk`：浅色配置管理工作台，渠道表格 + 发布单，偏 CRUD 流程
- `C · Traffic Control`：接口调度视图，业务入口 → 接口定义 → 渠道运行态 → 调用流水

原型是静态页面，没有后端接口，数据均为演示数据。选定变体后，再进入正式管理端页面开发。
