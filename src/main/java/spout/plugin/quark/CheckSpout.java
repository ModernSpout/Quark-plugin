package spout.plugin.quark;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

public final class CheckSpout {

    public static boolean checkSpout(ComponentLogger logger) {
        try {
            Class.forName("spout.server.paper.api.SpoutMarker");
            return true;
        } catch (ClassNotFoundException ignored) {
            logger.warn("This plugin requires Spout: https://github.com/ModernSpout/Spout-Paper-server");
            return false;
        }
    }

}
