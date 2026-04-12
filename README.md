# MiaoMenu

## 功能对照表

| Issue #5 需求 | 当前实现 |
| --- | --- |
| item_conditions | 已支持 Java/Bedrock 菜单项级条件 |
| view_requirement | 已支持菜单级访问条件 |
| requirement_blocks | 已支持可复用 requirement_blocks |
| deny_message_and_fallback_ui | 已支持 deny_message 与 fallback_menu |
| Placeholder 支持 | 已保留并扩展 PlaceholderAPI/基础占位符解析 |
| 多语言消息 | 已通过 config.yml messages 节点统一管理 |
| 权限节点 | 已通过权限条件与 plugin.yml 权限节点支持 |
| 数据持久化 | 仍为基于 YAML 的配置持久化 |

## 构建

```bash
mvn test
mvn package
```

生成产物：`target/MiaoMenu-2.6.jar`
