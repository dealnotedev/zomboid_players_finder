package dev.dealnote.players;

import zombie.characters.IsoPlayer;

import java.util.List;

public class Test {
    public static void main(String[] args) {
        ZomboidPlayerFinder finder = new ZomboidPlayerFinder();

        List<IsoPlayer> all = finder.findAll("players.db");
        for (IsoPlayer isoPlayer : all) {
            System.out.println(isoPlayer.getName() + ": " + isoPlayer.getZombieKills());
        }
    }
}
