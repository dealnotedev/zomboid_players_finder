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
import java.util.List;

public class ZomboidPlayerFinder {

    static {
        SoundManager.instance = new SoundManager();
        SurvivorDesc.HairCommonColors.add(ImmutableColor.blue);

        Rand.rand = new CellularAutomatonRNG();
        HairStyles.instance = new HairStyles();
        LuaManager.env = new KahluaTableImpl(new LinkedHashMap<>());

        Class<WorldDictionary> worldDictionaryClass = WorldDictionary.class;
        try {
            DictionaryData dictionaryData = new DictionaryData();

            Field data = worldDictionaryClass.getDeclaredField("data");
            data.setAccessible(true);
            data.set(null, dictionaryData);
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

        List<IsoPlayer> isoPlayers = new ArrayList<>();

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = PZSQLUtils.getConnection(databasePath);
            statement = connection.prepareStatement("SELECT data,worldversion,x,y,z,isDead,name,username FROM networkPlayers");

            resultSet = statement.executeQuery();
            while (resultSet.next()){
                final PlayerData data = new PlayerData();

                final InputStream var5 = resultSet.getBinaryStream(1);
                data.setBytes(var5);

                final IsoPlayer isoPlayer = new IsoPlayer(isoCell, survivorDesc, 0, 0, 0);
                isoPlayer.setName(resultSet.getString(8));
                isoPlayer.load(data.m_byteBuffer, IsoWorld.getWorldVersion());
                isoPlayers.add(isoPlayer);
            }
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }

        return isoPlayers;
    }

    private static void closeQuietly(AutoCloseable connection){
        if(connection != null){
            try {
                connection.close();
            } catch (Exception ignored) {

            }
        }
    }

    private static final ThreadLocal<byte[]> TL_Bytes = ThreadLocal.withInitial(() -> new byte[1024]);

    private static final class PlayerData {
        ByteBuffer m_byteBuffer = ByteBuffer.allocate(32768);

        void setBytes(InputStream var1) throws IOException {
            ByteBufferOutputStream var2 = new ByteBufferOutputStream(this.m_byteBuffer, true);
            var2.clear();
            byte[] var3 = TL_Bytes.get();

            while(true) {
                int var4 = var1.read(var3);
                if (var4 < 1) {
                    var2.flip();
                    this.m_byteBuffer = var2.getWrappedBuffer();
                    return;
                }

                var2.write(var3, 0, var4);
            }
        }
    }
}
