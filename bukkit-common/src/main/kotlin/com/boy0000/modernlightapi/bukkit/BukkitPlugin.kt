/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Vladimir Mikhailov <beykerykt@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.boy0000.modernlightapi.bukkit

import com.boy0000.modernlightapi.Build
import com.boy0000.modernlightapi.ModernLightAPI
import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin

class BukkitPlugin : JavaPlugin() {
    override fun onLoad() {
        instance = this
        mImpl = BukkitPlatformImpl(instance!!)
        // set server implementation
        try {
            ModernLightAPI.prepare(mImpl!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onEnable() {
        // try initialization LightAPI
        try {
            ModernLightAPI.initialization()
        } catch (ex: Exception) {
            ex.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }
    }

    override fun onDisable() {
        mImpl?.let { ModernLightAPI.shutdown(it) }
        HandlerList.unregisterAll(this)
    }

    private val platformImpl: BukkitPlatformImpl
        get() {
            checkNotNull(mImpl) { "IBukkitPlatformImpl not yet initialized!" }
            return mImpl as BukkitPlatformImpl
        }

    fun log(sender: CommandSender, message: String) {
        sender.sendMessage(ChatColor.AQUA.toString() + "<LightAPI>: " + ChatColor.WHITE + message)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (command.name.equals("lightapi", ignoreCase = true)) {
            if (sender is Player) {
                if (args.isEmpty()) {
                    sender.sendMessage(Component.text("<aqua> ------- <LightAPI <white>${description.version}</white>> ------- "))
                    sender.sendMessage(Component.text("<aqua>Current version: <white>${description.version}"))
                    sender.sendMessage(Component.text("<aqua>Current implementation: <white>${Build.CURRENT_IMPLEMENTATION}"))
                    sender.sendMessage(Component.text("<aqua>LightEngine type: <white>${platformImpl.lightEngine?.lightEngineType}"))
                    sender.sendMessage(Component.text("<aqua>LightEngine version: <white>${platformImpl.lightEngine?.lightEngineVersion}"))
                    sender.sendMessage(Component.text("<aqua>Server name: <white>${server.name}"))
                    sender.sendMessage(Component.text("<aqua>Server version: <white>${server.version}"))

                    //TODO Update this to use Adventure
                    val text = TextComponent(" | ")
                    val sourcecode = TextComponent(ChatColor.AQUA.toString() + "Source code")
                    sourcecode.clickEvent = ClickEvent(
                        ClickEvent.Action.OPEN_URL,
                        "http://github.com/BeYkeRYkt/LightAPI/"
                    )
                    sourcecode.hoverEvent = HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ComponentBuilder("Goto the GitHub!").create()
                    )
                    text.addExtra(sourcecode)
                    text.addExtra(TextComponent(ChatColor.WHITE.toString() + " | "))
                    val developer = TextComponent(ChatColor.AQUA.toString() + "Developers")
                    val authors = description.authors
                    var authorsLine = StringBuilder("none")
                    if (authors.size > 0) {
                        authorsLine = StringBuilder(authors[0])
                        if (authors.size > 1) {
                            for (i in 1 until authors.size - 1) {
                                authorsLine.append(", ").append(authors[i])
                            }
                            authorsLine.append(" and ").append(authors[authors.size - 1])
                        }
                    }
                    developer.hoverEvent = HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ComponentBuilder(authorsLine.toString()).create()
                    )
                    text.addExtra(developer)
                    text.addExtra(TextComponent(ChatColor.WHITE.toString() + " | "))
                    val contributors = TextComponent(ChatColor.AQUA.toString() + "Contributors")
                    contributors.clickEvent = ClickEvent(
                        ClickEvent.Action.OPEN_URL,
                        "https://github.com/BeYkeRYkt/LightAPI/graphs/contributors"
                    )
                    contributors.hoverEvent = HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ComponentBuilder("ALIENS!!").create()
                    )
                    text.addExtra(contributors)
                    text.addExtra(TextComponent(ChatColor.WHITE.toString() + " | "))
                    sender.spigot().sendMessage(text)
                    val licensed = TextComponent(" Licensed under ")
                    val MIT = TextComponent(ChatColor.AQUA.toString() + "MIT License")
                    MIT.clickEvent = ClickEvent(
                        ClickEvent.Action.OPEN_URL,
                        "https://opensource.org/licenses/MIT/"
                    )
                    MIT.hoverEvent = HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ComponentBuilder("Goto for information about license!").create()
                    )
                    licensed.addExtra(MIT)
                    sender.spigot().sendMessage(licensed)
                    if (platformImpl.isBackwardAvailable) {
                        sender.sendMessage(ChatColor.WHITE.toString() + "backwards compatibility is enabled")
                    }
                } else {
                    when (args[0]) {
                        "debug" -> if (sender.hasPermission("lightapi.debug") || sender.isOp()) {
                            mImpl?.toggleDebug()
                        } else {
                            log(sender, ChatColor.RED.toString() + "You don't have permission!")
                        }

                        else -> log(
                            sender, ChatColor.RED
                                .toString() + "Hmm... This command does not exist. Are you sure write correctly ?"
                        )
                    }
                }
            } else if (sender is ConsoleCommandSender) {
                val console: ConsoleCommandSender = sender
                if (args.isEmpty()) {
                    console.sendMessage(Component.text("<aqua> ------- <LightAPI <white>${description.version}</white>> ------- "))
                    console.sendMessage(Component.text("<aqua>Current version: <white>${description.version}"))
                    console.sendMessage(Component.text("<aqua>Current implementation: <white>${Build.CURRENT_IMPLEMENTATION}"))
                    console.sendMessage(Component.text("<aqua>LightEngine type: <white>${platformImpl.lightEngine?.lightEngineType}"))
                    console.sendMessage(Component.text("<aqua>LightEngine version: <white>${platformImpl.lightEngine?.lightEngineVersion}"))
                    console.sendMessage(Component.text("<aqua>Server name: <white>${server.name}"))
                    console.sendMessage(Component.text("<aqua>Server version: <white>${server.version}"))
                    console.sendMessage(Component.text("<aqua>Source code: ").append(Component.empty().clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl("https://github.com/BeYkeRYkt/LightAPI/"))))

                    //TODO Update below to adventure
                    val authors = description.authors
                    var authorsLine = StringBuilder("none")
                    if (authors.size > 0) {
                        authorsLine = StringBuilder(authors[0])
                        if (authors.size > 1) {
                            for (i in 1 until authors.size - 1) {
                                authorsLine.append(", ").append(authors[i])
                            }
                            authorsLine.append(" and ").append(authors[authors.size - 1])
                        }
                    }
                    console.sendMessage(ChatColor.AQUA.toString() + " Developers: " + ChatColor.WHITE + authorsLine)
                    console.sendMessage("")
                    console.sendMessage(ChatColor.WHITE.toString() + " Licensed under: " + ChatColor.AQUA + "MIT License")
                    if (platformImpl.isBackwardAvailable) {
                        console.sendMessage(ChatColor.WHITE.toString() + "backwards compatibility is enabled")
                    }
                } else {
                    when (args[0]) {
                        "debug" -> if (sender.hasPermission("lightapi.debug") || sender.isOp) {
                            mImpl?.toggleDebug()
                        } else {
                            log(sender, ChatColor.RED.toString() + "You don't have permission!")
                        }

                        else -> log(
                            console, ChatColor.RED
                                .toString() + "Hmm... This command does not exist. Are you sure write correctly ?"
                        )
                    }
                }
            }
        }
        return true
    }

    companion object {
        var instance: BukkitPlugin? = null
            private set
        private var mImpl: BukkitPlatformImpl? = null
    }
}
