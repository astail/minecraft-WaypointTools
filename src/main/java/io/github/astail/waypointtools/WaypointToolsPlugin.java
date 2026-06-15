package io.github.astail.waypointtools;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class WaypointToolsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        if (!register("wlist", new WListCommand(this))) return;
        if (!register("wcolor", new WColorCommand())) return;
        getLogger().info("WaypointTools を有効化しました。/wlist, /wcolor が利用可能です。");
    }

    private boolean register(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("コマンド '" + name + "' が plugin.yml に未定義です。プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
        return true;
    }
}
