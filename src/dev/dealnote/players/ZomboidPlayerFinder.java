package dev.dealnote.players;

import org.uncommons.maths.random.CellularAutomatonRNG;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.Lua.LuaManager;
import zombie.SoundManager;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.core.ImmutableColor;
import zombie.core.Rand;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.iso.IsoCell;
import zombie.iso.IsoWorld;
import zombie.util.ByteBufferOutputStream;
import zombie.util.PZSQLUtils;
import zombie.world.DictionaryData;
import zombie.world.WorldDictionary;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class ZomboidPlayerFinder {

    static {
        SoundManager.instance = new SoundManager();
        SurvivorDesc.HairCommonColors.add(ImmutableColor.blue);

        Rand.rand = new CellularAutomatonRNG();
        HairStyles.instance = new HairStyles();
        LuaManager.env = new KahluaTableImpl(new LinkedHashMap<>());

        try {
            final Field data = WorldDictionary.class.getDeclaredField("data");
            data.setAccessible(true);
            data.set(null, new DictionaryData());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException();
        }
    }

    public List<IsoPlayer> findAll(String databasePath) throws SQLException, IOException {
        int width = 512;
        int height = 512;

        final IsoCell isoCell = new IsoCell(width, height);

        final SurvivorDesc survivorDesc = new SurvivorDesc();
        survivorDesc.getHumanVisual().setSkinTextureIndex(0);
        survivorDesc.getHumanVisual().setSkinTextureName("Text");

        final List<IsoPlayer> players = new ArrayList<>();
        final Closeables closeables = new Closeables();

        try {
            final Connection connection = PZSQLUtils.getConnection(databasePath);
            closeables.register(connection);

            final PreparedStatement statement = connection.prepareStatement("SELECT data,worldversion,x,y,z,isDead,name,username FROM networkPlayers");
            closeables.register(statement);

            final ResultSet resultSet = closeables.register(statement.executeQuery());

            while (resultSet.next()){
                final PlayerData data = new PlayerData();
                data.setBytes(resultSet.getBinaryStream(1));

                final IsoPlayer player = new IsoPlayer(isoCell, survivorDesc, 0, 0, 0);
                player.setName(resultSet.getString(8));
                player.load(data.m_byteBuffer, IsoWorld.getWorldVersion());

                players.add(player);
            }
        } finally {
            closeables.close();
        }

        return players;
    }

    private static final class Closeables {
        final List<AutoCloseable> all = new LinkedList<>();

        <T extends AutoCloseable> T register(T closeable){
            all.add(closeable);
            return closeable;
        }

        void close(){
            for (AutoCloseable closeable : all) {
                try {
                    closeable.close();
                } catch (Exception ignored) {

                }
            }
            all.clear();
        }
    }

    private static final ThreadLocal<byte[]> TL_Bytes = ThreadLocal.withInitial(() -> new byte[1024]);

    private static final class PlayerData {
        ByteBuffer m_byteBuffer = ByteBuffer.allocate(32768);

        void setBytes(InputStream is) throws IOException {
            final ByteBufferOutputStream outputStream = new ByteBufferOutputStream(this.m_byteBuffer, true);
            outputStream.clear();

            byte[] buffer = TL_Bytes.get();

            while(true) {
                final int length = is.read(buffer);

                if (length < 1) {
                    outputStream.flip();
                    this.m_byteBuffer = outputStream.getWrappedBuffer();
                    return;
                }

                outputStream.write(buffer, 0, length);
            }
        }
    }
}
