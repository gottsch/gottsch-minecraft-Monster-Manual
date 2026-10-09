package mod.gottsch.neo.gmmtest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless smoke tests ({@code ./gradlew runGameTestServer}): every gmmtest mob is spawned on a
 * stone floor and ticked for three seconds. A mob whose AI, synched data or save path throws takes
 * the server down with it, so a green run means every gmm mob constructs, finalizes and ticks on
 * 1.21.1. It does not test behaviour; that is still the in-game checklist.
 * <p>
 * The game test server needs no EULA, unlike {@code runServer}.
 */
public final class GMMSmokeTests {
    private static final String TEMPLATE = GMMTest.MOD_ID + ":smoke_floor";
    private static final int TICKS = 60;

    private GMMSmokeTests() { }

    @GameTestGenerator
    public static List<TestFunction> spawnAndTick() {
        List<TestFunction> tests = new ArrayList<>();
        for (GMMTestEntities.MobEntry<?> mob : GMMTestEntities.MOBS) {
            tests.add(new TestFunction("gmmtest", "spawn_" + mob.name(), TEMPLATE, TICKS + 40, 0L, true, helper -> {
                helper.spawn(mob.type().get(), new BlockPos(2, 1, 2));
                helper.runAfterDelay(TICKS, helper::succeed);
            }));
        }
        return tests;
    }
}
