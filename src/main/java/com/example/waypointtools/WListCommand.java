package com.example.waypointtools;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;

public final class WListCommand implements TabExecutor {

    /** バニラ /waypoint コマンドに対応する Bukkit 権限ノード。 */
    private static final String WAYPOINT_PERMISSION = "minecraft.command.waypoint";
    /** 代理実行するバニラコマンド（名前空間付きで確実にバニラを叩く）。 */
    private static final String WAYPOINT_LIST_COMMAND = "minecraft:waypoint list";

    private final WaypointToolsPlugin plugin;

    public WListCommand(@NotNull WaypointToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // wlist.use の有無は plugin.yml の commands.wlist.permission により Bukkit が事前チェック済み。
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        // バニラ /waypoint list を「プレイヤー自身」として代理実行する。
        //  - sender がプレイヤーなので出力先・列挙対象ディメンション（ワールド単位）が正しくなる。
        //  - 一時的に minecraft.command.waypoint を付与し権限レベル2制約を満たす。付与〜解除は
        //    メインスレッド同期で完結し、list 固定のみ dispatch するため modify/remove は実行され得ない。
        PermissionAttachment attachment = player.addAttachment(plugin);
        try {
            attachment.setPermission(WAYPOINT_PERMISSION, true);
            Bukkit.dispatchCommand(player, WAYPOINT_LIST_COMMAND);
        } catch (Exception e) {
            plugin.getLogger().warning("/waypoint list の代理実行に失敗: " + e.getMessage());
            player.sendMessage("§cウェイポイント一覧の取得に失敗しました。");
        } finally {
            player.removeAttachment(attachment);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        // /wlist は引数を取らない。プレイヤー名のデフォルト補完を抑止するため空リストを返す。
        return Collections.emptyList();
    }
}
