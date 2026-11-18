package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class WorldTest {

    // Distinct sizes per dimension so a mix-up (e.g. checking x against
    // height) is caught.
    private static final int WIDTH = 4;
    private static final int HEIGHT = 3;
    private static final int DEPTH = 2;

    private World world;

    private static Tile[][][] filled(int width, int height, int depth, Tile tile) {
        Tile[][][] tiles = new Tile[width][height][depth];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    tiles[x][y][z] = tile;
                }
            }
        }
        return tiles;
    }

    private static Item item(String name) {
        return new Item(',', Color.YELLOW, name, null);
    }

    @BeforeEach
    void setUp() {
        world = new World(filled(WIDTH, HEIGHT, DEPTH, Tile.FLOOR));
    }

    @Test
    public void testConstructor_dimensions() {
        assertEquals(WIDTH, world.width());
        assertEquals(HEIGHT, world.height());
        assertEquals(DEPTH, world.depth());
    }

    // ---------------------------------------------------------------
    // tile(): every map edge and just inside it
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "tile({0},{1},{2}) is BOUNDS")
    @CsvSource({
        "-1, 1, 0",   // x below 0
        " 4, 1, 0",   // x == width
        " 1,-1, 0",   // y below 0
        " 1, 3, 0",   // y == height
        " 1, 1,-1",   // z below 0
        " 1, 1, 2",   // z == depth
    })
    public void testTile_outsideEdge_isBounds(int x, int y, int z) {
        assertEquals(Tile.BOUNDS, world.tile(x, y, z));
    }

    @ParameterizedTest(name = "tile({0},{1},{2}) is FLOOR")
    @CsvSource({
        "0, 1, 0",    // x == 0
        "3, 1, 0",    // x == width - 1
        "1, 0, 0",    // y == 0
        "1, 2, 0",    // y == height - 1
        "1, 1, 0",    // z == 0
        "1, 1, 1",    // z == depth - 1
    })
    public void testTile_insideEdge_isRealTile(int x, int y, int z) {
        assertEquals(Tile.FLOOR, world.tile(x, y, z));
    }

    // ---------------------------------------------------------------
    // creature()
    // ---------------------------------------------------------------

    @Test
    public void testCreature_emptyWorld_returnsNull() {
        assertNull(world.creature(1, 1, 0));
    }

    @Test
    public void testCreature_findsCreatureAtItsPosition() {
        Creature c = placeCreature(1, 1, 0);

        assertSame(c, world.creature(1, 1, 0));
    }

    @ParameterizedTest(name = "creature({0},{1},{2}) is null")
    @CsvSource({
        "2, 1, 0",    // only x differs
        "1, 2, 0",    // only y differs
        "1, 1, 1",    // only z differs
    })
    public void testCreature_oneCoordinateDiffers_returnsNull(int x, int y, int z) {
        placeCreature(1, 1, 0);

        assertNull(world.creature(x, y, z));
    }

    private Creature placeCreature(int x, int y, int z) {
        Creature c = new Creature(world, 'g', Color.GREEN, "goblin", 10, 1, 1);
        c.x = x;
        c.y = y;
        c.z = z;
        world.add(c);
        return c;
    }

    // ---------------------------------------------------------------
    // addAtEmptySpace()
    // ---------------------------------------------------------------

    @Test
    public void testAddAtEmptySpace_emptyFloor_placesItemThere() {
        Item rock = item("rock");

        assertTrue(world.addAtEmptySpace(rock, 1, 1, 0));
        assertSame(rock, world.item(1, 1, 0));
    }

    @Test
    public void testAddAtEmptySpace_occupiedTarget_spillsToNeighbor() {
        Item first = item("first");
        Item second = item("second");
        world.addAtEmptySpace(first, 1, 1, 0);

        assertTrue(world.addAtEmptySpace(second, 1, 1, 0));

        assertSame(first, world.item(1, 1, 0));
        // neighbors8() is shuffled, so search the surrounding 3x3 area.
        boolean found = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (world.item(1 + dx, 1 + dy, 0) == second) {
                    found = true;
                }
            }
        }
        assertTrue(found, "second item should land on a neighboring tile");
    }

    @Test
    public void testAddAtEmptySpace_noFreeGround_returnsFalse() {
        // A 1x1x1 world: once its only tile holds an item, every neighbor is
        // out of bounds, so there is nowhere left to put another item.
        World tiny = new World(filled(1, 1, 1, Tile.FLOOR));
        Item first = item("first");
        tiny.addAtEmptySpace(first, 0, 0, 0);

        assertFalse(tiny.addAtEmptySpace(item("second"), 0, 0, 0));
        assertSame(first, tiny.item(0, 0, 0));
    }

    // NOTE: This test documents CURRENT behavior, not necessarily INTENDED behavior.
    // Placing a null item reports success even though nothing is placed.
    @Test
    public void testAddAtEmptySpace_nullItem_returnsTrue() {
        assertTrue(world.addAtEmptySpace(null, 1, 1, 0));
        assertNull(world.item(1, 1, 0));
    }

    // NOTE: This test documents CURRENT behavior, not necessarily INTENDED behavior.
    // When the target tile is not ground, addAtEmptySpace() skips it without
    // exploring its neighbors, so the item is placed nowhere even though the
    // surrounding tiles are free floor.
    @Test
    public void testAddAtEmptySpace_wallTargetSurroundedByFloor_returnsFalse() {
        Tile[][][] tiles = filled(3, 3, 1, Tile.FLOOR);
        tiles[1][1][0] = Tile.WALL;
        World walled = new World(tiles);

        assertFalse(walled.addAtEmptySpace(item("rock"), 1, 1, 0));

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                assertNull(walled.item(x, y, 0));
            }
        }
    }
}
