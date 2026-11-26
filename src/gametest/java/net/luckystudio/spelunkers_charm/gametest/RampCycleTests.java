// Ramp cycle tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("spelunkers_ramp_cycles")
@PrefixGameTestTemplate(false)
public final class RampCycleTests {
    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void repeatedRisingRampPulled(GameTestHelper h) {
        RampContactTests.run(h, 10, 0, false, 6);
    }

    @GameTest(template = "matrix", timeoutTicks = 230)
    public static void repeatedRisingRampPushed(GameTestHelper h) {
        RampContactTests.run(h, 10, 0, true, 6);
    }
}
