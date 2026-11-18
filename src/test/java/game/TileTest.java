package game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class TileTest {

    @ParameterizedTest
    @EnumSource(Tile.class)
    public void testIsGround(Tile tile) {
        boolean expected = tile != Tile.WALL && tile != Tile.BOUNDS;

        assertEquals(expected, tile.isGround(), tile + ".isGround()");
    }

    @ParameterizedTest
    @EnumSource(Tile.class)
    public void testIsDiggable_onlyWall(Tile tile) {
        assertEquals(tile == Tile.WALL, tile.isDiggable(), tile + ".isDiggable()");
    }
}
