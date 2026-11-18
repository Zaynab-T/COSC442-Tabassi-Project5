package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ItemTest {

    private Item item;

    @BeforeEach
    void setUp() {
        item = new Item(')', Color.WHITE, "dagger", null);
    }

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_storesBasicFields() {
        assertEquals(')', item.glyph());
        assertEquals(Color.WHITE, item.color());
        assertEquals("dagger", item.name());
    }

    @Test
    public void testConstructor_nullAppearance_fallsBackToName() {
        assertEquals("dagger", item.appearance());
    }

    @Test
    public void testConstructor_givenAppearance_isUsed() {
        Item potion = new Item('!', Color.RED, "potion of health", "red potion");

        assertEquals("potion of health", potion.name());
        assertEquals("red potion", potion.appearance());
    }

    @Test
    public void testConstructor_defaultValues() {
        assertEquals(1, item.thrownAttackValue());
        assertEquals(0, item.foodValue());
        assertEquals(0, item.attackValue());
        assertEquals(0, item.defenseValue());
        assertEquals(0, item.rangedAttackValue());
        assertNull(item.quaffEffect());
        assertTrue(item.writtenSpells().isEmpty());
    }

    // ---------------------------------------------------------------
    // modifyFoodValue
    // ---------------------------------------------------------------

    @Test
    public void testModifyFoodValue_accumulates() {
        item.modifyFoodValue(5);
        item.modifyFoodValue(3);

        assertEquals(8, item.foodValue());
    }

    @Test
    public void testModifyFoodValue_negative() {
        item.modifyFoodValue(-4);

        assertEquals(-4, item.foodValue());
    }

    // ---------------------------------------------------------------
    // Getters after modification (details() reads fields directly, so the
    // getters need their own assertions with non-default values)
    // ---------------------------------------------------------------

    @Test
    public void testModifyAttackValue_getterReturnsNewValue() {
        item.modifyAttackValue(3);

        assertEquals(3, item.attackValue());
    }

    @Test
    public void testModifyDefenseValue_getterReturnsNewValue() {
        item.modifyDefenseValue(4);

        assertEquals(4, item.defenseValue());
    }

    @Test
    public void testModifyRangedAttackValue_getterReturnsNewValue() {
        item.modifyRangedAttackValue(5);

        assertEquals(5, item.rangedAttackValue());
    }

    @Test
    public void testModifyThrownAttackValue_getterReturnsNewValue() {
        item.modifyThrownAttackValue(2);

        assertEquals(3, item.thrownAttackValue());
    }

    @Test
    public void testSetQuaffEffect_getterReturnsSameEffect() {
        Effect effect = new Effect(3);

        item.setQuaffEffect(effect);

        assertSame(effect, item.quaffEffect());
    }

    @Test
    public void testAddWrittenSpell_appearsInWrittenSpells() {
        item.addWrittenSpell("blink", 4, new Effect(1));

        assertEquals(1, item.writtenSpells().size());
        Spell spell = item.writtenSpells().get(0);
        assertEquals("blink", spell.name());
        assertEquals(4, spell.manaCost());
    }

    // ---------------------------------------------------------------
    // details()
    // ---------------------------------------------------------------

    @Test
    public void testDetails_defaultItem_isEmpty() {
        assertEquals("", item.details());
    }

    @Test
    public void testDetails_positiveAttack_shown() {
        item.modifyAttackValue(3);

        assertEquals("  attack:3", item.details());
    }

    @Test
    public void testDetails_negativeAttack_shown() {
        item.modifyAttackValue(-2);

        assertEquals("  attack:-2", item.details());
    }

    @Test
    public void testDetails_thrownAtDefaultOne_hidden() {
        assertEquals(1, item.thrownAttackValue());
        assertEquals("", item.details());
    }

    @Test
    public void testDetails_thrownChanged_shown() {
        item.modifyThrownAttackValue(2);

        assertEquals("  thrown:3", item.details());
    }

    @Test
    public void testDetails_rangedOne_shown() {
        item.modifyRangedAttackValue(1);

        assertEquals("  ranged:1", item.details());
    }

    @Test
    public void testDetails_rangedNegative_hidden() {
        item.modifyRangedAttackValue(-1);

        assertEquals("", item.details());
    }

    @Test
    public void testDetails_defense_shown() {
        item.modifyDefenseValue(4);

        assertEquals("  defense:4", item.details());
    }

    @Test
    public void testDetails_food_shown() {
        item.modifyFoodValue(6);

        assertEquals("  food:6", item.details());
    }

    @Test
    public void testDetails_allFields_inOrder() {
        item.modifyAttackValue(2);
        item.modifyThrownAttackValue(2);
        item.modifyRangedAttackValue(4);
        item.modifyDefenseValue(5);
        item.modifyFoodValue(6);

        assertEquals("  attack:2  thrown:3  ranged:4  defense:5  food:6", item.details());
    }
}
