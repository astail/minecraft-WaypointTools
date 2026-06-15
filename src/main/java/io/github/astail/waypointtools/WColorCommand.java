package io.github.astail.waypointtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class WColorCommand implements TabExecutor {

    /** 第1引数のサジェスト候補（reset + 代表的な16進カラー）。 */
    private static final List<String> SUGGESTIONS = List.of(
            "reset", "FF0000", "00FF00", "0000FF", "FFFF00",
            "FF00FF", "00FFFF", "FFFFFF", "F77E31");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage("§e使い方: /wcolor <16進カラー>   例: /wcolor F77E31");
            player.sendMessage("§e      /wcolor reset   で色をリセット");
            return true;
        }

        String input = args[0];
        if (input.equalsIgnoreCase("reset") || input.equalsIgnoreCase("clear")) {
            player.setWaypointColor(null); // 公式 API: 自分の waypoint 色を既定へ戻す
            player.sendMessage("§aウェイポイントの色をリセットしました。");
            return true;
        }

        // "#RRGGBB" / "0xRRGGBB" / "RRGGBB" を許容
        String hex = input;
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.length() >= 2 && (hex.startsWith("0x") || hex.startsWith("0X"))) {
            hex = hex.substring(2);
        }
        if (!hex.matches("[0-9A-Fa-f]{6}")) {
            player.sendMessage("§c無効なカラーコードです。6桁の16進で指定してください。例: /wcolor F77E31");
            return true;
        }

        int rgb = Integer.parseInt(hex, 16);
        player.setWaypointColor(Color.fromRGB(rgb)); // = /waypoint modify @s color hex <hex> 相当（自分のみ）
        player.sendMessage("§aウェイポイントの色を §f#" + hex.toUpperCase() + " §aに変更しました。");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        // 第1引数のみ候補を返す。プレイヤー名のデフォルト補完を出さないため、
        // 該当なしでも null ではなく空リストを返す。
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : SUGGESTIONS) {
            if (s.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(s);
            }
        }
        return out;
    }
}
