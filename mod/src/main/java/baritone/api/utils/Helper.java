/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.utils;

import baritone.api.BaritoneAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Calendar;
import java.util.stream.Stream;

/**
 * Server-side helper. Chat/toast/notification methods log to server console
 * instead of rendering on the client.
 *
 * @author Brady (original), Alice Project (server-side port)
 * @since 8/1/2018
 */
public interface Helper {

    Logger LOGGER = LogManager.getLogger("Baritone");

    Helper HELPER = new Helper() {};

    static Component getPrefix() {
        final Calendar now = Calendar.getInstance();
        final boolean xd = now.get(Calendar.MONTH) == Calendar.APRIL && now.get(Calendar.DAY_OF_MONTH) <= 3;
        MutableComponent baritone = Component.literal(xd ? "Baritoe" : BaritoneAPI.getSettings().shortBaritonePrefix.value ? "B" : "Baritone");
        baritone.setStyle(baritone.getStyle().withColor(ChatFormatting.LIGHT_PURPLE));

        MutableComponent prefix = Component.literal("");
        prefix.setStyle(baritone.getStyle().withColor(ChatFormatting.DARK_PURPLE));
        prefix.append("[");
        prefix.append(baritone);
        prefix.append("]");

        return prefix;
    }

    default void logToast(Component title, Component message) {
        LOGGER.info("[Toast] {} {}", title.getString(), message.getString());
    }

    default void logToast(String title, String message) {
        logToast(Component.literal(title), Component.literal(message));
    }

    default void logToast(String message) {
        logToast(Helper.getPrefix(), Component.literal(message));
    }

    default void logNotification(String message) {
        logNotification(message, false);
    }

    default void logNotification(String message, boolean error) {
        if (BaritoneAPI.getSettings().desktopNotifications.value) {
            logNotificationDirect(message, error);
        }
    }

    default void logNotificationDirect(String message) {
        logNotificationDirect(message, false);
    }

    default void logNotificationDirect(String message, boolean error) {
        if (error) {
            LOGGER.warn("[Notification] {}", message);
        } else {
            LOGGER.info("[Notification] {}", message);
        }
    }

    default void logDebug(String message) {
        if (!BaritoneAPI.getSettings().chatDebug.value) {
            return;
        }
        logDirect(message, false);
    }

    default void logDirect(boolean logAsToast, Component... components) {
        MutableComponent component = Component.literal("");
        component.append(getPrefix());
        component.append(Component.literal(" "));
        Arrays.asList(components).forEach(component::append);
        LOGGER.info(component.getString());
    }

    default void logDirect(Component... components) {
        logDirect(false, components);
    }

    default void logDirect(String message, ChatFormatting color, boolean logAsToast) {
        Stream.of(message.split("\n")).forEach(line -> {
            MutableComponent component = Component.literal(line.replace("\t", "    "));
            component.setStyle(component.getStyle().withColor(color));
            logDirect(logAsToast, component);
        });
    }

    default void logDirect(String message, ChatFormatting color) {
        logDirect(message, color, false);
    }

    default void logDirect(String message, boolean logAsToast) {
        logDirect(message, ChatFormatting.GRAY, logAsToast);
    }

    default void logDirect(String message) {
        logDirect(message, false);
    }

    default void logUnhandledException(final Throwable exception) {
        HELPER.logDirect("An unhandled exception occurred. " +
                        "The error is in your game's log, please report this at https://github.com/cabaletta/baritone/issues",
                ChatFormatting.RED);
        exception.printStackTrace();
    }
}
