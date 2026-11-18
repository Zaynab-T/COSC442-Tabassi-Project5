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
        int gainLevelCalls = 0;

        RecordingAi(Creature creature) {
            super(creature);
        }

        @Override
        public void onNotify(String message) {
            messages.add(message);
        }

        // Counts level-ups instead of applying a random LevelUpOption, so
        // stats stay deterministic in the XP tests.
        @Override
        public void onGainLevel() {
            gainLevelCalls++;
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

    // ---------------------------------------------------------------
    // Item helpers for equip / attackValue / defenseValue tests
    // ---------------------------------------------------------------

    private static Item itemWith(String name, int attack, int ranged, int defense) {
        Item item = new Item(')', Color.WHITE, name, null);
        item.modifyAttackValue(attack);
        item.modifyRangedAttackValue(ranged);
        item.modifyDefenseValue(defense);
        return item;
    }

    private void fillInventory() {
        for (int i = 0; i < 20; i++) {
            creature.inventory().add(new Item(',', Color.YELLOW, "pebble" + i, null));
        }
        assertTrue(creature.inventory().isFull());
    }

    // ---------------------------------------------------------------
    // attackValue / defenseValue
    // ---------------------------------------------------------------

    @Test
    public void testAttackAndDefense_IncludeWeaponAndArmorBonuses() {
        addCreatureWithRecorder();
        Item sword = itemWith("sword", 3, 0, 1);  // 3 >= 1 -> weapon
        Item mail = itemWith("mail", 2, 0, 4);    // 2 < 4  -> armor

        creature.equip(sword);
        creature.equip(mail);

        assertSame(sword, creature.weapon());
        assertSame(mail, creature.armor());
        assertEquals(10 + 3 + 2, creature.attackValue());
        assertEquals(5 + 1 + 4, creature.defenseValue());
    }

    @Test
    public void testAttackAndDefense_ModifiersAreAdded() {
        creature.modifyAttackValue(4);
        creature.modifyDefenseValue(-2);

        assertEquals(14, creature.attackValue());
        assertEquals(3, creature.defenseValue());
    }

    // ---------------------------------------------------------------
    // equip
    // ---------------------------------------------------------------

    @Test
    public void testEquip_MeleeWeaponIsWielded() {
        RecordingAi ai = addCreatureWithRecorder();
        Item sword = itemWith("sword", 3, 0, 0);
        creature.inventory().add(sword);

        creature.equip(sword);

        assertSame(sword, creature.weapon());
        assertNull(creature.armor());
        assertEquals(List.of("You wield a sword."), ai.messages);
    }

    @Test
    public void testEquip_RangedOnlyItemIsWielded() {
        addCreatureWithRecorder();
        Item bow = itemWith("bow", 0, 2, 0);
        creature.inventory().add(bow);

        creature.equip(bow);

        assertSame(bow, creature.weapon());
        assertNull(creature.armor());
    }

    @Test
    public void testEquip_RangedPlusAttackDecidesWeaponOverArmor() {
        addCreatureWithRecorder();
        // attack + ranged = 1 + 2 = 3 >= defense 2 -> weapon.
        // If ranged were subtracted instead (1 - 2 = -1 < 2) it would be armor.
        Item spear = itemWith("spear", 1, 2, 2);
        creature.inventory().add(spear);

        creature.equip(spear);

        assertSame(spear, creature.weapon());
        assertNull(creature.armor());
    }

    @Test
    public void testEquip_AttackEqualToDefenseCountsAsWeapon() {
        addCreatureWithRecorder();
        Item buckler = itemWith("buckler", 2, 0, 2);
        creature.inventory().add(buckler);

        creature.equip(buckler);

        assertSame(buckler, creature.weapon());
        assertNull(creature.armor());
    }

    @Test
    public void testEquip_DefenseOnlyItemIsWorn() {
        RecordingAi ai = addCreatureWithRecorder();
        Item mail = itemWith("mail", 0, 0, 3);
        creature.inventory().add(mail);

        creature.equip(mail);

        assertSame(mail, creature.armor());
        assertNull(creature.weapon());
        assertEquals(List.of("You put on a mail."), ai.messages);
    }

    @Test
    public void testEquip_ItemWithNoStatsIsNotEquipped() {
        RecordingAi ai = addCreatureWithRecorder();
        Item rock = itemWith("rock", 0, 0, 0);
        creature.inventory().add(rock);

        creature.equip(rock);

        assertNull(creature.weapon());
        assertNull(creature.armor());
        assertTrue(ai.messages.isEmpty(), "messages were: " + ai.messages);
        assertTrue(creature.inventory().contains(rock));
    }

    @Test
    public void testEquip_ItemOnGroundIsPickedUpThenEquipped() {
        addCreatureWithRecorder();
        Item sword = itemWith("sword", 3, 0, 0);
        world.addAtEmptySpace(sword, CX, CY, CZ);
        assertSame(sword, world.item(CX, CY, CZ));

        creature.equip(sword);

        assertNull(world.item(CX, CY, CZ));
        assertTrue(creature.inventory().contains(sword));
        assertSame(sword, creature.weapon());
    }

    @Test
    public void testEquip_FullInventoryAndItemNotHeld_RefusesToEquip() {
        RecordingAi ai = addCreatureWithRecorder();
        fillInventory();
        Item sword = itemWith("sword", 3, 0, 0);
        world.addAtEmptySpace(sword, CX, CY, CZ);
        ai.messages.clear();

        creature.equip(sword);

        assertNull(creature.weapon());
        assertFalse(creature.inventory().contains(sword));
        assertSame(sword, world.item(CX, CY, CZ));
        assertEquals(List.of("Can't equip sword since you're holding too much stuff."), ai.messages);
    }

    @Test
    public void testEquip_FullInventoryButItemAlreadyHeld_StillEquips() {
        addCreatureWithRecorder();
        Item sword = itemWith("sword", 3, 0, 0);
        creature.inventory().add(sword);
        fillInventory();

        creature.equip(sword);

        assertSame(sword, creature.weapon());
    }

    @Test
    public void testEquip_NewWeaponReplacesOldWeapon() {
        RecordingAi ai = addCreatureWithRecorder();
        Item dagger = itemWith("dagger", 1, 0, 0);
        Item sword = itemWith("sword", 3, 0, 0);
        creature.inventory().add(dagger);
        creature.inventory().add(sword);
        creature.equip(dagger);
        ai.messages.clear();

        creature.equip(sword);

        assertSame(sword, creature.weapon());
        assertEquals(List.of("You put away a dagger.", "You wield a sword."), ai.messages);
        assertEquals(13, creature.attackValue());
    }

    @Test
    public void testEquip_NewArmorReplacesOldArmor() {
        RecordingAi ai = addCreatureWithRecorder();
        Item leather = itemWith("leather", 0, 0, 1);
        Item mail = itemWith("mail", 0, 0, 3);
        creature.inventory().add(leather);
        creature.inventory().add(mail);
        creature.equip(leather);
        ai.messages.clear();

        creature.equip(mail);

        assertSame(mail, creature.armor());
        assertEquals(List.of("You remove a leather.", "You put on a mail."), ai.messages);
        assertEquals(8, creature.defenseValue());
    }

    // ---------------------------------------------------------------
    // unequip
    // ---------------------------------------------------------------

    @Test
    public void testUnequip_Null_DoesNothing() {
        RecordingAi ai = addCreatureWithRecorder();

        creature.unequip(null);

        assertNull(creature.weapon());
        assertNull(creature.armor());
        assertTrue(ai.messages.isEmpty(), "messages were: " + ai.messages);
    }

    @Test
    public void testUnequip_Weapon_ClearsWeaponOnly() {
        RecordingAi ai = addCreatureWithRecorder();
        Item sword = itemWith("sword", 3, 0, 0);
        Item mail = itemWith("mail", 0, 0, 3);
        creature.inventory().add(sword);
        creature.inventory().add(mail);
        creature.equip(sword);
        creature.equip(mail);
        ai.messages.clear();

        creature.unequip(sword);

        assertNull(creature.weapon());
        assertSame(mail, creature.armor());
        assertTrue(creature.inventory().contains(sword));
        assertEquals(List.of("You put away a sword."), ai.messages);
        assertEquals(10, creature.attackValue());
    }

    @Test
    public void testUnequip_Armor_ClearsArmorOnly() {
        RecordingAi ai = addCreatureWithRecorder();
        Item sword = itemWith("sword", 3, 0, 0);
        Item mail = itemWith("mail", 0, 0, 3);
        creature.inventory().add(sword);
        creature.inventory().add(mail);
        creature.equip(sword);
        creature.equip(mail);
        ai.messages.clear();

        creature.unequip(mail);

        assertNull(creature.armor());
        assertSame(sword, creature.weapon());
        assertEquals(List.of("You remove a mail."), ai.messages);
        assertEquals(5, creature.defenseValue());
    }

    @Test
    public void testUnequip_ItemNotEquipped_DoesNothing() {
        RecordingAi ai = addCreatureWithRecorder();
        Item sword = itemWith("sword", 3, 0, 0);
        Item mail = itemWith("mail", 0, 0, 3);
        Item rock = itemWith("rock", 0, 0, 0);
        creature.inventory().add(sword);
        creature.inventory().add(mail);
        creature.equip(sword);
        creature.equip(mail);
        ai.messages.clear();

        creature.unequip(rock);

        assertSame(sword, creature.weapon());
        assertSame(mail, creature.armor());
        assertTrue(ai.messages.isEmpty(), "messages were: " + ai.messages);
    }

    @Test
    public void testUnequip_WhenDead_WeaponRemovedSilently() {
        RecordingAi ai = addCreatureWithRecorder();
        Item sword = itemWith("sword", 3, 0, 0);
        creature.inventory().add(sword);
        creature.equip(sword);
        ai.messages.clear();

        // Death drops inventory, which unequips with hp == 0.
        creature.modifyHp(-100, "test");

        assertEquals(0, creature.hp());
        assertNull(creature.weapon());
        assertTrue(ai.messages.contains("You drop a sword."), "messages were: " + ai.messages);
        assertFalse(ai.messages.contains("You put away a sword."), "messages were: " + ai.messages);
    }

    @Test
    public void testUnequip_WhenDead_ArmorRemovedSilently() {
        RecordingAi ai = addCreatureWithRecorder();
        Item mail = itemWith("mail", 0, 0, 3);
        creature.inventory().add(mail);
        creature.equip(mail);
        ai.messages.clear();

        creature.modifyHp(-100, "test");

        assertEquals(0, creature.hp());
        assertNull(creature.armor());
        assertTrue(ai.messages.contains("You drop a mail."), "messages were: " + ai.messages);
        assertFalse(ai.messages.contains("You remove a mail."), "messages were: " + ai.messages);
    }

    // ---------------------------------------------------------------
    // drop
    // ---------------------------------------------------------------

    @Test
    public void testDrop_EquippedWeapon_LandsAtFeetAndIsUnequipped() {
        RecordingAi ai = addCreatureWithRecorder();
        Item sword = itemWith("sword", 3, 0, 0);
        creature.inventory().add(sword);
        creature.equip(sword);
        ai.messages.clear();

        creature.drop(sword);

        assertSame(sword, world.item(CX, CY, CZ));
        assertFalse(creature.inventory().contains(sword));
        assertNull(creature.weapon());
        assertEquals(10, creature.attackValue());
        assertTrue(ai.messages.contains("You drop a sword."), "messages were: " + ai.messages);
        assertTrue(ai.messages.contains("You put away a sword."), "messages were: " + ai.messages);
    }

    @Test
    public void testDrop_NowhereToDrop_KeepsItemAndNotifies() {
        // A creature standing on a wall: addAtEmptySpace finds no ground tile.
        Tile[][][] walls = new Tile[3][3][1];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                walls[x][y][0] = Tile.WALL;
            }
        }
        World walled = new World(walls);
        Creature boxed = new Creature(walled, '@', Color.GRAY, "josh", 100, 10, 5);
        boxed.x = 1;
        boxed.y = 1;
        boxed.z = 0;
        RecordingAi ai = new RecordingAi(boxed);
        walled.add(boxed);
        Item sword = itemWith("sword", 3, 0, 0);
        boxed.inventory().add(sword);
        boxed.equip(sword);
        ai.messages.clear();

        boxed.drop(sword);

        assertTrue(boxed.inventory().contains(sword));
        assertSame(sword, boxed.weapon());
        assertNull(walled.item(1, 1, 0));
        assertEquals(List.of("There's nowhere to drop the sword."), ai.messages);
    }

    // ---------------------------------------------------------------
    // pickup
    // ---------------------------------------------------------------

    @Test
    public void testPickup_ItemAtFeet_MovesIntoInventory() {
        RecordingAi ai = addCreatureWithRecorder();
        Item rock = itemWith("rock", 0, 0, 0);
        world.addAtEmptySpace(rock, CX, CY, CZ);
        ai.messages.clear();

        creature.pickup();

        assertNull(world.item(CX, CY, CZ));
        assertTrue(creature.inventory().contains(rock));
        assertEquals(List.of("You pickup a rock."), ai.messages);
    }

    @Test
    public void testPickup_NothingThere_GrabsAtGround() {
        RecordingAi ai = addCreatureWithRecorder();

        creature.pickup();

        assertEquals(List.of("You grab at the ground."), ai.messages);
    }

    @Test
    public void testPickup_FullInventory_LeavesItemOnGround() {
        RecordingAi ai = addCreatureWithRecorder();
        fillInventory();
        Item rock = itemWith("rock", 0, 0, 0);
        world.addAtEmptySpace(rock, CX, CY, CZ);
        ai.messages.clear();

        creature.pickup();

        assertSame(rock, world.item(CX, CY, CZ));
        assertFalse(creature.inventory().contains(rock));
        assertEquals(List.of("You grab at the ground."), ai.messages);
    }

    // ---------------------------------------------------------------
    // modifyXp / xp
    // Level threshold is (int)(level^1.75 * 25): L1 -> 25, L2 -> 84, L3 -> 170.
    // ---------------------------------------------------------------

    @Test
    public void testModifyXp_GainBelowThreshold_NoLevelUp() {
        RecordingAi ai = addCreatureWithRecorder();

        creature.modifyXp(25);

        assertEquals(25, creature.xp());
        assertEquals(1, creature.level());
        assertEquals(0, ai.gainLevelCalls);
        assertEquals(List.of("You gain 25 xp."), ai.messages);
    }

    @Test
    public void testModifyXp_JustOverThreshold_LevelsUpAndHeals() {
        RecordingAi ai = addCreatureWithRecorder();
        creature.modifyHp(-50, "test");

        creature.modifyXp(26);

        assertEquals(26, creature.xp());
        assertEquals(2, creature.level());
        assertEquals(1, ai.gainLevelCalls);
        assertEquals(54, creature.hp()); // healed by level * 2 = 4
        assertEquals(List.of("You gain 26 xp.", "You advance to level 2."), ai.messages);
    }

    @Test
    public void testModifyXp_AccumulatesAcrossCalls() {
        addCreatureWithRecorder();

        creature.modifyXp(10);
        creature.modifyXp(10);

        assertEquals(20, creature.xp());
        assertEquals(1, creature.level());
    }

    @Test
    public void testModifyXp_LargeGain_LevelsUpMultipleTimes() {
        RecordingAi ai = addCreatureWithRecorder();
        creature.modifyHp(-50, "test");

        creature.modifyXp(171);

        assertEquals(4, creature.level());
        assertEquals(3, ai.gainLevelCalls);
        assertEquals(50 + 4 + 6 + 8, creature.hp());
        assertEquals(List.of("You gain 171 xp.", "You advance to level 2.",
                "You advance to level 3.", "You advance to level 4."), ai.messages);
    }

    @Test
    public void testModifyXp_ExactlySecondThreshold_StaysAtLevelTwo() {
        addCreatureWithRecorder();

        creature.modifyXp(84);

        assertEquals(2, creature.level());
    }

    @Test
    public void testModifyXp_Zero_SaysGain() {
        RecordingAi ai = addCreatureWithRecorder();

        creature.modifyXp(0);

        assertEquals(0, creature.xp());
        assertEquals(List.of("You gain 0 xp."), ai.messages);
    }

    // NOTE: documents CURRENT behavior. A negative amount prints the signed
    // number after "lose", producing "You lose -5 xp.", and xp can go below 0.
    @Test
    public void testModifyXp_Negative_LosesXpWithSignedMessage() {
        RecordingAi ai = addCreatureWithRecorder();

        creature.modifyXp(-5);

        assertEquals(-5, creature.xp());
        assertEquals(1, creature.level());
        assertEquals(0, ai.gainLevelCalls);
        assertEquals(List.of("You lose -5 xp."), ai.messages);
    }

    // ---------------------------------------------------------------
    // gainXp
    // ---------------------------------------------------------------

    @Test
    public void testGainXp_AwardsMaxHpPlusAttackPlusDefenseMinusLevel() {
        RecordingAi ai = addCreatureWithRecorder();
        Creature goblin = new Creature(world, 'g', Color.GREEN, "goblin", 10, 3, 2);

        creature.gainXp(goblin);

        assertEquals(10 + 3 + 2 - 1, creature.xp());
        assertEquals(List.of("You gain 14 xp."), ai.messages);
    }

    @Test
    public void testGainXp_UsesVictimsEquipmentBonuses() {
        addCreatureWithRecorder();
        Creature goblin = new Creature(world, 'g', Color.GREEN, "goblin", 10, 3, 2);
        new CreatureAi(goblin);
        Item club = itemWith("club", 4, 0, 0);
        goblin.inventory().add(club);
        goblin.equip(club);

        creature.gainXp(goblin);

        assertEquals(10 + 7 + 2 - 1, creature.xp());
    }

    @Test
    public void testGainXp_ZeroAmount_AwardsNothing() {
        RecordingAi ai = addCreatureWithRecorder();
        // 1 + 0 + 0 - level 1 = 0
        Creature rat = new Creature(world, 'r', Color.GRAY, "rat", 1, 0, 0);

        creature.gainXp(rat);

        assertEquals(0, creature.xp());
        assertTrue(ai.messages.isEmpty(), "messages were: " + ai.messages);
    }

    @Test
    public void testGainXp_NegativeAmount_AwardsNothing() {
        RecordingAi ai = addCreatureWithRecorder();
        creature.modifyXp(26); // reach level 2
        ai.messages.clear();
        Creature rat = new Creature(world, 'r', Color.GRAY, "rat", 1, 0, 0);

        creature.gainXp(rat); // 1 + 0 + 0 - 2 = -1

        assertEquals(26, creature.xp());
        assertTrue(ai.messages.isEmpty(), "messages were: " + ai.messages);
    }
}
