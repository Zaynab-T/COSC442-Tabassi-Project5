package game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class PointTest {

    // neighbors8() is shuffled, so compare as a set rather than by order.
    @Test
    public void testNeighbors8_returnsEightSurroundingPointsOnSameLevel() {
        Point center = new Point(5, 7, 2);

        List<Point> neighbors = center.neighbors8();

        Set<Point> expected = new HashSet<>();
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                if (ox != 0 || oy != 0) {
                    expected.add(new Point(5 + ox, 7 + oy, 2));
                }
            }
        }

        assertEquals(8, neighbors.size(), "should return exactly 8 neighbors");
        assertEquals(expected, new HashSet<>(neighbors),
                "should be the 8 surrounding points, same z, excluding the center");
    }
}
