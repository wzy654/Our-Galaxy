# Our Galaxy · 我们的宇宙

专属于两个人的私人数字记忆空间。核心理念：**记录过去，陪伴现在，创造未来。**

当前已完成 **Phase 1：账号、空间与共同回忆 MVP**，并完成电影感宇宙首页、穿梭过渡和沉浸式三维回忆星图。前后端可连接真实 MySQL 8 与 Redis，支持注册登录、JWT 退出失效、情侣空间邀请、回忆 CRUD、标签和受控图片上传；三维星图支持回忆星球旋转、搜索索引、详情查看与原有增删改查。时间胶囊、AI、地图与年度报告尚未实现。

## 技术架构

- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios、Element Plus、SCSS、Three.js
- 后端：Java 17、Spring Boot 3、Maven Wrapper、MyBatis Plus、Spring Security、JWT、Flyway
- 数据：MySQL 8、Redis
- 接口：统一 `/api` 前缀、`ApiResponse<T>` 统一响应、SpringDoc OpenAPI
- 文件：本地存储、真实图片内容校验、空间成员鉴权下载
- 部署：Docker Compose 为可选开发方式；当前验收使用本机便携式 MySQL/Redis，不依赖 Docker

## 目录结构

```text
Our Galaxy/
├── backend/
│   ├── src/main/java/com/ourgalaxy/
│   │   ├── auth/               # 注册、登录、退出、JWT 与限流
│   │   ├── common/             # 安全配置、统一响应、异常与实体基类
│   │   ├── file/               # 上传、图片校验、鉴权下载
│   │   ├── memory/             # 回忆、图片关联与标签
│   │   ├── space/              # 空间、成员与邀请
│   │   ├── system/             # 健康检查
│   │   └── user/               # 用户
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-test.yml
│   │   └── db/migration/       # Flyway 迁移
│   ├── src/test/java/com/ourgalaxy/
│   │   ├── integration/        # 真实 MySQL/Redis 集成测试
│   │   └── system/             # 安全与健康检查测试
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw
├── frontend/
│   ├── src/
│   │   ├── api/                # Axios 与业务 API
│   │   ├── assets/styles/      # 设计 Token 与全局样式
│   │   ├── components/         # 页头、鉴权图片组件
│   │   ├── router/             # 路由与认证守卫
│   │   ├── stores/             # Pinia 状态
│   │   ├── types/
│   │   └── views/              # 首页、认证、空间、回忆
│   ├── Dockerfile
│   ├── package.json
│   └── vite.config.ts
├── docs/database/er.md          # 完整 ER 图与约束说明
├── deploy/                      # 后续 Nginx 配置目录
├── .local/                      # 本机便携式 MySQL/Redis，不提交 Git
├── docker-compose.yml
├── .env.example
└── README.md
```

## 环境要求

- JDK 17 或更高版本；项目通过 `maven.compiler.release=17` 生成 Java 17 字节码
- Maven 3.9+（可选，项目自带 Maven Wrapper 3.9.9）
- Node.js 22+ 与 npm
- MySQL 8 与 Redis；本机已安装便携版，也可使用已有服务
- Docker Engine 与 Docker Compose 仅在选用容器启动时需要

后端通过 `.mvn/settings.xml` 使用阿里云 Central 镜像，不修改全局 Maven 配置。本次开发环境使用 JDK 21 编译 Java 17 目标字节码、Node.js 24 和 npm 11。

## 环境配置

复制根目录模板并填写真实值：

```bash
cp .env.example .env
```

至少填写 `DB_PASSWORD`、`REDIS_PASSWORD` 和 `JWT_SECRET`；测试变量可使用默认值：

| 变量 | 用途 |
| --- | --- |
| `DB_PASSWORD` | MySQL 应用账号密码 |
| `REDIS_PASSWORD` | Redis 访问密码 |
| `JWT_SECRET` | JWT 签名密钥，至少 32 字节 |
| `TEST_DB_NAME` | 集成测试数据库，默认 `our_galaxy_test` |
| `TEST_REDIS_DATABASE` | 集成测试 Redis 逻辑库，默认 `15` |

其他变量及默认值见 `.env.example`。不要提交 `.env`，也不要把密钥写入前端代码。

Maven 和 Java 子进程不会自动读取 `.env`。在 Git Bash 中必须先导出：

```bash
set -a
. ./.env
set +a
```

## 本机 MySQL 与 Redis

本机便携版已安装在 `.local/`，当前数据目录分别为 `.local/mysql/data/` 和 `.local/redis/data/`。MySQL 绑定 `127.0.0.1:3306`，Redis 绑定 `127.0.0.1:6379`；应用库 `our_galaxy` 与测试库 `our_galaxy_test` 已创建，Flyway V1 已执行成功。

这些进程目前是开发会话中的后台进程，不是 Windows 服务，重启电脑后需要重新启动。

### 启动 MySQL

在项目根目录执行：

```bash
"./.local/mysql-8.0.44-winx64/bin/mysqld.exe" \
  --defaults-file="$(pwd)/.local/mysql/my.ini"
```

配置见 `.local/mysql/my.ini`：UTC、`utf8mb4`、`utf8mb4_0900_ai_ci`、仅监听本机。

### 启动 Redis

```bash
set -a
. ./.env
set +a
exec "./.local/redis-server.exe" "./.local/redis/redis.conf" \
  --requirepass "$REDIS_PASSWORD"
```

配置见 `.local/redis/redis.conf`，启用 AOF 持久化。验证连接：

```bash
set -a
. ./.env
set +a
(cd .local && ./redis-cli.exe -h 127.0.0.1 -p 6379 -a "$REDIS_PASSWORD" ping)
```

预期返回 `PONG`。

## 本地启动应用

### 1. 启动后端

先确保 MySQL、Redis 已启动。当前机器的 JDK 不在 `PATH` 中，因此显式指定 `JAVA_HOME` 和 Java 可执行文件：

```bash
cd backend
set -a
. ../.env
set +a
export JAVA_HOME="D:/JDK"

./mvnw -q test package
"D:/JDK/bin/java.exe" -jar target/our-galaxy-backend-0.1.0-SNAPSHOT.jar
```

其他机器若已将 Java 加入 `PATH`，可把最后一行改为 `java -jar target/our-galaxy-backend-0.1.0-SNAPSHOT.jar`。后端默认监听 `8080`。

应用启动时自动执行 Flyway。已执行的迁移禁止修改；schema 变更必须新增 `V2_...sql` 等版本文件。

### 2. 启动前端

另开一个终端：

```bash
cd frontend
npm ci
npm run dev
```

前端默认监听 `http://localhost:5173`，通过 Vite 代理将 `/api` 转发到 `http://localhost:8080`。首页健康状态来自后端真实响应，不使用模拟数据。

## Docker Compose（可选）

当前 Phase 1 验收没有使用 Docker。填写 `.env` 后，也可选择容器化开发环境：

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

服务包含 MySQL 8、Redis、后端和前端；后端的 JWT、上传目录及持久化卷已接入 Compose。该编排仍为开发用途，不包含 HTTPS 和生产级 Nginx，本次未在无 Docker 的机器上执行容器验收。

## 健康检查与接口文档

- 前端入口：<http://localhost:5173>
- 应用存活检查：<http://localhost:8080/api/system/health>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- Swagger UI：<http://localhost:8080/swagger-ui.html>

主要接口：

- `POST /api/auth/register`、`POST /api/auth/login`
- `GET /api/auth/me`、`POST /api/auth/logout`
- `GET /api/spaces/current`、`POST /api/spaces`
- `POST /api/spaces/invitations`、`POST /api/spaces/join`
- `GET/POST /api/memories`
- `GET/PUT/DELETE /api/memories/{id}`
- `POST /api/files`、`GET /api/files/{id}`

统一响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "UP",
    "service": "our-galaxy-backend",
    "version": "0.1.0",
    "scope": "APPLICATION",
    "timestamp": "2026-11-25T00:00:00Z"
  },
  "timestamp": "2026-11-25T00:00:00Z"
}
```

## 空间隔离与文件安全

- 前端不能指定或切换其他情侣空间；`couple_id` 由后端从 JWT 用户和成员关系推导。
- 所有详情、修改、删除和文件查询都带当前空间条件。
- 创建空间和加入空间使用用户行锁、空间行锁与邀请行锁，限制每个用户一个空间、每个空间最多两名成员。
- 登录使用 Redis 限流；退出后将 token ID 写入 Redis 拒绝列表，原 token 立即失效。
- 文件访问必须验证空间成员关系，前端使用 Authorization 请求获取 Blob，不把文件 URL 当作公开资源。
- 图片仅支持 JPEG/PNG/GIF，校验真实文件内容、大小和像素数，物理文件名随机化。

## 编译与测试

后端：

```bash
cd backend
set -a
. ../.env
set +a
export JAVA_HOME="D:/JDK"
./mvnw -q test package
```

集成测试使用 `our_galaxy_test` 和 Redis 逻辑库 `15`，请勿指向生产数据。

前端：

```bash
cd frontend
npm ci
npm run type-check
npm test
npm run build
```

### Phase 1 验收记录（2026-11-25）

- MySQL 8.0.44、Redis 8.10.3 本机启动及认证通过，Redis `PING` 返回 `PONG`。
- Flyway V1 执行成功，共 13 张业务表。
- `./mvnw -q test package`：3 个后端测试通过，0 失败、0 错误，可执行 JAR 打包成功。
- 真实 MySQL/Redis 集成测试覆盖注册登录、空间邀请加入、跨空间回忆和文件访问隔离、同空间共享、退出令牌失效。
- 回忆编辑回归覆盖保留原关联、移除全部图片和标签、再次加入原图片和标签。
- `npm test`：5 个前端测试通过；`npm run build`：类型检查和 Vite 构建成功。
- 浏览器验证注册登录、空间创建与邀请、带图回忆创建、双成员共享、编辑移除与重新添加、认证路由守卫。
- 524px 窄视口下回忆页移动端媒体查询生效，无横向溢出。
- 页面控制台无错误或警告。构建存在约 1 MB 主 JS chunk 提示，不影响当前运行，后续再做路由级代码分割。

## 数据库迁移

- 迁移目录：`backend/src/main/resources/db/migration`
- 当前版本：`V1__init_schema.sql`
- 共 13 张表：`user`、`couple_space`、`couple_member`、`couple_invitation`、`memory_event`、`file_object`、`memory_media`、`memory_tag`、`capsule`、`ai_record`、`place_memory`、`place_media`、`annual_report`
- 所有空间业务表包含 `couple_id`
- `DATETIME(6)` 统一为 UTC，`DATE` 表示不带时区的日历日期
- 删除采用逻辑删除，关联恢复复用历史记录以避免唯一键冲突

## 当前边界与下一阶段

Phase 1 不包含时间胶囊、AI 助手、地点地图、年度报告、未来模拟器和 Three.js 沉浸式首页。

下一步 Phase 2 将实现时间胶囊：

1. 信件创建、列表与详情
2. 后端强制隐藏未到期正文，不能只依赖前端隐藏
3. 到期开启、开启记录和空间隔离
4. 未开启正文的跨空间与直接接口访问测试

后续阶段再依次接入 AI、Love Map、Annual Report、Future Simulator 和完整 3D 星空体验。
