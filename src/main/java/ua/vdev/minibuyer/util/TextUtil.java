package ua.vdev.minibuyer.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public static Component mm(String msg, Map<String, String> placeholders) {
        if (msg == null) return Component.empty();

        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        msg = replaceHex(msg);

        if (msg.contains("&")) {
            msg = msg.replace("<reset>", "");
            return LEGACY.deserialize(msg);
        }

        return MINI.deserialize(msg);
    }

    private static String replaceHex(String msg) {
        Matcher matcher = HEX_PATTERN.matcher(msg);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<color:#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}