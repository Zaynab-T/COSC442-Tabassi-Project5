package game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class CreatureTest {

    // World is large enough that a creature at the center can have
    // observers 9 tiles away (the notification radius) and still be in bounds.
    private static final int WIDTH = 21;
    private static final int HEIGHT = 21;
    private static final int DEPTH = 2;

    private static final int CX = 10;
    private static final int CY = 10;
    private static final int CZ = 0;

    private World world;
    private Creature creature;

    /** Test-only AI that records every message the creature is notified of. */
    private static class RecordingAi extends CreatureAi {
        final List<String> messages = new ArrayList<String>();

        RecordingAi(Creature creature) {
            super(creature);
        }

        @Override
        public void onNotify(String message) {
            messages.add(message);
        }
    }

    @BeforeEach
    void setUp() {
        Tile[][][] tiles = new Tile[WIDTH][HEIGHT][DEPTH];

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    tiles[x][y][z] = Tile.FLOOR;
                }
            }
        }

        world = new World(tiles);

        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);

        creature.x = CX;
        creature.y = CY;
        creature.z = CZ;
    }

    /** Gives the test creature a recording AI and places it in the world. */
    private RecordingAi addCreatureWithRecorder() {
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        return ai;
    }

    /** Creates a second creature with a recording AI at the given position. */
    private RecordingAi observerAt(int x, int y, int z) {
        Creature observer = new Creature(world, 'g', Color.GREEN, "goblin", 10, 1, 1);
        observer.x = x;
        observer.y = y;
        observer.z = z;
        RecordingAi ai = new RecordingAi(observer);
        world.add(observer);
        return ai;
    }

    // ---------------------------------------------------------------
    // Starting stats
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_startingStats() {
        assertEquals(100, creature.hp());
        assertEquals(100, creature.maxHp());
        assertEquals(1000, creature.maxFood());
        assertEquals(666, creature.food());
        assertEquals(1, creature.level());
        assertEquals(0, creature.xp());
        assertEquals(5, creature.mana());
        assertEquals(5, creature.maxMana());
        assertEquals(9, creature.visionRadius());
        assertEquals(10, creature.attackValue());
        assertEquals(5, creature.defenseValue());
        assertEquals("josh", creature.name());
        assertEquals('@', creature.glyph());
        assertEquals(Color.GRAY, creature.color());
        assertNull(creature.causeOfDeath());
    }

    // ---------------------------------------------------------------
    // modifyHp
    // ---------------------------------------------------------------

    @Test
    public void testModifyHp_HpIsOne() {

        creature.modifyHp(-99, "test");

        assertEquals(1, creature.hp());
        assertNull(world.item(CX, CY, CZ));
    }

    @Test
    public void testModifyHp_HpIsZero() {

        creature.modifyHp(-100, "test");

        assertEquals(0, creature.hp());
        assertNotNull(world.item(CX, CY, CZ));
    }

    @Test
    public void testModifyHp_DeadCreatureIsRemoved() {

        new CreatureAi(creature);
        world.add(creature);
        creature.modifyHp(-100, "test");

        assertNull(world.creature(CX, CY, CZ));
    }

    @Test
    public void testModifyHp_NonLethalDamage_NoMessagesAndStillInWorld() {
        RecordingAi ai = addCreatureWithRecorder();

        creature.modifyHp(-30, "Hit by a test");

        assertEquals(70, creature.hp());
        assertEquals("Hit by a test", creature.causeOfDeath());
        assertTrue(ai.messages.isEmpty());
        assertSame(creature, world.creature(CX, CY, CZ));
        assertNull(world.item(CX, CY, CZ));
    }

    @Test
    public void testModifyHp_HealingIsCappedAtMaxHp() {
        creature.modifyHp(-30, "test");

        creature.modifyHp(500, "heal");

        assertEquals(100, creature.hp());
    }

    // ---------------------------------------------------------------
    // doAction / notify on death
    // ---------------------------------------------------------------

    @Test
    public void testModifyHp_Death_NotifiesSelf() {
        RecordingAi ai = addCreatureWithRecorder();

        creature.modifyHp(-100, "Killed by a test");

        assertTrue(ai.messages.contains("You die."), "messages were: " + ai.messages);
        assertEquals("Killed by a test", creature.causeOfDeath());
        assertNull(world.creature(CX, CY, CZ));
    }

    @Test
    public void testModifyHp_Death_NotifiesObserverAtExactRadius() {
        addCreatureWithRecorder();
        // Offset (9, 0): 9*9 + 0*0 = 81, exactly on the radius-9 boundary.
        RecordingAi observer = observerAt(CX + 9, CY, CZ);

        creature.modifyHp(-100, "test");

        assertEquals(List.of("The josh dies."), observer.messages);
    }

    @Test
    public void testModifyHp_Death_NotifiesObserverAtExactRadiusVertically() {
        addCreatureWithRecorder();
        // Offset (0, 9): exercises the upper bound of the y-offset loop.
        RecordingAi observer = observerAt(CX, CY + 9, CZ);

        creature.modifyHp(-100, "test");

        assertEquals(List.of("The josh dies."), observer.messages);
    }

    @Test
    public void testModifyHp_Death_DoesNotNotifyObserverJustOutsideRadius() {
        addCreatureWithRecorder();
        // Offset (9, 1): 9*9 + 1*1 = 82, just outside the radius-9 circle.
        RecordingAi observer = observerAt(CX + 9, CY + 1, CZ);

        creature.modifyHp(-100, "test");

        assertTrue(observer.messages.isEmpty(), "messages were: " + observer.messages);
    }

    @Test
    public void testModifyHp_Death_DoesNotNotifyObserverOnDifferentLevel() {
        addCreatureWithRecorder();
        RecordingAi observer = observerAt(CX, CY, CZ + 1);

        creature.modifyHp(-100, "test");

        assertTrue(observer.messages.isEmpty(), "messages were: " + observer.messages);
    }

    // ---------------------------------------------------------------
    // leaveCorpse
    // ---------------------------------------------------------------

    @Test
    public void testModifyHp_Death_LeavesCorpseWithFoodValue() {
        addCreatureWithRecorder();

        creature.modifyHp(-100, "test");

        Item corpse = world.item(CX, CY, CZ);
        assertNotNull(corpse);
        assertEquals("josh corpse", corpse.name());
        assertEquals('%', corpse.glyph());
        assertEquals(Color.GRAY, corpse.color());
        assertEquals(500, corpse.foodValue()); // maxHp * 5
    }

    // NOTE: This test documents CURRENT behavior, not necessarily INTENDED behavior.
    // The dying creature is still in the world when leaveCorpse() runs, so
    // World.addAtEmptySpace() notifies it that its own corpse landed at its feet.
    @Test
    public void testModifyHp_Death_DyingCreatureIsNotifiedOfItsOwnCorpse() {
        RecordingAi ai = addCreatureWithRecorder();

        creature.modifyHp(-100, "test");

        assertTrue(ai.messages.contains("A josh corpse lands between your feet."),
                "messages were: " + ai.messages);
    }

    @Test
    public void testModifyHp_Death_DropsInventoryNearby() {
        addCreatureWithRecorder();
        Item rock = new Item(',', Color.YELLOW, "rock", null);
        creature.inventory().add(rock);

        creature.modifyHp(-100, "test");

        assertFalse(creature.inventory().contains(rock));

        // The corpse takes the creature's own tile, so the item spills to a
        // neighbor. neighbors8() is shuffled, so search the surrounding 3x3 area.
        boolean found = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (world.item(CX + dx, CY + dy, CZ) == rock) {
                    found = true;
                }
            }
        }
        assertTrue(found, "dropped item should be within one tile of the corpse");
    }
}
