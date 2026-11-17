package game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CombatRulesTest {

    @Test 
    public void testCalculateNewHp_Positive() {

        int currentHp = 50;
        int maxHp = 100;
        int amount = 20;

        int newHp = CombatRules.calculateNewHp(currentHp, maxHp, amount);

        assertEquals(70, newHp);
        assertEquals(false, CombatRules.isDead(newHp));
    }

    @Test 
    public void testCalculateNewHp_ExactlyMax() {

        int currentHp = 80;
        int maxHp = 100;
        int amount = 20;

        int newHp = CombatRules.calculateNewHp(currentHp, maxHp, amount);

        assertEquals(100, newHp);
        assertEquals(false, CombatRules.isDead(newHp));
    }

    @Test
    public void testCalculateNewHp_Negative() {

        int currentHp = 50;
        int maxHp = 100;
        int amount = -20;

        int newHp = CombatRules.calculateNewHp(currentHp, maxHp, amount);

        assertEquals(30, newHp);
        assertEquals(false, CombatRules.isDead(newHp));
    }

    @Test
    public void testCalculateNewHp_JustBelowZero() {

        int currentHp = 50;
        int maxHp = 100;
        int amount = -51;

        int newHp = CombatRules.calculateNewHp(currentHp, maxHp, amount);

        assertEquals(-1, newHp);
        assertEquals(true, CombatRules.isDead(newHp));
    }

    @Test 
    public void testCalculateNewHp_Zero() {

        int currentHp = 50;
        int maxHp = 100;
        int amount = 0;

        int newHp = CombatRules.calculateNewHp(currentHp, maxHp, amount);

        assertEquals(50, newHp);
        assertEquals(false, CombatRules.isDead(newHp));
    }

    @Test 
    public void testCalculateNewHp_JustAboveMax() {
        
        int currentHp = 80;
        int maxHp = 100;
        int amount = 21;

        int newHp = CombatRules.calculateNewHp(currentHp, maxHp, amount);

        assertEquals(100, newHp);
        assertEquals(false, CombatRules.isDead(newHp));
    }

    @Test 
    public void testCalculateNewHp_DownToOne() {

        int currentHp = 50;
        int maxHp = 100;
        int amount = -49;

        int newHp = CombatRules.calculateNewHp(currentHp, maxHp, amount);

        assertEquals(1, newHp);
        assertEquals(false, CombatRules.isDead(newHp));
    }

    @Test 
    public void testCalculateNewHp_DownToZero() {

        int currentHp = 50;
        int maxHp = 100;
        int amount = -50;

        int newHp = CombatRules.calculateNewHp(currentHp, maxHp, amount);

        assertEquals(0, newHp);
        assertEquals(true, CombatRules.isDead(newHp));
    }
}
