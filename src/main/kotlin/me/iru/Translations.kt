package me.iru

import net.md_5.bungee.api.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.regex.Pattern

enum class PrefixType {
    WARNING,
    ERROR,
    LOGIN,
    PIN,
    REGISTER,
    REMEMBER,
    UNREGISTER
}

enum class ParseMode {
    ResetAndTranslate,
    Translate,
    None
}

class Translations {
    private val authy = Authy.instance
    private val defaultLangFolder = File(authy.dataFolder, "lang" + File.separator + "defaults" + File.separator)
    private val defaultLangs = arrayListOf("en_us", "pl_pl", "ru_ru")
    private var cache: YamlConfiguration? = null

    init {
        checkDefaults()
        updateCache()
    }

    fun getPrefix(type: PrefixType): String {
        return "${getColor("prefix_${type.name.lowercase()}_color")}${get("prefix_${type.name.lowercase()}_value",
            ParseMode.Translate
        )}"
    }

    fun getColor(key: String): ChatColor {
        val mcRegex = Pattern.compile("^&([0-9a-f]{1})\$")
        val hexRegex = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})\$")
        val value = get(key, ParseMode.None)
        if(hexRegex.matcher(value).matches()) {
            return ChatColor.of(value)
        } else if(mcRegex.matcher(value).matches()) {
            return ChatColor.getByChar(value[1])
        } else {
            return ChatColor.BLACK
        }
    }

    fun get(key: String, mode: ParseMode = ParseMode.ResetAndTranslate): String {
        val value = cache?.getString(key) ?: "&cThere has been a translation error! Couldn't find key $key in ${authy.config.getString("lang")}"
        return when(mode) {
            ParseMode.ResetAndTranslate -> ChatColor.translateAlternateColorCodes('&', "&r$value")
            ParseMode.Translate -> ChatColor.translateAlternateColorCodes('&', value)
            ParseMode.None -> value
        }
    }

    fun updateCache() {
        val selectedLang = authy.config.getString("lang")
        val langFile = File(authy.dataFolder, "lang" + File.separator + selectedLang + ".yml")
        if(!langFile.exists()) {
            authy.server.consoleSender.sendMessage("${org.bukkit.ChatColor.DARK_GRAY}[${org.bukkit.ChatColor.GOLD}${authy.description.name}${org.bukkit.ChatColor.DARK_GRAY}] ${ChatColor.RED}No language file for $selectedLang found! Saving default...")
            checkDefaults()
        }
        cache = YamlConfiguration.loadConfiguration((langFile))
    }

    private fun checkDefaults() {
        if(!defaultLangFolder.exists()) {
            defaultLangFolder.mkdirs()
        }
        for (lang in defaultLangs) {
            val langFile = File(authy.dataFolder, "lang" + File.separator + lang + ".yml")
            if(!langFile.exists()) {
                authy.saveResource("lang${File.separator}$lang.yml", false)
            }
            authy.saveResource("lang${File.separator}defaults${File.separator}$lang.yml", true)
            if(hasOldVersion(lang)) {
                authy.server.consoleSender.sendMessage("${org.bukkit.ChatColor.DARK_GRAY}[${org.bukkit.ChatColor.GOLD}${authy.description.name}${org.bukkit.ChatColor.DARK_GRAY}] ${org.bukkit.ChatColor.AQUA}It looks like you have an older vesrion of lang file $lang.yml! You can go to ${authy.dataFolder.name}/lang/defaults/$lang.yml to see the changes and update/replace your main lang files. If not you may experience some issues")
            }
        }
    }

    private fun hasOldVersion(l : String) : Boolean{
        val langFile = File(authy.dataFolder, "lang" + File.separator + l + ".yml")
        val defaultLangFile = File(authy.dataFolder, "lang" + File.separator + "defaults" + File.separator + l + ".yml")
        val parsed = YamlConfiguration.loadConfiguration(langFile)
        val defaultParsed = YamlConfiguration.loadConfiguration(defaultLangFile)
        return defaultParsed.getInt("version") > parsed.getInt("version")
    }

}