package game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;

public class CreatureTest {
    
    private World world;
    private Creature creature;

    @BeforeEach 
    void setUp() {
        Tile[][][] tiles = new Tile[3][3][1];

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                tiles[x][y][0] = Tile.FLOOR;
            }
        }

        world = new World(tiles);

        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);

        creature.x = 1;
        creature.y = 1;
        creature.z = 0;
    }

    @Test 
    public void testModifyHp_HpIsOne() {
        
        creature.modifyHp(-99, "test");

        assertEquals(1, creature.hp());
        assertNull(world.item(1, 1, 0));
    }

    @Test 
    public void testModifyHp_HpIsZero() {
        
        creature.modifyHp(-100, "test");

        assertEquals(0, creature.hp());
        assertNotNull(world.item(1, 1, 0));
    }

    @Test 
    public void testModifyHp_DeadCreatureIsRemoved() {

        new CreatureAi(creature);
        world.add(creature);
        creature.modifyHp(-100, "test");

        assertNull(world.creature(1, 1, 0));
    }
}
