# WaypointTools

Minecraft 公式 Waypoint（Locator Bar）機能の一部を、一般プレイヤーへ安全に開放する Paper プラグインです。

- **`/wlist`** … `/waypoint list` 相当のウェイポイント一覧を表示
- **`/wcolor <hex>`** … 自分自身のウェイポイント色を自由に変更（`/waypoint modify @s color hex <hex>` 相当）

---

## 背景・目的

公式 `/waypoint` コマンドは権限レベル2が必要で、Paper が公開する権限ノードは `minecraft.command.waypoint` の **1つだけ**です。
これを LuckPerms 等で付与すると `list` / `modify` / `remove` を **まとめて** 許可してしまい、「`list` だけ」「自分の色変更だけ」をサブコマンド単位で開放できません。

本プラグインは `/waypoint` 本体（特に他者を編集し得る `modify` / `remove`）は不可のまま、一般プレイヤーに必要な操作だけを専用コマンドとして提供します。

---

## 動作要件

| 項目 | バージョン |
| --- | --- |
| サーバー | Paper **26.1.2**（build 69 で確認） |
| Java | **25**（25.0.1 で確認） |
| ビルド | JDK 25 + Maven（`brew install openjdk@25 maven`） |
| 依存プラグイン | **なし**（LuckPerms は任意） |

> この jar 1個だけで動作します。追加ライブラリ・別プラグインは不要です（`paper-api` は `provided` スコープ＝サーバーが実行時に提供）。

---

## コマンド仕様

| コマンド | 引数 | 説明 | 実行者 | 権限 |
| --- | --- | --- | --- | --- |
| `/wlist` | なし | `/waypoint list` と同等の一覧（送信中の waypoint 名・色・件数）を表示 | プレイヤーのみ | `wlist.use` |
| `/wcolor <hex>` | 6桁の16進カラー | 自分のウェイポイント色を変更 | プレイヤーのみ | `wcolor.use` |
| `/wcolor reset` | `reset` / `clear` | 自分のウェイポイント色を既定に戻す | プレイヤーのみ | `wcolor.use` |

### `/wcolor` の入力フォーマット

以下はいずれも同じ色として解釈されます（6桁の16進・大文字小文字不問）。

```text
/wcolor F77E31
/wcolor #F77E31
/wcolor 0xF77E31
```

- 6桁の16進以外（例: `/wcolor xyz`, `/wcolor 123`）はエラーメッセージを返します。
- `/wcolor` は **自分自身にのみ** 作用し、他プレイヤーの色は変更できません。

### タブ補完

`/wcolor` でタブを押すと、`reset` と代表的な16進カラーのプリセットが候補に出ます（入力文字で前方一致フィルタ）。任意の6桁16進は今まで通り直接入力できます。

```text
reset  FF0000  00FF00  0000FF  FFFF00  FF00FF  00FFFF  FFFFFF  F77E31
```

`/wlist` は引数を取らないため候補を表示しません（既定のプレイヤー名補完を抑止）。
補完一覧はサーバー起動時にクライアントへ配信されるため、更新後は **再起動＋再接続** で反映されます。

---

## 権限

| 権限ノード | 既定 | 説明 |
| --- | --- | --- |
| `wlist.use` | `true`（全員） | `/wlist` の使用を許可 |
| `wcolor.use` | `true`（全員） | `/wcolor` の使用を許可 |

既定では全プレイヤーが両コマンドを使用できます（LuckPerms の追加設定は不要）。
特定プレイヤー／グループを **禁止** したい場合のみ、対象に対して該当ノードを `false` に設定します。

```bash
# 例: あるグループに /wcolor を禁止
lp group default permission set wcolor.use false
# 例: あるユーザーに /wlist を禁止
lp user <name> permission set wlist.use false
```

> `/waypoint` 本体は従来どおり `minecraft.command.waypoint` を付与しない限り一般プレイヤーは実行不可のままです。本プラグインは同ノードを永続付与しません。

---

## 仕組み（技術メモ）

2コマンドで実装手法を分けています（公開 API のカバー範囲が異なるため）。

### `/wlist`
バニラ `/waypoint list` を **プレイヤー自身として代理実行** します。

- Paper 26.1 には「送信中の waypoint を列挙する」公開 API が無いため、バニラコマンドを呼びます。
- `/waypoint list` は **ソースのワールド（ディメンション）単位** で列挙されるため、プレイヤー自身を実行者にすることで結果が正しくなります（ネザー/エンドでも正しい）。
- 実行の一瞬だけ `minecraft.command.waypoint` を一時付与し、権限レベル2制約を満たします。付与〜解除はメインスレッド同期で完結し、`list` のみを固定で dispatch するため `modify` / `remove` が実行されることはありません。

### `/wcolor`
公開 API `Player#setWaypointColor(Color)` を直接呼びます。

- `/waypoint modify @s color hex <hex>` と等価で、**権限昇格も dispatch も不要** のクリーンな実装です。
- `player` オブジェクトに対して呼ぶため、**構造上、自分以外には作用しません**。

### タブ補完（共通）

両コマンドとも `TabExecutor` を実装し、`/wcolor` は色プリセット、`/wlist` は空リストを返すことで、既定のオンラインプレイヤー名補完を抑止しています。

---

## ビルド

JDK 25 と Maven が必要です（未導入なら `brew install openjdk@25 maven`）。
付属の `deploy.sh` でビルドできます（**Docker 不要**）。

```bash
./deploy.sh
```

生成物: `target/WaypointTools-1.0.0.jar`

`deploy.sh` は内部で JDK 25 を指定して `mvn clean package` を実行します。
別の場所の JDK を使う場合は `JAVA_HOME=/path/to/jdk25 ./deploy.sh` で上書きできます。直接ビルドするなら:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean package
```

---

## サーバーへの配置

ビルドした jar をサーバーの `plugins/` に置いてサーバーを再起動します。

```bash
# バインドマウントしている場合（ホスト側 plugins ディレクトリへコピー）
cp target/WaypointTools-1.0.0.jar /path/to/data/plugins/
docker restart <コンテナ名>

# 名前付きボリューム等の場合（コンテナへ直接コピー）
docker cp target/WaypointTools-1.0.0.jar <コンテナ名>:/data/plugins/
docker restart <コンテナ名>
```

起動ログに以下が出れば成功です。

```text
[WaypointTools] WaypointTools を有効化しました。/wlist, /wcolor が利用可能です。
```

---

## プロジェクト構成

```text
.
├── pom.xml
├── deploy.sh
├── README.md
└── src/main/
    ├── java/com/example/waypointtools/
    │   ├── WaypointToolsPlugin.java   # 本体（2コマンド登録）
    │   ├── WListCommand.java          # /wlist
    │   └── WColorCommand.java         # /wcolor
    └── resources/plugin.yml
```

> `com.example` / `WaypointTools` / コマンド名は任意でリネーム可能です（pom.xml・各 `package`・`plugin.yml` を揃えて変更）。

---

## 注意点

以下は **`/wlist` のみ** に関係します（`/wcolor` は公開 API 直叩きのため無関係）。

- **`sendCommandFeedback` gamerule**: `false` の場合、バニラのクエリ出力が抑制され `/wlist` の結果が表示されない可能性があります（既定 `true`）。表示されない場合はこの gamerule を確認してください。
- **LuckPerms で `minecraft.command.waypoint` を明示的に negate している場合**、一時付与より negate が優先され `/wlist` が機能しないことがあります。その場合は `Bukkit.createCommandSender(...)`（コンソール相当権限で実行し出力をプレイヤーへ転送）方式へ切り替えてください（ただし列挙はオーバーワールド固定になります）。
- `paper-api` の build 番号はサーバー更新に追従可能です（例: `26.1.2.build.70-stable`）。
