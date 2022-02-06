package dev.dealnote.players;

import zombie.characters.IsoPlayer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        ZomboidPlayerFinder finder = new ZomboidPlayerFinder();

        try {
            List<IsoPlayer> all = finder.findAll("players.db");

            for (IsoPlayer player : all) {
                System.out.println(player.getName() + ": " + player.getZombieKills());
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}
